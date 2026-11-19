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
package org.b3log.solo.processor.console;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.lang3.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.BeanManager;
import org.b3log.latke.repository.Transaction;
import org.b3log.solo.AbstractTestCase;
import org.b3log.solo.MockRequest;
import org.b3log.solo.MockResponse;
import org.b3log.solo.model.TreeHole;
import org.b3log.solo.repository.TreeHoleRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * {@link TreeHoleConsole} test case.
 *
 * @version 1.0.0.0, Nov 19, 2026
 */
@Test(suiteName = "processor")
public class TreeHoleConsoleTestCase extends AbstractTestCase {

    private String idPending1;
    private String idPending2;
    private String idVisible1;

    /**
     * Verify admin-index.do and admin-treehole.do render TreeHole admin console elements.
     */
    public void renderAdminTreeHoleViews() {
        final MockRequest indexReq = mockRequest();
        indexReq.setRequestURI("/admin-index.do");
        mockAdminLogin(indexReq);
        final MockResponse indexRes = mockResponse();
        mockDispatcher(indexReq, indexRes);

        final String indexHtml = indexRes.getString();
        Assert.assertTrue(StringUtils.contains(indexHtml, "树洞管理"), "admin-index.do should contain 树洞管理 label");
        Assert.assertTrue(StringUtils.contains(indexHtml, "tabsPanel_treehole"), "admin-index.do should contain tabsPanel_treehole");
        Assert.assertTrue(StringUtils.contains(indexHtml, "/js/admin/treehole.js"), "admin-index.do should load treehole.js");

        final MockRequest pageReq = mockRequest();
        pageReq.setRequestURI("/admin-treehole.do");
        mockAdminLogin(pageReq);
        final MockResponse pageRes = mockResponse();
        mockDispatcher(pageReq, pageRes);

        final String pageHtml = pageRes.getString();
        Assert.assertTrue(StringUtils.contains(pageHtml, "thStatTotal"));
        Assert.assertTrue(StringUtils.contains(pageHtml, "thTableBody"));
        Assert.assertTrue(StringUtils.contains(pageHtml, "thBatchApproveBtn"));
    }

    /**
     * Verify unauthenticated request is rejected with 401 and admin Cookie + API Key can query stats/list.
     */
    @Test(dependsOnMethods = "renderAdminTreeHoleViews")
    public void authAndQueryStatsAndList() throws Exception {
        // 1. Unauthenticated access must fail with 401
        final MockRequest unauthReq = mockRequest();
        unauthReq.setRequestURI("/console/tree-hole/list");
        unauthReq.setMethod("GET");
        final HttpResponse unauthNetty = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse unauthRes = new MockResponse(unauthNetty);
        mockDispatcher(unauthReq, unauthRes);
        Assert.assertEquals(unauthNetty.status().code(), 401);

        // 2. Seed records (2 pending, 1 visible)
        final long now = System.currentTimeMillis();
        idPending1 = insertRecord("th-pending-1", "Hangzhou secret wish", "115.236.10.20", "中国|0|浙江省|杭州市|电信",
                now - 3000, 0, 2, 5);
        idPending2 = insertRecord("th-pending-2", "Shenzhen night thoughts", "223.104.62.1", "中国|0|广东省|深圳市|移动",
                now - 2000, 0, 1, 3);
        idVisible1 = insertRecord("th-visible-1", "Public encouragement", "125.121.18.9", "中国|0|浙江省|杭州市|电信",
                now - 1000, 1, 8, 17);

        // 3. Admin Cookie checks /console/tree-hole/stats
        final MockRequest statsReq = mockRequest();
        statsReq.setRequestURI("/console/tree-hole/stats");
        statsReq.setMethod("GET");
        mockAdminLogin(statsReq);
        final MockResponse statsRes = mockResponse();
        mockDispatcher(statsReq, statsRes);

        final JSONObject statsJson = new JSONObject(statsRes.getString());
        Assert.assertEquals(statsJson.getInt(Keys.CODE), 0);
        final JSONObject stats = statsJson.getJSONObject("stats");
        Assert.assertEquals(stats.getLong("totalCount"), 3L);
        Assert.assertEquals(stats.getLong("visibleCount"), 1L);
        Assert.assertEquals(stats.getLong("pendingCount"), 2L);
        Assert.assertEquals(stats.getLong("totalLikeCount"), 11L);
        Assert.assertEquals(stats.getLong("totalWatchedCount"), 25L);

        // 4. Create an API Key and test API Key access + status/keyword filters
        final String apiKey = createTestApiKey();
        final MockRequest apiListReq = mockApiKeyRequest(HttpMethod.GET, "/console/tree-hole/list", apiKey);
        apiListReq.setParameter("status", "pending");
        apiListReq.setParameter("keyword", "Hangzhou");
        final HttpResponse apiNetty = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse apiListRes = new MockResponse(apiNetty);
        mockDispatcher(apiListReq, apiListRes);
        Assert.assertEquals(apiNetty.status().code(), 200);

        final JSONObject listJson = new JSONObject(apiListRes.getString());
        Assert.assertEquals(listJson.getInt(Keys.CODE), 0);
        final JSONArray holes = listJson.getJSONArray("holes");
        Assert.assertEquals(holes.length(), 1);
        Assert.assertEquals(holes.getJSONObject(0).getString(Keys.OBJECT_ID), idPending1);
        Assert.assertFalse(holes.getJSONObject(0).has(TreeHole.hole_random_double));

        // 5. Filter visible by API Key
        final MockRequest visReq = mockApiKeyRequest(HttpMethod.GET, "/console/tree-hole/list", apiKey);
        visReq.setParameter("status", "visible");
        final MockResponse visRes = mockResponse();
        mockDispatcher(visReq, visRes);
        final JSONArray visHoles = new JSONObject(visRes.getString()).getJSONArray("holes");
        Assert.assertEquals(visHoles.length(), 1);
        Assert.assertEquals(visHoles.getJSONObject(0).getString(Keys.OBJECT_ID), idVisible1);
    }

    /**
     * Verify single toggle, batch approve, single delete, and batch delete workflows.
     */
    @Test(dependsOnMethods = "authAndQueryStatsAndList")
    public void toggleBatchApproveAndDelete() throws Exception {
        // 1. Single approve idPending1
        final MockRequest toggleReq = mockRequest();
        toggleReq.setRequestURI("/console/tree-hole/" + idPending1);
        toggleReq.setMethod("PUT");
        toggleReq.setJSON(new JSONObject().put(TreeHole.isShow, 1));
        mockAdminLogin(toggleReq);
        final MockResponse toggleRes = mockResponse();
        mockDispatcher(toggleReq, toggleRes);
        Assert.assertEquals(new JSONObject(toggleRes.getString()).getInt(Keys.CODE), 0);
        Assert.assertEquals(getTreeHoleRepository().get(idPending1).getInt(TreeHole.isShow), 1);

        // 2. Single hide back idPending1
        final MockRequest hideReq = mockRequest();
        hideReq.setRequestURI("/console/tree-hole/" + idPending1);
        hideReq.setMethod("PUT");
        hideReq.setJSON(new JSONObject().put(TreeHole.isShow, 0));
        mockAdminLogin(hideReq);
        final MockResponse hideRes = mockResponse();
        mockDispatcher(hideReq, hideRes);
        Assert.assertEquals(new JSONObject(hideRes.getString()).getInt(Keys.CODE), 0);
        Assert.assertEquals(getTreeHoleRepository().get(idPending1).getInt(TreeHole.isShow), 0);

        // 3. Batch approve idPending1 & idPending2
        final MockRequest batchApproveReq = mockRequest();
        batchApproveReq.setRequestURI("/console/tree-hole/batch/status");
        batchApproveReq.setMethod("PUT");
        batchApproveReq.setJSON(new JSONObject()
                .put("ids", new JSONArray().put(idPending1).put(idPending2))
                .put(TreeHole.isShow, 1));
        mockAdminLogin(batchApproveReq);
        final MockResponse batchApproveRes = mockResponse();
        mockDispatcher(batchApproveReq, batchApproveRes);
        final JSONObject batchApproveJson = new JSONObject(batchApproveRes.getString());
        Assert.assertEquals(batchApproveJson.getInt(Keys.CODE), 0);
        Assert.assertEquals(batchApproveJson.getInt("updatedCount"), 2);
        Assert.assertEquals(getTreeHoleRepository().get(idPending1).getInt(TreeHole.isShow), 1);
        Assert.assertEquals(getTreeHoleRepository().get(idPending2).getInt(TreeHole.isShow), 1);

        // 4. Single delete idVisible1
        final MockRequest delReq = mockRequest();
        delReq.setRequestURI("/console/tree-hole/" + idVisible1);
        delReq.setMethod("DELETE");
        mockAdminLogin(delReq);
        final MockResponse delRes = mockResponse();
        mockDispatcher(delReq, delRes);
        Assert.assertEquals(new JSONObject(delRes.getString()).getInt(Keys.CODE), 0);
        Assert.assertNull(getTreeHoleRepository().get(idVisible1));

        // 5. Batch delete idPending1 & idPending2
        final MockRequest batchDelReq = mockRequest();
        batchDelReq.setRequestURI("/console/tree-hole/batch/delete");
        batchDelReq.setMethod("POST");
        batchDelReq.setJSON(new JSONObject().put("ids", new JSONArray().put(idPending1).put(idPending2)));
        mockAdminLogin(batchDelReq);
        final MockResponse batchDelRes = mockResponse();
        mockDispatcher(batchDelReq, batchDelRes);
        final JSONObject batchDelJson = new JSONObject(batchDelRes.getString());
        Assert.assertEquals(batchDelJson.getInt(Keys.CODE), 0);
        Assert.assertEquals(batchDelJson.getInt("deletedCount"), 2);
        Assert.assertEquals(getTreeHoleRepository().count(), 0L);
    }

    private String createTestApiKey() {
        final MockRequest createReq = mockRequest();
        createReq.setRequestURI("/console/api-key");
        createReq.setMethod("POST");
        createReq.setJSON(new JSONObject().put("name", "TreeHole Test Key"));
        mockAdminLogin(createReq);
        final MockResponse createRes = mockResponse();
        mockDispatcher(createReq, createRes);
        final JSONObject json = new JSONObject(createRes.getString());
        Assert.assertEquals(json.getInt(Keys.CODE), 0);
        return json.getString("apiKey");
    }

    private MockRequest mockApiKeyRequest(final HttpMethod method, final String uri, final String apiKey) {
        final DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        req.headers().set("X-Solo-API-Key", apiKey);
        return new MockRequest(req);
    }

    private String insertRecord(final String id, final String content, final String ip, final String location,
                                final long createTime, final int isShow, final int likeCount, final int watchedCount)
            throws Exception {
        final TreeHoleRepository repo = getTreeHoleRepository();
        final Transaction tx = repo.beginTransaction();
        try {
            final JSONObject record = new JSONObject();
            record.put(Keys.OBJECT_ID, id);
            record.put(TreeHole.content, content);
            record.put(TreeHole.clientIp, ip);
            record.put(TreeHole.location, location);
            record.put(TreeHole.createTime, String.valueOf(createTime));
            record.put(TreeHole.isShow, isShow);
            record.put(TreeHole.likeCount, likeCount);
            record.put(TreeHole.watchedCount, watchedCount);
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
