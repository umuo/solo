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
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.ioc.Singleton;
import org.b3log.solo.util.Solos;

/**
 * The admin auth check middleware for admin console.
 * <p>
 * Requires the user to be an admin (not just logged in).
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.1.0.0, Nov 18, 2026
 * @since 2.9.5
 */
@Singleton
public class ConsoleAdminAuthMidware {

    public void handle(final RequestContext context) {
        // 1. Check API Key in header first (API Key always has admin privilege)
        final String apiKey = context.header("X-Solo-API-Key");
        if (StringUtils.isNotBlank(apiKey)) {
            if (ConsoleAuthMidware.authenticateByApiKey(apiKey, context)) {
                context.handle();
                return;
            }
            context.sendError(401);
            context.abort();
            return;
        }

        // 2. Fall back to Cookie-based admin check
        if (!Solos.isAdminLoggedIn(context)) {
            context.sendError(401);
            context.abort();
            return;
        }

        context.handle();
    }
}
