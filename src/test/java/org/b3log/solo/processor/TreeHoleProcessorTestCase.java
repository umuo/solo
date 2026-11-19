/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-present, b3log.org
 *
 * Solo is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *         http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.b3log.solo.processor;

import org.apache.commons.lang3.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.http.Session;
import org.b3log.latke.http.Sessions;
import org.b3log.latke.ioc.BeanManager;
import org.b3log.latke.repository.Transaction;
import org.b3log.solo.AbstractTestCase;
import org.b3log.solo.MockRequest;
import org.b3log.solo.MockResponse;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.TreeHole;
import org.b3log.solo.repository.TreeHoleRepository;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link TreeHoleProcessor} test case.
 *
 * @version 1.0.0.0, Nov 19, 2026
 */
@Test(suiteName = "processor")
public class TreeHoleProcessorTestCase extends AbstractTestCase {

    private String hiddenId;
    private String visibleId1;
    private String visibleId2;

    @BeforeMethod
    public void clearInMemorySets() {
        TreeHoleProcessor.WATCHED_SET.clear();
        TreeHoleProcessor.LIKED_SET.clear();
    }

    /**
     * Verify addMessage sanitizes HTML, ignores tampered fields, defaults isShow=0, and rejects blank input.
     */
    public void addMessageAndSanitizeXss() throws Exception {
        // 1. Blank content rejected
        final MockRequest blankReq = mockRequest();
        blankReq.setRequestURI("/hole/message/add");
        blankReq.setMethod("POST");
        blankReq.setJSON(new JSONObject().put(TreeHole.hole, new JSONObject().put(TreeHole.content, "   ")));
        final MockResponse blankRes = mockResponse();
        mockDispatcher(blankReq, blankRes);
        final JSONObject blankJson = new JSONObject(blankRes.getString());
        Assert.assertNotEquals(blankJson.getInt(Keys.CODE), 0);

        // 2. XSS payload escaped and tampered fields ignored
        final MockRequest addReq = mockRequest();
        addReq.setRequestURI("/hole/message/add");
        addReq.setMethod("POST");
        final JSONObject holePayload = new JSONObject()
                .put(TreeHole.content, "<script>alert('xss')</script> & \"hi\"")
                .put(TreeHole.isShow, 1)
                .put(TreeHole.likeCount, 999);
        addReq.setJSON(new JSONObject().put(TreeHole.hole, holePayload));
        final MockResponse addRes = mockResponse();
        mockDispatcher(addReq, addRes);

        final JSONObject addJson = new JSONObject(addRes.getString());
        Assert.assertEquals(addJson.getInt(Keys.CODE), 0);
        hiddenId = addJson.getString(Keys.OBJECT_ID);

        final TreeHoleRepository repo = getTreeHoleRepository();
        final JSONObject stored = repo.get(hiddenId);
        Assert.assertNotNull(stored);
        Assert.assertEquals(stored.getInt(TreeHole.isShow), 0, "New message must default to isShow=0");
        Assert.assertEquals(stored.getInt(TreeHole.likeCount), 0, "Tampered likeCount must be ignored");
        Assert.assertEquals(stored.getString(TreeHole.content),
                "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; &amp; &quot;hi&quot;");
    }

    /**
     * Verify randomMessage excludes hidden rows, deduplicates across session, resets when exhausted, and escapes legacy HTML.
     */
    @Test(dependsOnMethods = "addMessageAndSanitizeXss")
    public void randomMessageFilteringAndSessionDedup() throws Exception {
        // 1. Only hidden row exists -> random must return empty
        final MockRequest emptyReq = mockRequest();
        emptyReq.setRequestURI("/hole/message/random");
        emptyReq.setSession(Sessions.add());
        final MockResponse emptyRes = mockResponse();
        mockDispatcher(emptyReq, emptyRes);
        final JSONObject emptyMsg = new JSONObject(emptyRes.getString()).getJSONObject(Common.RANDOM_TREE_HOLE);
        Assert.assertTrue(emptyMsg.isEmpty(), "Hidden message must never be returned");

        // 2. Insert 2 visible rows (one with legacy unescaped HTML)
        visibleId1 = insertVisibleRecord("visible-1", "Hello 树洞");
        visibleId2 = insertVisibleRecord("visible-2", "<svg onload=alert(1)>Legacy XSS");

        final Session session = Sessions.add();
        final Set<String> seenFirstRound = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            final MockRequest req = mockRequest();
            req.setRequestURI("/hole/message/random");
            req.setSession(session);
            final MockResponse res = mockResponse();
            mockDispatcher(req, res);

            final JSONObject msg = new JSONObject(res.getString()).getJSONObject(Common.RANDOM_TREE_HOLE);
            Assert.assertFalse(msg.isEmpty());
            final String oId = msg.getString(Keys.OBJECT_ID);
            Assert.assertNotEquals(oId, hiddenId, "Hidden record must stay excluded");
            Assert.assertFalse(msg.has(TreeHole.clientIp));
            Assert.assertFalse(msg.has(TreeHole.location));
            Assert.assertFalse(msg.has(TreeHole.isShow));
            Assert.assertFalse(msg.has(TreeHole.hole_random_double));
            Assert.assertTrue(StringUtils.isNotBlank(msg.optString(TreeHole.createTime)),
                    "Random message should include createTime");
            Assert.assertTrue(msg.optString("createTimeStr").matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"),
                    "Random message should include formatted createTimeStr");
            Assert.assertFalse(StringUtils.containsAny(msg.getString(TreeHole.content), '<', '>'),
                    "Content must not contain unescaped angle brackets");
            seenFirstRound.add(oId);
        }
        Assert.assertEquals(seenFirstRound.size(), 2, "Session should not repeat messages within the cycle");

        // 3. 3rd request resets cycle and returns a visible message cleanly (no SQL quote error)
        final MockRequest resetReq = mockRequest();
        resetReq.setRequestURI("/hole/message/random");
        resetReq.setSession(session);
        final MockResponse resetRes = mockResponse();
        mockDispatcher(resetReq, resetRes);
        final JSONObject resetMsg = new JSONObject(resetRes.getString()).getJSONObject(Common.RANDOM_TREE_HOLE);
        Assert.assertFalse(resetMsg.isEmpty());
        Assert.assertNotEquals(resetMsg.getString(Keys.OBJECT_ID), hiddenId);

        // 4. Null session still works without NPE
        final MockRequest nullSessionReq = mockRequest();
        nullSessionReq.setRequestURI("/hole/message/random");
        final MockResponse nullSessionRes = mockResponse();
        mockDispatcher(nullSessionReq, nullSessionRes);
        final JSONObject nullSessionMsg = new JSONObject(nullSessionRes.getString()).getJSONObject(Common.RANDOM_TREE_HOLE);
        Assert.assertFalse(nullSessionMsg.isEmpty());
    }

    /**
     * Verify addLike increments likeCount once per IP and handles invalid ids.
     */
    @Test(dependsOnMethods = "randomMessageFilteringAndSessionDedup")
    public void addLikeBehavior() throws Exception {
        // 1. First like succeeds
        final MockRequest likeReq = mockRequest();
        likeReq.setRequestURI("/hole/message/addLike/" + visibleId1);
        final MockResponse likeRes = mockResponse();
        mockDispatcher(likeReq, likeRes);
        Assert.assertEquals(new JSONObject(likeRes.getString()).getInt(Keys.CODE), 0);
        Assert.assertEquals(getTreeHoleRepository().get(visibleId1).getInt(TreeHole.likeCount), 1);

        // 2. Duplicate like from same IP rejected
        final MockRequest dupReq = mockRequest();
        dupReq.setRequestURI("/hole/message/addLike/" + visibleId1);
        final MockResponse dupRes = mockResponse();
        mockDispatcher(dupReq, dupRes);
        Assert.assertNotEquals(new JSONObject(dupRes.getString()).getInt(Keys.CODE), 0);
        Assert.assertEquals(getTreeHoleRepository().get(visibleId1).getInt(TreeHole.likeCount), 1);

        // 3. Unknown oId returns error
        final MockRequest missingReq = mockRequest();
        missingReq.setRequestURI("/hole/message/addLike/nonexistent-id-999");
        final MockResponse missingRes = mockResponse();
        mockDispatcher(missingReq, missingRes);
        Assert.assertNotEquals(new JSONObject(missingRes.getString()).getInt(Keys.CODE), 0);
    }

    /**
     * Verify /tree-hole and /tree-hole.html render public TreeHole page cleanly.
     */
    @Test(dependsOnMethods = "addLikeBehavior")
    public void renderTreeHolePage() {
        final MockRequest req = mockRequest();
        req.setRequestURI("/tree-hole");
        req.setMethod("GET");
        final MockResponse res = mockResponse();
        mockDispatcher(req, res);

        final String html = res.getString();
        Assert.assertTrue(StringUtils.contains(html, "treeHoleApp"), "/tree-hole should render treeHoleApp container");
        Assert.assertTrue(StringUtils.contains(html, "树洞"), "/tree-hole should render title 树洞");

        // Also test Casper template directly with skin=Casper query parameter
        final MockRequest casperReq = mockRequest();
        casperReq.setRequestURI("/tree-hole");
        casperReq.setMethod("GET");
        casperReq.setParameter("skin", "Casper");
        final MockResponse casperRes = mockResponse();
        mockDispatcher(casperReq, casperRes);
        final String casperHtml = casperRes.getString();
        Assert.assertTrue(StringUtils.contains(casperHtml, "treeHoleApp"));
        Assert.assertTrue(StringUtils.contains(casperHtml, "treehole-card"));
        Assert.assertTrue(StringUtils.contains(casperHtml, "treeHoleOpenModalBtn"));

        // Also test .html route
        final MockRequest htmlReq = mockRequest();
        htmlReq.setRequestURI("/tree-hole.html");
        htmlReq.setMethod("GET");
        final MockResponse htmlRes = mockResponse();
        mockDispatcher(htmlReq, htmlRes);
        Assert.assertTrue(StringUtils.contains(htmlRes.getString(), "treeHoleApp"));
    }

    private String insertVisibleRecord(final String id, final String content) throws Exception {
        final TreeHoleRepository repo = getTreeHoleRepository();
        final Transaction tx = repo.beginTransaction();
        try {
            final JSONObject record = new JSONObject();
            record.put(Keys.OBJECT_ID, id);
            record.put(TreeHole.content, content);
            record.put(TreeHole.clientIp, "127.0.0.1");
            record.put(TreeHole.location, "Local");
            record.put(TreeHole.createTime, String.valueOf(System.currentTimeMillis()));
            record.put(TreeHole.isShow, 1);
            record.put(TreeHole.likeCount, 0);
            record.put(TreeHole.watchedCount, 0);
            record.put(TreeHole.hole_random_double, Math.random());
            repo.add(record);
            tx.commit();
            return id;
        } catch (final Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private TreeHoleRepository getTreeHoleRepository() {
        return BeanManager.getInstance().getReference(TreeHoleRepository.class);
    }
}
