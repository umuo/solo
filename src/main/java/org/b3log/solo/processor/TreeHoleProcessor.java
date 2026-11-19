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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.Request;
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.http.Response;
import org.b3log.latke.http.Session;
import org.b3log.latke.http.renderer.JsonRenderer;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.TreeHole;
import org.b3log.solo.service.TreeHoleService;
import org.b3log.solo.util.IPUtils;
import org.b3log.solo.util.StatusCodes;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: gitsilence
 * @description: 树洞
 * @date: 2021/6/26 10:24 上午
 **/
@Singleton
public class TreeHoleProcessor {

    @Inject
    private TreeHoleService treeHoleService;

    @Inject
    private LangPropsService langPropsService;

    private static final Logger LOGGER = LogManager.getLogger(TreeHoleProcessor.class);

    public static final Set<String> WATCHED_SET = Collections.synchronizedSet(new HashSet<>());

    public static final Set<String> LIKED_SET = Collections.synchronizedSet(new HashSet<>());

    public boolean checkIsAllowAdd(final String content, final String realIp) {
        final LocalDate now = LocalDate.now();
        LOGGER.info("Today date is {}", now.toString());
        // 获取当天日期的时间戳 13位的
        final long todayDate = now.atStartOfDay(ZoneOffset.ofHours(8)).toInstant().toEpochMilli();
        final long count = treeHoleService.countByIp(todayDate, realIp);
        return count < Common.MAX_MES_DAY_COUNT;
    }

    public void addMessage(final RequestContext context) {
        setCrossOrigin(context.getResponse());
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        if ("options".equalsIgnoreCase(context.getRequest().getMethod())) {
            renderer.setJSONObject(ret);
            return;
        }

        final String realIp = IPUtils.getRealIp(context.getRequest());
        try {
            final JSONObject requestJSON = context.requestJSON();
            final JSONObject jsonObject = null == requestJSON ? null : requestJSON.optJSONObject(TreeHole.hole);
            if (null == jsonObject || StringUtils.isBlank(jsonObject.optString(TreeHole.content))) {
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, "留言内容不能为空");
                renderer.setJSONObject(ret);
                return;
            }

            if (!checkIsAllowAdd(jsonObject.optString(TreeHole.content), realIp)) {
                LOGGER.warn("the current IP {} can't be allowed to invoke...", realIp);
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, "not allowed");
                renderer.setJSONObject(ret);
                return;
            }

            LOGGER.log(Level.INFO, "add Message, request body: [{}]", requestJSON.toString());
            jsonObject.put(TreeHole.clientIp, realIp);
            jsonObject.put(TreeHole.location, IPUtils.getRegion(realIp));
            final String messageId = treeHoleService.addMessage(requestJSON);
            ret.put(Keys.OBJECT_ID, messageId);
            ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));
            ret.put(Keys.CODE, StatusCodes.SUCC);
            renderer.setJSONObject(ret);
        } catch (final Exception e) {
            final JSONObject err = new JSONObject().put(Keys.CODE, StatusCodes.ERR);
            err.put(Keys.MSG, StringUtils.defaultIfBlank(e.getMessage(), "add message failed"));
            renderer.setJSONObject(err);
        }
    }

    public void addLike(final RequestContext context) {
        setCrossOrigin(context.getResponse());
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final String realIp = IPUtils.getRealIp(context.getRequest());
        final String id = context.pathVar(Keys.OBJECT_ID);
        if (StringUtils.isBlank(id)) {
            final JSONObject err = new JSONObject().put(Keys.CODE, StatusCodes.ERR);
            err.put(Keys.MSG, "id is required");
            renderer.setJSONObject(err);
            return;
        }

        final String likedKey = realIp + id;
        if (LIKED_SET.contains(likedKey)) {
            final JSONObject jsonObject = new JSONObject().put(Keys.CODE, StatusCodes.ERR);
            jsonObject.put(Keys.MSG, "已经点过赞了");
            renderer.setJSONObject(jsonObject);
            return;
        }
        try {
            treeHoleService.updateCountById(TreeHole.likeCount, id);
            LIKED_SET.add(likedKey);
            final JSONObject ret = new JSONObject();
            ret.put(Keys.OBJECT_ID, id);
            ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));
            ret.put(Keys.CODE, StatusCodes.SUCC);
            renderer.setJSONObject(ret);
        } catch (final Exception e) {
            final JSONObject jsonObject = new JSONObject().put(Keys.CODE, StatusCodes.ERR);
            jsonObject.put(Keys.MSG, StringUtils.defaultIfBlank(e.getMessage(), "like failed"));
            renderer.setJSONObject(jsonObject);
        }
    }

    public void getRandomMessage(final RequestContext context) {
        final Response response = context.getResponse();
        setCrossOrigin(response);
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject jsonObject = new JSONObject();
        final Request request = context.getRequest();
        final Session session = request.getSession();
        final JSONObject ret = getRandomly(IPUtils.getRealIp(request), session);
        jsonObject.put(Common.RANDOM_TREE_HOLE, ret);
        renderer.setJSONObject(jsonObject);
    }

    public JSONObject getRandomly(final String clientIp, final Session session) {
        try {
            final JSONObject randomMessage = treeHoleService.getRandomMessage(session);
            if (null == randomMessage) {
                return new JSONObject();
            }
            final String id = randomMessage.optString(Keys.OBJECT_ID);
            if (StringUtils.isBlank(id)) {
                return new JSONObject();
            }

            final String watchedKey = clientIp + id;
            if (!WATCHED_SET.contains(watchedKey)) {
                try {
                    treeHoleService.updateCountById(TreeHole.watchedCount, id);
                    WATCHED_SET.add(watchedKey);
                } catch (final ServiceException e) {
                    LOGGER.warn("update watchedCount failed for id {}: {}", id, e.getMessage());
                }
            }

            // Defense-in-depth: escape raw angle brackets in case legacy database rows contain unescaped HTML.
            final String rawContent = randomMessage.optString(TreeHole.content);
            if (StringUtils.containsAny(rawContent, '<', '>')) {
                randomMessage.put(TreeHole.content, rawContent.replace("<", "&lt;").replace(">", "&gt;"));
            }

            randomMessage.remove(TreeHole.hole_random_double);
            randomMessage.remove(TreeHole.createTime);
            randomMessage.remove(TreeHole.isShow);
            randomMessage.remove(TreeHole.location);
            randomMessage.remove(TreeHole.clientIp);
            return randomMessage;
        } catch (final ServiceException e) {
            LOGGER.error("get Randomly , {}", e.getMessage());
            return new JSONObject();
        }
    }

    public void setCrossOrigin(final Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        // 解决post请求跨域，因为 解决POST请求跨域，会先发送一个options请求
        response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        response.addHeader("Access-Control-Allow-Headers", "*");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Max-Age", "3600");
    }
}
