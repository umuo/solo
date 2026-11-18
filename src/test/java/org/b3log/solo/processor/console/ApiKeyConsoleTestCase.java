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
import org.b3log.solo.AbstractTestCase;
import org.b3log.solo.MockRequest;
import org.b3log.solo.MockResponse;
import org.b3log.solo.model.Option;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * {@link ApiKeyConsole} test case.
 *
 * @version 1.0.0.0, Nov 18, 2026
 */
@Test(suiteName = "processor")
public class ApiKeyConsoleTestCase extends AbstractTestCase {

    private static final String LEGACY_KEY = "Wp9quLRGYpIxZwPTck32agOntEv6mbbi";
    private String createdKeyId;
    private String createdRawKey;

    /**
     * Verify admin-index.do and admin-apikey.do render API Key UI elements.
     */
    public void renderAdminApiKeyViews() {
        final MockRequest indexReq = mockRequest();
        indexReq.setRequestURI("/admin-index.do");
        mockAdminLogin(indexReq);
        final MockResponse indexRes = mockResponse();
        mockDispatcher(indexReq, indexRes);

        final String indexHtml = indexRes.getString();
        Assert.assertTrue(StringUtils.contains(indexHtml, "API Key 管理"), "admin-index.do should contain API Key menu label");
        Assert.assertTrue(StringUtils.contains(indexHtml, "tabsPanel_apikey"), "admin-index.do should contain tabsPanel_apikey");
        Assert.assertTrue(StringUtils.contains(indexHtml, "/js/admin/apikey.js"), "admin-index.do should load apikey.js");

        final MockRequest pageReq = mockRequest();
        pageReq.setRequestURI("/admin-apikey.do");
        mockAdminLogin(pageReq);
        final MockResponse pageRes = mockResponse();
        mockDispatcher(pageReq, pageRes);

        final String pageHtml = pageRes.getString();
        Assert.assertTrue(StringUtils.contains(pageHtml, "apiKeyNameInput"));
        Assert.assertTrue(StringUtils.contains(pageHtml, "apiKeyTableBody"));
        Assert.assertTrue(StringUtils.contains(pageHtml, "apiKeyNewBanner"));
    }

    /**
     * Verify transparent migration from legacy single Option.ID_C_API_KEY.
     */
    @Test(dependsOnMethods = "renderAdminApiKeyViews")
    public void migrateLegacyApiKeyAndTrackCall() throws Exception {
        final JSONObject legacyOpt = new JSONObject();
        legacyOpt.put(Keys.OBJECT_ID, Option.ID_C_API_KEY);
        legacyOpt.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_API);
        legacyOpt.put(Option.OPTION_VALUE, LEGACY_KEY);
        getOptionMgmtService().addOrUpdateOption(legacyOpt);

        // Call /console/tags with legacy key
        final MockRequest tagReq = mockApiKeyRequest(HttpMethod.GET, "/console/tags", LEGACY_KEY);
        final HttpResponse nettyRes = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse tagRes = new MockResponse(nettyRes);
        mockDispatcher(tagReq, tagRes);

        Assert.assertEquals(nettyRes.status().code(), 200);
        final JSONObject tagBody = new JSONObject(tagRes.getString());
        Assert.assertEquals(tagBody.getInt(Keys.CODE), 0);

        // Verify legacy key was migrated into apiKeys with call tracking
        final JSONObject listResult = fetchApiKeysByAdminCookie();
        Assert.assertEquals(listResult.getInt(Keys.CODE), 0);
        Assert.assertTrue(listResult.getBoolean("hasKey"));
        final JSONArray apiKeys = listResult.getJSONArray("apiKeys");
        Assert.assertEquals(apiKeys.length(), 1);

        final JSONObject migrated = apiKeys.getJSONObject(0);
        Assert.assertEquals(migrated.getString("name"), "默认密钥");
        Assert.assertTrue(migrated.getBoolean("enabled"));
        Assert.assertEquals(migrated.getLong("callCount"), 1L);
        Assert.assertTrue(migrated.getLong("lastUsedAt") > 0);
        Assert.assertEquals(migrated.getString("lastUsedEndpoint"), "GET /console/tags");
        Assert.assertFalse(migrated.has("key"), "List response must not leak raw key");
        Assert.assertFalse(StringUtils.contains(migrated.getString("maskedKey"), LEGACY_KEY));
    }

    /**
     * Verify creating a new API Key and tracking its usage.
     */
    @Test(dependsOnMethods = "migrateLegacyApiKeyAndTrackCall")
    public void createAndTrackNewApiKey() {
        final MockRequest createReq = mockRequest();
        createReq.setRequestURI("/console/api-key");
        createReq.setMethod("POST");
        createReq.setJSON(new JSONObject().put("name", "Test Agent Key"));
        mockAdminLogin(createReq);

        final MockResponse createRes = mockResponse();
        mockDispatcher(createReq, createRes);

        final JSONObject createJson = new JSONObject(createRes.getString());
        Assert.assertEquals(createJson.getInt(Keys.CODE), 0);
        Assert.assertEquals(createJson.getString("name"), "Test Agent Key");
        createdKeyId = createJson.getString("id");
        createdRawKey = createJson.getString("apiKey");
        Assert.assertTrue(StringUtils.startsWith(createdRawKey, "sk-"));
        Assert.assertEquals(createdRawKey.length(), 35);

        // Call /console/tags using the newly created API Key
        final MockRequest tagReq = mockApiKeyRequest(HttpMethod.GET, "/console/tags", createdRawKey);
        final HttpResponse nettyRes = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse tagRes = new MockResponse(nettyRes);
        mockDispatcher(tagReq, tagRes);
        Assert.assertEquals(nettyRes.status().code(), 200);

        // Verify list now shows 2 keys and tracks usage on the new key without exposing plaintext
        final JSONObject listResult = fetchApiKeysByAdminCookie();
        final JSONArray apiKeys = listResult.getJSONArray("apiKeys");
        Assert.assertEquals(apiKeys.length(), 2);

        final JSONObject target = findKeyById(apiKeys, createdKeyId);
        Assert.assertNotNull(target);
        Assert.assertEquals(target.getString("name"), "Test Agent Key");
        Assert.assertEquals(target.getLong("callCount"), 1L);
        Assert.assertTrue(target.getLong("lastUsedAt") > 0);
        Assert.assertEquals(target.getString("lastUsedEndpoint"), "GET /console/tags");
        Assert.assertFalse(StringUtils.contains(listResult.toString(), createdRawKey), "GET list must never contain raw API key");
    }

    /**
     * Verify disabling, renaming, re-enabling, and deleting an API Key.
     */
    @Test(dependsOnMethods = "createAndTrackNewApiKey")
    public void toggleRenameAndDeleteApiKey() {
        // 1. Disable key
        final MockRequest disableReq = mockRequest();
        disableReq.setRequestURI("/console/api-key/" + createdKeyId);
        disableReq.setMethod("PUT");
        disableReq.setJSON(new JSONObject().put("enabled", false));
        mockAdminLogin(disableReq);
        final MockResponse disableRes = mockResponse();
        mockDispatcher(disableReq, disableRes);
        Assert.assertEquals(new JSONObject(disableRes.getString()).getInt(Keys.CODE), 0);

        // Disabled key must be rejected with 401
        final MockRequest blockedReq = mockApiKeyRequest(HttpMethod.GET, "/console/tags", createdRawKey);
        final HttpResponse blockedNettyRes = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse blockedRes = new MockResponse(blockedNettyRes);
        mockDispatcher(blockedReq, blockedRes);
        Assert.assertEquals(blockedNettyRes.status().code(), 401);

        // 2. Rename & re-enable key
        final MockRequest enableReq = mockRequest();
        enableReq.setRequestURI("/console/api-key/" + createdKeyId);
        enableReq.setMethod("PUT");
        enableReq.setJSON(new JSONObject().put("enabled", true).put("name", "Renamed Agent Key"));
        mockAdminLogin(enableReq);
        final MockResponse enableRes = mockResponse();
        mockDispatcher(enableReq, enableRes);
        Assert.assertEquals(new JSONObject(enableRes.getString()).getInt(Keys.CODE), 0);

        // Re-enabled key succeeds again
        final MockRequest okReq = mockApiKeyRequest(HttpMethod.GET, "/console/tags", createdRawKey);
        final HttpResponse okNettyRes = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse okRes = new MockResponse(okNettyRes);
        mockDispatcher(okReq, okRes);
        Assert.assertEquals(okNettyRes.status().code(), 200);

        final JSONObject renamedItem = findKeyById(fetchApiKeysByAdminCookie().getJSONArray("apiKeys"), createdKeyId);
        Assert.assertNotNull(renamedItem);
        Assert.assertEquals(renamedItem.getString("name"), "Renamed Agent Key");
        Assert.assertEquals(renamedItem.getLong("callCount"), 2L);

        // 3. Delete key
        final MockRequest delReq = mockRequest();
        delReq.setRequestURI("/console/api-key/" + createdKeyId);
        delReq.setMethod("DELETE");
        mockAdminLogin(delReq);
        final MockResponse delRes = mockResponse();
        mockDispatcher(delReq, delRes);
        Assert.assertEquals(new JSONObject(delRes.getString()).getInt(Keys.CODE), 0);

        // Deleted key must be rejected with 401
        final MockRequest afterDelReq = mockApiKeyRequest(HttpMethod.GET, "/console/tags", createdRawKey);
        final HttpResponse afterDelNettyRes = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        final MockResponse afterDelRes = new MockResponse(afterDelNettyRes);
        mockDispatcher(afterDelReq, afterDelRes);
        Assert.assertEquals(afterDelNettyRes.status().code(), 401);

        // Remaining list has only migrated default key
        final JSONArray remaining = fetchApiKeysByAdminCookie().getJSONArray("apiKeys");
        Assert.assertEquals(remaining.length(), 1);
        Assert.assertNull(findKeyById(remaining, createdKeyId));
    }

    private MockRequest mockApiKeyRequest(final HttpMethod method, final String uri, final String apiKey) {
        final DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        req.headers().set("X-Solo-API-Key", apiKey);
        return new MockRequest(req);
    }

    private JSONObject fetchApiKeysByAdminCookie() {
        final MockRequest listReq = mockRequest();
        listReq.setRequestURI("/console/api-key");
        listReq.setMethod("GET");
        mockAdminLogin(listReq);
        final MockResponse listRes = mockResponse();
        mockDispatcher(listReq, listRes);
        return new JSONObject(listRes.getString());
    }

    private JSONObject findKeyById(final JSONArray keys, final String id) {
        for (int i = 0; i < keys.length(); i++) {
            final JSONObject item = keys.optJSONObject(i);
            if (null != item && id.equals(item.optString("id"))) {
                return item;
            }
        }
        return null;
    }
}
