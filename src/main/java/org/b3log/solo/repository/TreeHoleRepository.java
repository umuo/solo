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
package org.b3log.solo.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.Session;
import org.b3log.latke.repository.*;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.latke.service.ServiceException;
import org.b3log.solo.model.Common;
import org.b3log.solo.model.TreeHole;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: gitsilence
 * @description: TreeHole repository
 * @date: 2021/6/26 11:15 上午
 **/
@Repository
public class TreeHoleRepository extends AbstractRepository {

    private static final Logger LOGGER = LogManager.getLogger(TreeHoleRepository.class);

    private static final int MAX_SESSION_SEEN_IDS = 200;

    /**
     * Constructs a repository with the specified name.
     */
    public TreeHoleRepository() {
        super(TreeHole.hole);
    }

    /**
     * Gets one random visible TreeHole message while deduplicating messages already seen in the session.
     *
     * @param session current HTTP session, may be {@code null}
     * @return random visible message JSON, or empty JSON when none available
     * @throws ServiceException if repository query fails
     */
    public JSONObject getRandom(final Session session) throws ServiceException {
        try {
            final Set<String> seenIds = parseSeenIds(session);
            long unseenCount = count(buildVisibleRandomQuery(seenIds));

            if (0 == unseenCount && !seenIds.isEmpty()) {
                // Current visible pool exhausted in this session; reset cycle and query all visible messages.
                seenIds.clear();
                unseenCount = count(buildVisibleRandomQuery(seenIds));
            }
            if (0 == unseenCount) {
                return new JSONObject();
            }

            final int bound = (int) Math.min(unseenCount, Integer.MAX_VALUE);
            final int randomPage = ThreadLocalRandom.current().nextInt(bound) + 1;
            final Query pageQuery = buildVisibleRandomQuery(seenIds)
                    .setPage(randomPage, 1)
                    .setPageCount(1)
                    .addSort(Keys.OBJECT_ID, SortDirection.DESCENDING);
            final List<JSONObject> records = getList(pageQuery);
            if (records.isEmpty()) {
                return new JSONObject();
            }

            final JSONObject picked = records.get(0);
            if (null != session) {
                final String pickedId = picked.optString(Keys.OBJECT_ID);
                if (StringUtils.isNotBlank(pickedId)) {
                    seenIds.add(pickedId);
                    while (seenIds.size() > MAX_SESSION_SEEN_IDS) {
                        final String oldest = seenIds.iterator().next();
                        seenIds.remove(oldest);
                    }
                    session.setAttribute(Common.SESSION_LIST_KEY, String.join(",", seenIds));
                }
            }
            return picked;
        } catch (final Exception e) {
            LOGGER.error("get random message failed", e);
            throw new ServiceException(e);
        }
    }

    private Set<String> parseSeenIds(final Session session) {
        final Set<String> seenIds = new LinkedHashSet<>();
        if (null == session) {
            return seenIds;
        }
        final Object attr = session.getAttribute(Common.SESSION_LIST_KEY);
        if (!(attr instanceof String)) {
            return seenIds;
        }
        final String raw = (String) attr;
        if (StringUtils.isBlank(raw)) {
            return seenIds;
        }
        for (final String part : raw.split(",")) {
            final String cleaned = part.replace("'", "").replace("\"", "").trim();
            if (StringUtils.isNotBlank(cleaned)) {
                seenIds.add(cleaned);
            }
        }
        return seenIds;
    }

    private Query buildVisibleRandomQuery(final Set<String> seenIds) {
        final List<Filter> filters = new ArrayList<>();
        filters.add(new PropertyFilter(TreeHole.isShow, FilterOperator.EQUAL, 1));
        if (!seenIds.isEmpty()) {
            filters.add(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.NOT_IN, new ArrayList<>(seenIds)));
        }
        final Filter filter = 1 == filters.size() ? filters.get(0) : CompositeFilterOperator.and(filters);
        return new Query().setFilter(filter);
    }

    @Override
    public List<JSONObject> getRandomly(int fetchSize) throws RepositoryException {
        final List<JSONObject> ret = new ArrayList<>();
        int loops = 0;
        do {
            loops++;
            double min = Math.random();
            double max = Math.random();
            if (min > max) {
                final double tmp = min;
                min = max;
                max = tmp;
            }
            final Query query = new Query().setFilter(
                    CompositeFilterOperator.and(
                            new PropertyFilter(TreeHole.hole_random_double, FilterOperator.GREATER_THAN_OR_EQUAL, min),
                            new PropertyFilter(TreeHole.hole_random_double, FilterOperator.LESS_THAN_OR_EQUAL, max),
                            new PropertyFilter(TreeHole.isShow, FilterOperator.EQUAL, 1))).
                    setPage(1, (int) Math.ceil(fetchSize / 5)).setPageCount(1);
            if (0.05 <= min) {
                query.addSort(Keys.OBJECT_ID, SortDirection.DESCENDING);
            }

            final List<JSONObject> records = getList(query);
            for (final JSONObject record : records) {
                final String id = record.optString(Keys.OBJECT_ID);
                boolean contain = false;
                for (final JSONObject retRecord : ret) {
                    if (retRecord.optString(Keys.OBJECT_ID).equals(id)) {
                        contain = true;
                        break;
                    }
                }
                if (!contain) {
                    ret.add(record);
                }
            }
        } while (1 <= fetchSize - ret.size() && 10 >= loops);
        return ret;
    }

    public long countByIp(long todayDate, String realIp) {
        Query query = new Query()
                .setFilter(CompositeFilterOperator.and(
                        new PropertyFilter(TreeHole.createTime, FilterOperator.GREATER_THAN_OR_EQUAL, todayDate),
                        new PropertyFilter(TreeHole.clientIp, FilterOperator.EQUAL, realIp)
                ));
        try {
            return count(query);
        } catch (RepositoryException e) {
            LOGGER.error("countByIp failed", e);
            return Common.MAX_MES_DAY_COUNT;
        }
    }

    public void updateCountById(String column, String id) throws RepositoryException {
        JSONObject jsonObject = get(id);
        if (jsonObject == null) {
            throw new RepositoryException("TreeHole record not found [" + id + "]");
        }
        long count = jsonObject.optLong(column) + 1;
        jsonObject.put(column, count);
        update(id, jsonObject, column);
    }
}
