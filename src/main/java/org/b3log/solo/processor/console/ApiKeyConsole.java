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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.http.renderer.JsonRenderer;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.ServiceException;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.OptionMgmtService;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.util.StatusCodes;
import org.json.JSONObject;

/**
 * API Key console request processing.
 * <p>
 * Provides API Key management for programmatic access to console APIs.
 * </p>
 *
 * @version 1.0.0.0, Nov 18, 2026
 * @since 4.4.0
 */
@Singleton
public class ApiKeyConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ApiKeyConsole.class);

    /**
     * API Key length.
     */
    private static final int API_KEY_LENGTH = 32;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * Option management service.
     */
    @Inject
    private OptionMgmtService optionMgmtService;

    /**
     * Gets the current API Key (partially masked).
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "code": 0,
     *     "apiKey": "sk-abcd****wxyz",
     *     "hasKey": true
     * }
     * </pre>
     * </p>
     *
     * @param context the specified request context
     */
    public void getApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final JSONObject apiKeyOpt = optionQueryService.getOptionById(Option.ID_C_API_KEY);
            if (null == apiKeyOpt) {
                ret.put(Keys.CODE, StatusCodes.SUCC);
                ret.put("hasKey", false);
                ret.put("apiKey", "");
                return;
            }

            final String apiKey = apiKeyOpt.optString(Option.OPTION_VALUE);
            if (StringUtils.isBlank(apiKey)) {
                ret.put(Keys.CODE, StatusCodes.SUCC);
                ret.put("hasKey", false);
                ret.put("apiKey", "");
                return;
            }

            // Mask the middle part: show first 7 and last 4 chars
            final String masked;
            if (apiKey.length() > 11) {
                masked = apiKey.substring(0, 7) + "****" + apiKey.substring(apiKey.length() - 4);
            } else {
                masked = "****";
            }

            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("hasKey", true);
            ret.put("apiKey", masked);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets API Key failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "Gets API Key failed");
        }
    }

    /**
     * Generates or regenerates an API Key.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "code": 0,
     *     "apiKey": "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
     * }
     * </pre>
     * </p>
     *
     * @param context the specified request context
     */
    public void generateApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final String apiKey = "sk-" + RandomStringUtils.randomAlphanumeric(API_KEY_LENGTH);

            final JSONObject option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_API_KEY);
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_API);
            option.put(Option.OPTION_VALUE, apiKey);

            optionMgmtService.addOrUpdateOption(option);

            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("apiKey", apiKey);

            LOGGER.log(Level.INFO, "API Key generated successfully");
        } catch (final ServiceException e) {
            LOGGER.log(Level.ERROR, "Generates API Key failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "Generates API Key failed");
        }
    }

    /**
     * Removes the API Key.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "code": 0,
     *     "msg": "Removed"
     * }
     * </pre>
     * </p>
     *
     * @param context the specified request context
     */
    public void removeApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final JSONObject option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_API_KEY);
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_API);
            option.put(Option.OPTION_VALUE, "");

            optionMgmtService.addOrUpdateOption(option);

            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put(Keys.MSG, "API Key removed");

            LOGGER.log(Level.INFO, "API Key removed");
        } catch (final ServiceException e) {
            LOGGER.log(Level.ERROR, "Removes API Key failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "Removes API Key failed");
        }
    }
}
