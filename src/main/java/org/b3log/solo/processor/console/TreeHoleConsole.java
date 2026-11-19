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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.http.renderer.JsonRenderer;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.model.Pagination;
import org.b3log.latke.service.LangPropsService;
import org.b3log.solo.model.TreeHole;
import org.b3log.solo.service.TreeHoleService;
import org.b3log.solo.util.StatusCodes;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TreeHole admin console request processing.
 *
 * @version 1.0.0.0, Nov 19, 2026
 * @since 4.4.0
 */
@Singleton
public class TreeHoleConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(TreeHoleConsole.class);

    /**
     * TreeHole service.
     */
    @Inject
    private TreeHoleService treeHoleService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Gets aggregate TreeHole statistics.
     *
     * @param context the specified request context
     */
    public void getStats(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final JSONObject stats = treeHoleService.getStats();
            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("stats", stats);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets TreeHole stats failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Gets paginated TreeHole messages along with current stats.
     *
     * @param context the specified request context
     */
    public void getList(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final int pageNum = parsePositiveInt(context.param("page"), 1);
            final int pageSize = Math.min(100, parsePositiveInt(context.param("pageSize"), 15));
            final Integer statusFilter = parseStatusFilter(context.param("status"));
            String keyword = StringUtils.trimToEmpty(context.param("keyword"));
            if (StringUtils.isBlank(keyword)) {
                keyword = StringUtils.trimToEmpty(context.param("k"));
            }

            final JSONObject listResult = treeHoleService.getAdminList(pageNum, pageSize, statusFilter, keyword);
            final JSONObject stats = treeHoleService.getStats();

            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("stats", stats);
            ret.put(Pagination.PAGINATION, listResult.optJSONObject(Pagination.PAGINATION));
            ret.put("holes", listResult.opt("holes"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets TreeHole list failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Updates a single TreeHole message's visibility status.
     *
     * @param context the specified request context
     */
    public void updateStatus(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final String id = context.pathVar("id");
        if (StringUtils.isBlank(id)) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "TreeHole id is required");
            return;
        }

        final JSONObject req;
        try {
            req = context.requestJSON();
        } catch (final Exception e) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("updateFailLabel"));
            return;
        }

        final Integer isShow = parseIsShow(req);
        if (null == isShow) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "isShow is required");
            return;
        }

        try {
            final int updatedCount = treeHoleService.updateStatus(Collections.singletonList(id), isShow);
            if (0 == updatedCount) {
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, "记录不存在");
                return;
            }
            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Updates TreeHole status failed [id={}]", id, e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("updateFailLabel"));
        }
    }

    /**
     * Batch updates visibility status for multiple TreeHole messages.
     *
     * @param context the specified request context
     */
    public void batchUpdateStatus(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final JSONObject req;
        try {
            req = context.requestJSON();
        } catch (final Exception e) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("updateFailLabel"));
            return;
        }

        final List<String> ids = parseIds(req);
        final Integer isShow = parseIsShow(req);
        if (ids.isEmpty() || null == isShow) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "ids and isShow are required");
            return;
        }

        try {
            final int updatedCount = treeHoleService.updateStatus(ids, isShow);
            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("updatedCount", updatedCount);
            ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Batch updates TreeHole status failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("updateFailLabel"));
        }
    }

    /**
     * Deletes a single TreeHole message by id.
     *
     * @param context the specified request context
     */
    public void removeMessage(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final String id = context.pathVar("id");
        if (StringUtils.isBlank(id)) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "TreeHole id is required");
            return;
        }

        try {
            final int deletedCount = treeHoleService.removeMessages(Collections.singletonList(id));
            if (0 == deletedCount) {
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, "记录不存在");
                return;
            }
            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Removes TreeHole message failed [id={}]", id, e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
        }
    }

    /**
     * Batch deletes multiple TreeHole messages.
     *
     * @param context the specified request context
     */
    public void batchRemoveMessages(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final JSONObject req;
        try {
            req = context.requestJSON();
        } catch (final Exception e) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
            return;
        }

        final List<String> ids = parseIds(req);
        if (ids.isEmpty()) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "ids is required");
            return;
        }

        try {
            final int deletedCount = treeHoleService.removeMessages(ids);
            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("deletedCount", deletedCount);
            ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Batch removes TreeHole messages failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
        }
    }

    private static int parsePositiveInt(final String raw, final int defaultVal) {
        if (StringUtils.isBlank(raw)) {
            return defaultVal;
        }
        try {
            final int val = Integer.parseInt(raw.trim());
            return val > 0 ? val : defaultVal;
        } catch (final NumberFormatException e) {
            return defaultVal;
        }
    }

    private static Integer parseStatusFilter(final String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        final String s = status.trim().toLowerCase();
        if ("pending".equals(s) || "0".equals(s)) {
            return 0;
        }
        if ("visible".equals(s) || "1".equals(s)) {
            return 1;
        }
        return null;
    }

    private static Integer parseIsShow(final JSONObject req) {
        if (null == req || !req.has(TreeHole.isShow)) {
            return null;
        }
        final Object val = req.opt(TreeHole.isShow);
        if (val instanceof Boolean) {
            return Boolean.TRUE.equals(val) ? 1 : 0;
        }
        if (val instanceof Number) {
            return 1 == ((Number) val).intValue() ? 1 : 0;
        }
        if (val instanceof String) {
            final String s = ((String) val).trim();
            if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
                return 1;
            }
            if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
                return 0;
            }
        }
        return null;
    }

    private static List<String> parseIds(final JSONObject req) {
        final List<String> ids = new ArrayList<>();
        if (null == req) {
            return ids;
        }
        final JSONArray arr = req.optJSONArray("ids");
        if (null == arr) {
            return ids;
        }
        for (int i = 0; i < arr.length(); i++) {
            final String id = StringUtils.trimToEmpty(arr.optString(i));
            if (StringUtils.isNotBlank(id)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
