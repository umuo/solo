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
package org.b3log.solo.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.Session;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.Ids;
import org.b3log.solo.model.TreeHole;
import org.b3log.solo.processor.TreeHoleProcessor;
import org.b3log.solo.repository.TreeHoleRepository;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: gitsilence
 * @description: 树洞业务逻辑层
 * @date: 2021/6/26 10:45 上午
 **/
@Service
public class TreeHoleService {

    private static final Logger LOGGER = LogManager.getLogger(TreeHoleService.class);

    private static final int MAX_CONTENT_LENGTH = 5000;

    @Inject
    private TreeHoleRepository treeHoleRepository;

    /**
     * Escapes HTML metacharacters while keeping Unicode characters and line breaks intact.
     *
     * @param input raw user input
     * @return HTML-escaped string
     */
    public static String sanitizeHtml(final String input) {
        if (null == input) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public String addMessage(final JSONObject requestJSON) throws ServiceException {
        if (null == requestJSON) {
            throw new ServiceException("留言内容不能为空");
        }
        final JSONObject input = requestJSON.optJSONObject(TreeHole.hole);
        if (null == input) {
            throw new ServiceException("留言内容不能为空");
        }

        final String rawContent = StringUtils.trimToEmpty(input.optString(TreeHole.content));
        if (StringUtils.isBlank(rawContent)) {
            throw new ServiceException("留言内容不能为空");
        }
        if (rawContent.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException("留言内容过长");
        }

        final Transaction transaction = treeHoleRepository.beginTransaction();
        try {
            final String ret = Ids.genTimeMillisId();
            // Build record strictly from whitelisted fields to prevent client-side tampering (e.g. isShow, likeCount)
            final JSONObject record = new JSONObject();
            record.put(Keys.OBJECT_ID, ret);
            record.put(TreeHole.content, sanitizeHtml(rawContent));
            record.put(TreeHole.clientIp, StringUtils.defaultString(input.optString(TreeHole.clientIp), ""));
            record.put(TreeHole.location, StringUtils.defaultString(input.optString(TreeHole.location), ""));
            record.put(TreeHole.createTime, String.valueOf(System.currentTimeMillis()));
            record.put(TreeHole.isShow, 0);
            record.put(TreeHole.likeCount, 0);
            record.put(TreeHole.hole_random_double, Math.random());
            record.put(TreeHole.watchedCount, 0);

            treeHoleRepository.add(record);
            transaction.commit();
            return ret;
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new ServiceException(e);
        }
    }

    public JSONObject getRandomMessage(final Session session) throws ServiceException {
        return treeHoleRepository.getRandom(session);
    }

    public long countByIp(final long todayDate, final String realIp) {
        return treeHoleRepository.countByIp(todayDate, realIp);
    }

    public void updateCountById(final String column, final String id) throws ServiceException {
        final Transaction transaction = treeHoleRepository.beginTransaction();
        try {
            treeHoleRepository.updateCountById(column, id);
            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.error("update count ex, column: {}, id: {}, msg: {}", column, id, e.getMessage());
            throw new ServiceException(e);
        }
    }

    /**
     * Gets aggregate TreeHole statistics for admin console.
     *
     * @return stats JSON
     * @throws ServiceException if query fails
     */
    public JSONObject getStats() throws ServiceException {
        try {
            return treeHoleRepository.getStats();
        } catch (final Exception e) {
            LOGGER.error("get tree hole stats failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets paginated TreeHole messages for admin console.
     *
     * @param pageNum      1-based page number
     * @param pageSize     page size (clamped to 1..100)
     * @param statusFilter optional {@code 0} or {@code 1}, or {@code null} for all
     * @param keyword      optional search keyword
     * @return paginated result JSON
     * @throws ServiceException if query fails
     */
    public JSONObject getAdminList(final int pageNum, final int pageSize, final Integer statusFilter, final String keyword)
            throws ServiceException {
        final int safePageNum = Math.max(1, pageNum);
        final int safePageSize = Math.min(100, Math.max(1, pageSize));
        try {
            return treeHoleRepository.getAdminList(safePageNum, safePageSize, statusFilter, keyword);
        } catch (final Exception e) {
            LOGGER.error("get tree hole admin list failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Updates visibility status ({@code isShow}) for the given TreeHole ids.
     *
     * @param ids    message ids
     * @param isShow {@code 1} for visible, {@code 0} for hidden
     * @return count of actually updated records
     * @throws ServiceException if persistence fails
     */
    public int updateStatus(final List<String> ids, final int isShow) throws ServiceException {
        final Set<String> uniqueIds = normalizeIds(ids);
        if (uniqueIds.isEmpty()) {
            return 0;
        }
        final int target = (1 == isShow) ? 1 : 0;

        final Transaction transaction = treeHoleRepository.beginTransaction();
        int updatedCount = 0;
        try {
            for (final String id : uniqueIds) {
                final JSONObject existing = treeHoleRepository.get(id);
                if (null != existing) {
                    existing.put(TreeHole.isShow, target);
                    treeHoleRepository.update(id, existing, TreeHole.isShow);
                    updatedCount++;
                }
            }
            transaction.commit();
            return updatedCount;
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.error("update tree hole status failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Deletes TreeHole messages by id list and clears in-memory view/like dedup entries for deleted ids.
     *
     * @param ids message ids
     * @return count of deleted records
     * @throws ServiceException if persistence fails
     */
    public int removeMessages(final List<String> ids) throws ServiceException {
        final Set<String> uniqueIds = normalizeIds(ids);
        if (uniqueIds.isEmpty()) {
            return 0;
        }

        final Transaction transaction = treeHoleRepository.beginTransaction();
        int deletedCount = 0;
        final List<String> deletedIds = new java.util.ArrayList<>();
        try {
            for (final String id : uniqueIds) {
                final JSONObject existing = treeHoleRepository.get(id);
                if (null != existing) {
                    treeHoleRepository.remove(id);
                    deletedIds.add(id);
                    deletedCount++;
                }
            }
            transaction.commit();
            for (final String deletedId : deletedIds) {
                TreeHoleProcessor.WATCHED_SET.removeIf(key -> key.endsWith(deletedId));
                TreeHoleProcessor.LIKED_SET.removeIf(key -> key.endsWith(deletedId));
            }
            return deletedCount;
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.error("remove tree hole messages failed", e);
            throw new ServiceException(e);
        }
    }

    private Set<String> normalizeIds(final List<String> ids) {
        final Set<String> uniqueIds = new LinkedHashSet<>();
        if (null == ids) {
            return uniqueIds;
        }
        for (final String id : ids) {
            final String trimmed = StringUtils.trimToEmpty(id);
            if (StringUtils.isNotBlank(trimmed)) {
                uniqueIds.add(trimmed);
            }
        }
        return uniqueIds;
    }
}
