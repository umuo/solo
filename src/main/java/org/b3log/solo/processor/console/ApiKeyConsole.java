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
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.util.Requests;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.OptionMgmtService;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.StatusCodes;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * API Key console request processing.
 * <p>
 * Provides multi-API-Key management and usage tracking for programmatic access to console APIs.
 * </p>
 *
 * @version 2.0.0.0, Nov 18, 2026
 * @since 4.4.0
 */
@Singleton
public class ApiKeyConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ApiKeyConsole.class);

    /**
     * API Key random length.
     */
    private static final int API_KEY_LENGTH = 32;

    /**
     * Maximum number of API Keys.
     */
    private static final int MAX_API_KEYS = 20;

    /**
     * Synchronization lock for loading, updating, and recording API Key usage.
     */
    private static final Object LOCK = new Object();

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
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Authenticates a request by API Key and records call statistics.
     *
     * @param apiKey  the API Key from request header
     * @param context the request context
     * @return {@code true} if authentication succeeds
     */
    public boolean authenticateAndRecord(final String apiKey, final RequestContext context) {
        if (StringUtils.isBlank(apiKey)) {
            return false;
        }

        final byte[] candidateBytes = apiKey.getBytes(StandardCharsets.UTF_8);

        synchronized (LOCK) {
            try {
                final JSONArray keys = loadAndMigrateKeys();
                JSONObject matched = null;
                for (int i = 0; i < keys.length(); i++) {
                    final JSONObject item = keys.optJSONObject(i);
                    if (null == item) {
                        continue;
                    }
                    final String storedKey = item.optString("key", "");
                    if (StringUtils.isBlank(storedKey)) {
                        continue;
                    }
                    if (MessageDigest.isEqual(candidateBytes, storedKey.getBytes(StandardCharsets.UTF_8))) {
                        matched = item;
                        break;
                    }
                }

                if (null == matched || !matched.optBoolean("enabled", true)) {
                    return false;
                }

                final JSONObject admin = userQueryService.getAdmin();
                if (null == admin) {
                    return false;
                }

                // Update usage stats
                matched.put("lastUsedAt", System.currentTimeMillis());
                matched.put("callCount", matched.optLong("callCount", 0L) + 1L);
                final String ip = StringUtils.defaultString(Requests.getRemoteAddr(context.getRequest()), "");
                matched.put("lastUsedIp", StringUtils.abbreviate(ip, 64));
                final String endpoint = (StringUtils.defaultString(context.method(), "") + " "
                        + StringUtils.defaultString(context.requestURI(), "")).trim();
                matched.put("lastUsedEndpoint", StringUtils.abbreviate(endpoint, 120));

                try {
                    saveKeys(keys);
                } catch (final Exception e) {
                    LOGGER.log(Level.WARN, "Failed to persist API Key usage statistics", e);
                }

                context.attr(Common.CURRENT_USER, admin);
                return true;
            } catch (final Exception e) {
                LOGGER.log(Level.WARN, "API Key authentication failed", e);
                return false;
            }
        }
    }

    /**
     * Gets all API Keys (with keys masked).
     *
     * @param context the specified request context
     */
    public void getApiKeys(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final JSONArray keys;
            synchronized (LOCK) {
                keys = loadAndMigrateKeys();
            }

            final List<JSONObject> items = new ArrayList<>();
            for (int i = 0; i < keys.length(); i++) {
                final JSONObject item = keys.optJSONObject(i);
                if (null != item) {
                    items.add(item);
                }
            }
            items.sort(Comparator.comparingLong((JSONObject o) -> o.optLong("createdAt", 0L)).reversed());

            final JSONArray safeKeys = new JSONArray();
            String firstEnabledMasked = "";
            boolean hasEnabledKey = false;

            for (final JSONObject item : items) {
                final String rawKey = item.optString("key", "");
                final String masked = maskKey(rawKey);
                final boolean enabled = item.optBoolean("enabled", true);

                if (enabled && !hasEnabledKey && StringUtils.isNotBlank(rawKey)) {
                    hasEnabledKey = true;
                    firstEnabledMasked = masked;
                }

                final JSONObject safe = new JSONObject();
                safe.put("id", item.optString("id", ""));
                safe.put("name", item.optString("name", ""));
                safe.put("maskedKey", masked);
                safe.put("enabled", enabled);
                safe.put("createdAt", item.optLong("createdAt", 0L));
                safe.put("lastUsedAt", item.optLong("lastUsedAt", 0L));
                safe.put("lastUsedIp", item.optString("lastUsedIp", ""));
                safe.put("lastUsedEndpoint", item.optString("lastUsedEndpoint", ""));
                safe.put("callCount", item.optLong("callCount", 0L));
                safeKeys.put(safe);
            }

            ret.put(Keys.CODE, StatusCodes.SUCC);
            ret.put("apiKeys", safeKeys);
            ret.put("hasKey", hasEnabledKey);
            ret.put("apiKey", firstEnabledMasked);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets API Keys failed", e);
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Creates a new API Key.
     *
     * @param context the specified request context
     */
    public void createApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        String name = "API Key";
        try {
            final JSONObject req = context.requestJSON();
            if (null != req) {
                final String reqName = StringUtils.trimToEmpty(req.optString("name"));
                if (StringUtils.isNotBlank(reqName)) {
                    name = StringUtils.abbreviate(reqName, 50);
                }
            }
        } catch (final Exception ignored) {
            // Empty or non-JSON request body falls back to default name
        }

        synchronized (LOCK) {
            try {
                final JSONArray keys = loadAndMigrateKeys();
                if (keys.length() >= MAX_API_KEYS) {
                    ret.put(Keys.CODE, StatusCodes.ERR);
                    ret.put(Keys.MSG, "API Key count exceeds limit (" + MAX_API_KEYS + ")");
                    return;
                }

                final long now = System.currentTimeMillis();
                final String id = now + RandomStringUtils.randomAlphanumeric(4).toLowerCase();
                final String rawKey = "sk-" + RandomStringUtils.randomAlphanumeric(API_KEY_LENGTH);

                final JSONObject entry = new JSONObject();
                entry.put("id", id);
                entry.put("name", name);
                entry.put("key", rawKey);
                entry.put("enabled", true);
                entry.put("createdAt", now);
                entry.put("lastUsedAt", 0L);
                entry.put("lastUsedIp", "");
                entry.put("lastUsedEndpoint", "");
                entry.put("callCount", 0L);

                keys.put(entry);
                saveKeys(keys);

                ret.put(Keys.CODE, StatusCodes.SUCC);
                ret.put("id", id);
                ret.put("name", name);
                ret.put("apiKey", rawKey);
                ret.put("maskedKey", maskKey(rawKey));
                ret.put("createdAt", now);
                ret.put(Keys.MSG, langPropsService.get("addSuccLabel"));

                LOGGER.log(Level.INFO, "API Key created [id={}, name={}]", id, name);
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Creates API Key failed", e);
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, langPropsService.get("addFailLabel"));
            }
        }
    }

    /**
     * Updates an API Key's enabled status or name.
     *
     * @param context the specified request context
     */
    public void updateApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final String id = context.pathVar("id");
        if (StringUtils.isBlank(id)) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "API Key id is required");
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

        synchronized (LOCK) {
            try {
                final JSONArray keys = loadAndMigrateKeys();
                JSONObject target = null;
                for (int i = 0; i < keys.length(); i++) {
                    final JSONObject item = keys.optJSONObject(i);
                    if (null != item && id.equals(item.optString("id"))) {
                        target = item;
                        break;
                    }
                }

                if (null == target) {
                    ret.put(Keys.CODE, StatusCodes.ERR);
                    ret.put(Keys.MSG, "API Key not found");
                    return;
                }

                if (null != req) {
                    if (req.has("enabled")) {
                        target.put("enabled", req.optBoolean("enabled", true));
                    }
                    if (req.has("name")) {
                        final String reqName = StringUtils.trimToEmpty(req.optString("name"));
                        if (StringUtils.isNotBlank(reqName)) {
                            target.put("name", StringUtils.abbreviate(reqName, 50));
                        }
                    }
                }

                saveKeys(keys);
                ret.put(Keys.CODE, StatusCodes.SUCC);
                ret.put(Keys.MSG, langPropsService.get("updateSuccLabel"));

                LOGGER.log(Level.INFO, "API Key updated [id={}]", id);
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Updates API Key failed", e);
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, langPropsService.get("updateFailLabel"));
            }
        }
    }

    /**
     * Removes an API Key by path variable id.
     *
     * @param context the specified request context
     */
    public void removeApiKeyById(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final String id = context.pathVar("id");
        removeKeyInternal(id, ret);
    }

    /**
     * Removes an API Key via query param {@code id}, or removes the single key when only one exists.
     *
     * @param context the specified request context
     */
    public void removeApiKey(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        final String id = context.param("id");
        if (StringUtils.isNotBlank(id)) {
            removeKeyInternal(id, ret);
            return;
        }

        synchronized (LOCK) {
            try {
                final JSONArray keys = loadAndMigrateKeys();
                if (0 == keys.length()) {
                    ret.put(Keys.CODE, StatusCodes.SUCC);
                    ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
                    return;
                }
                if (1 == keys.length()) {
                    saveKeys(new JSONArray());
                    ret.put(Keys.CODE, StatusCodes.SUCC);
                    ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
                    LOGGER.log(Level.INFO, "Single API Key removed");
                    return;
                }

                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, "Multiple API Keys exist, please specify DELETE /console/api-key/{id}");
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Removes API Key failed", e);
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
            }
        }
    }

    private void removeKeyInternal(final String id, final JSONObject ret) {
        if (StringUtils.isBlank(id)) {
            ret.put(Keys.CODE, StatusCodes.ERR);
            ret.put(Keys.MSG, "API Key id is required");
            return;
        }

        synchronized (LOCK) {
            try {
                final JSONArray keys = loadAndMigrateKeys();
                final JSONArray remaining = new JSONArray();
                boolean removed = false;
                for (int i = 0; i < keys.length(); i++) {
                    final JSONObject item = keys.optJSONObject(i);
                    if (null == item) {
                        continue;
                    }
                    if (id.equals(item.optString("id"))) {
                        removed = true;
                        continue;
                    }
                    remaining.put(item);
                }

                if (!removed) {
                    ret.put(Keys.CODE, StatusCodes.ERR);
                    ret.put(Keys.MSG, "API Key not found");
                    return;
                }

                saveKeys(remaining);
                ret.put(Keys.CODE, StatusCodes.SUCC);
                ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
                LOGGER.log(Level.INFO, "API Key removed [id={}]", id);
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Removes API Key failed", e);
                ret.put(Keys.CODE, StatusCodes.ERR);
                ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
            }
        }
    }

    /**
     * Loads the API Keys array from option storage and transparently migrates legacy {@link Option#ID_C_API_KEY}.
     * Caller must hold {@link #LOCK}.
     *
     * @return the API Keys JSON array
     * @throws ServiceException if option persistence fails
     */
    private JSONArray loadAndMigrateKeys() throws ServiceException {
        final JSONObject apiKeysOpt = optionQueryService.getOptionById(Option.ID_C_API_KEYS);
        if (null == apiKeysOpt) {
            final JSONArray migrated = new JSONArray();
            final JSONObject legacyOpt = optionQueryService.getOptionById(Option.ID_C_API_KEY);
            if (null != legacyOpt) {
                final String legacyKey = StringUtils.trimToEmpty(legacyOpt.optString(Option.OPTION_VALUE));
                if (StringUtils.isNotBlank(legacyKey)) {
                    final long now = System.currentTimeMillis();
                    final JSONObject entry = new JSONObject();
                    entry.put("id", now + RandomStringUtils.randomAlphanumeric(4).toLowerCase());
                    entry.put("name", "默认密钥");
                    entry.put("key", legacyKey);
                    entry.put("enabled", true);
                    entry.put("createdAt", now);
                    entry.put("lastUsedAt", 0L);
                    entry.put("lastUsedIp", "");
                    entry.put("lastUsedEndpoint", "");
                    entry.put("callCount", 0L);
                    migrated.put(entry);

                    saveKeys(migrated);

                    // Clear legacy option value to prevent repeated migration
                    final JSONObject clearLegacy = new JSONObject();
                    clearLegacy.put(Keys.OBJECT_ID, Option.ID_C_API_KEY);
                    clearLegacy.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_API);
                    clearLegacy.put(Option.OPTION_VALUE, "");
                    optionMgmtService.addOrUpdateOption(clearLegacy);

                    LOGGER.log(Level.INFO, "Migrated legacy API Key to multi-key storage");
                }
            }
            return migrated;
        }

        final String raw = apiKeysOpt.optString(Option.OPTION_VALUE, "");
        if (StringUtils.isBlank(raw)) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (final Exception e) {
            LOGGER.log(Level.WARN, "Invalid apiKeys JSON stored in option, resetting empty array", e);
            return new JSONArray();
        }
    }

    /**
     * Persists the API Keys JSON array as a fresh option JSONObject to avoid mutating cached instances.
     *
     * @param keys the API Keys array
     * @throws ServiceException if persistence fails
     */
    private void saveKeys(final JSONArray keys) throws ServiceException {
        final JSONObject option = new JSONObject();
        option.put(Keys.OBJECT_ID, Option.ID_C_API_KEYS);
        option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_API);
        option.put(Option.OPTION_VALUE, keys.toString());
        optionMgmtService.addOrUpdateOption(option);
    }

    /**
     * Masks an API Key for safe display.
     *
     * @param key the raw key
     * @return masked key string
     */
    static String maskKey(final String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        if (key.length() > 11) {
            return key.substring(0, 7) + "****" + key.substring(key.length() - 4);
        }
        return "****";
    }
}
