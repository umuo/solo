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
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.ioc.BeanManager;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.model.Role;
import org.b3log.latke.model.User;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.Option;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Solos;
import org.json.JSONObject;

/**
 * The common auth check middleware for admin console.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.1.0.0, Nov 18, 2026
 * @since 2.9.5
 */
@Singleton
public class ConsoleAuthMidware {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ConsoleAuthMidware.class);

    public void handle(final RequestContext context) {
        // 1. Check API Key in header first
        final String apiKey = context.header("X-Solo-API-Key");
        if (StringUtils.isNotBlank(apiKey)) {
            if (authenticateByApiKey(apiKey, context)) {
                context.handle();
                return;
            }
            context.sendError(401);
            context.abort();
            return;
        }

        // 2. Fall back to Cookie-based authentication
        final JSONObject currentUser = Solos.getCurrentUser(context);
        if (null == currentUser) {
            context.sendError(401);
            context.abort();
            return;
        }

        final String userRole = currentUser.optString(User.USER_ROLE);
        if (Role.VISITOR_ROLE.equals(userRole)) {
            context.sendError(403);
            context.abort();
        }

        context.handle();
    }

    /**
     * Authenticates by API Key.
     *
     * @param apiKey  the API Key from request header
     * @param context the request context
     * @return {@code true} if authentication succeeds
     */
    static boolean authenticateByApiKey(final String apiKey, final RequestContext context) {
        try {
            final BeanManager beanManager = BeanManager.getInstance();
            final OptionQueryService optionQueryService = beanManager.getReference(OptionQueryService.class);
            final JSONObject apiKeyOpt = optionQueryService.getOptionById(Option.ID_C_API_KEY);
            if (null == apiKeyOpt) {
                return false;
            }

            final String storedKey = apiKeyOpt.optString(Option.OPTION_VALUE);
            if (StringUtils.isBlank(storedKey) || !apiKey.equals(storedKey)) {
                return false;
            }

            // API Key valid, set admin user in context
            final UserQueryService userQueryService = beanManager.getReference(UserQueryService.class);
            final JSONObject admin = userQueryService.getAdmin();
            if (null == admin) {
                return false;
            }

            context.attr(Common.CURRENT_USER, admin);
            return true;
        } catch (final Exception e) {
            LOGGER.log(Level.WARN, "API Key authentication failed", e);
            return false;
        }
    }
}
