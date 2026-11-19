<#--

    Solo - A small and beautiful blogging system written in Java.
    Copyright (c) 2010-present, b3log.org

    Solo is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
            http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<style>
  .th-stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 18px;
  }
  .th-stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 18px 20px;
    border: 1px solid #eef1f5;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .th-stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }
  .th-stat-icon--total { background: #eaf4ff; color: #2575fc; }
  .th-stat-icon--visible { background: #e6f7f5; color: #1fb5ad; }
  .th-stat-icon--pending { background: #fef3c7; color: #d97706; }
  .th-stat-icon--likes { background: #ffe4e6; color: #e11d48; }
  .th-stat-info {
    min-width: 0;
  }
  .th-stat-label {
    font-size: 12px;
    color: #8492a6;
    margin-bottom: 4px;
  }
  .th-stat-value {
    font-size: 24px;
    font-weight: 700;
    color: #1f2d3d;
    line-height: 1.2;
  }
  .th-stat-value--pending {
    color: #d97706;
  }
  .th-stat-sub {
    font-size: 11px;
    color: #99a9bf;
    margin-top: 3px;
  }

  .th-panel {
    border-radius: 8px;
    overflow: hidden;
  }
  .th-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 16px 18px;
    background: #fafafa;
    border-bottom: 1px solid #eff2f7;
  }
  .th-filter-tabs {
    display: inline-flex;
    align-items: center;
    background: #eff2f7;
    padding: 3px;
    border-radius: 6px;
    gap: 4px;
  }
  button.th-tab {
    background: transparent;
    border: none;
    color: #606266;
    font-size: 13px;
    padding: 6px 14px;
    border-radius: 4px;
    line-height: 18px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  button.th-tab:hover {
    background: rgba(255, 255, 255, 0.7);
    color: #1f2d3d;
    border: none;
  }
  button.th-tab.is-active {
    background: #fff;
    color: #1fb5ad;
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }

  .th-search-wrap {
    flex: 1;
    min-width: 220px;
    max-width: 340px;
  }
  .th-search-wrap input[type="search"],
  .th-search-wrap input[type="text"] {
    width: 100%;
    height: 34px;
    padding: 6px 12px;
    box-sizing: border-box;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 13px;
    color: #303133;
    background: #fff;
    outline: none;
  }
  .th-search-wrap input:focus {
    border-color: #1fb5ad;
  }

  .th-toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  button.th-btn {
    padding: 6px 13px;
    font-size: 13px;
    line-height: 18px;
    border-radius: 5px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  button.th-btn--approve {
    background: #1fb5ad;
    border-color: #1fb5ad;
    color: #fff;
  }
  button.th-btn--approve:hover {
    background: #199c95;
    border-color: #199c95;
  }
  button.th-btn--delete {
    background: #fa8564;
    border-color: #fa8564;
    color: #fff;
  }
  button.th-btn--delete:hover {
    background: #ec6459;
    border-color: #ec6459;
  }
  button.th-btn--secondary {
    background: #fff;
    border-color: #dcdfe6;
    color: #606266;
  }
  button.th-btn--secondary:hover {
    background: #f5f7fa;
    border-color: #c0c4cc;
    color: #303133;
  }

  .th-table-wrap {
    overflow-x: auto;
  }
  .th-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 880px;
  }
  .th-table th {
    background: #fafbfc;
    border-bottom: 1px solid #e4e7ed;
    padding: 12px 12px;
    text-align: left;
    font-weight: 600;
    font-size: 12.5px;
    color: #606266;
    white-space: nowrap;
  }
  .th-table td {
    border-bottom: 1px solid #f0f2f5;
    padding: 13px 12px;
    vertical-align: middle;
    color: #303133;
    font-size: 13px;
  }
  .th-table tbody tr:hover td {
    background: #fafcff;
  }
  .th-col-check {
    width: 40px;
    text-align: center !important;
  }
  .th-col-center {
    text-align: center !important;
  }

  .th-content-cell {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    max-width: 430px;
    cursor: pointer;
  }
  .th-content-text {
    flex: 1;
    min-width: 0;
    line-height: 1.6;
    color: #2c3e50;
  }
  .th-content-text--collapsed {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .th-content-cell.is-expanded .th-content-text {
    white-space: pre-wrap;
    word-break: break-word;
  }
  button.th-expand-btn {
    background: transparent;
    border: none;
    color: #909399;
    padding: 2px 5px;
    font-size: 14px;
    line-height: 1;
    border-radius: 4px;
    flex-shrink: 0;
    transition: transform 0.15s ease, color 0.15s ease;
  }
  button.th-expand-btn:hover {
    background: #f0f2f5;
    border: none;
    color: #1fb5ad;
  }
  .th-content-cell.is-expanded button.th-expand-btn {
    transform: rotate(180deg);
    color: #1fb5ad;
  }

  .th-id-tag {
    display: inline-block;
    font-size: 11px;
    color: #909399;
    margin-top: 3px;
    font-family: Menlo, Monaco, Consolas, monospace;
  }
  .th-ip {
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    font-size: 12.5px;
    color: #303133;
  }
  .th-loc {
    font-size: 12px;
    color: #909399;
    margin-top: 3px;
  }
  .th-metric {
    font-weight: 500;
    color: #475569;
    white-space: nowrap;
  }
  .th-badge {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 999px;
    font-size: 12px;
    line-height: 1.4;
    white-space: nowrap;
    font-weight: 500;
  }
  .th-badge--visible {
    background: #e6f7f5;
    color: #137a75;
    border: 1px solid #b5e6e1;
  }
  .th-badge--pending {
    background: #fff7ed;
    color: #d97706;
    border: 1px solid #fed7aa;
  }

  .th-actions {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    white-space: nowrap;
  }
  .th-switch {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    user-select: none;
  }
  .th-switch-track {
    position: relative;
    width: 36px;
    height: 20px;
    border-radius: 999px;
    background: #d1d5db;
    transition: background-color 0.2s ease;
    display: inline-block;
  }
  .th-switch-thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s ease;
  }
  .th-switch.is-on .th-switch-track {
    background: #1fb5ad;
  }
  .th-switch.is-on .th-switch-thumb {
    transform: translateX(16px);
  }
  .th-switch-label {
    font-size: 12px;
    color: #606266;
  }
  button.th-action-del {
    background: transparent;
    border: 1px solid #fbd3c8;
    color: #e65a42;
    padding: 3px 9px;
    font-size: 12px;
    line-height: 16px;
    border-radius: 4px;
  }
  button.th-action-del:hover {
    background: #fa8564;
    border-color: #fa8564;
    color: #fff;
  }

  .th-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 14px 18px;
    border-top: 1px solid #eff2f7;
    background: #fff;
    font-size: 13px;
    color: #606266;
  }
  .th-page-btns {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }
  button.th-page-btn {
    background: #fff;
    border: 1px solid #dcdfe6;
    color: #606266;
    padding: 4px 10px;
    font-size: 12px;
    line-height: 18px;
    border-radius: 4px;
  }
  button.th-page-btn:hover:not(:disabled) {
    background: #f0fdfa;
    border-color: #1fb5ad;
    color: #1fb5ad;
  }
  button.th-page-btn.is-current {
    background: #1fb5ad;
    border-color: #1fb5ad;
    color: #fff;
  }
  button.th-page-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  @media (max-width: 1100px) {
    .th-stats-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (max-width: 600px) {
    .th-stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

<div class="th-stats-grid">
    <div class="th-stat-card">
        <div class="th-stat-icon th-stat-icon--total" aria-hidden="true">💬</div>
        <div class="th-stat-info">
            <div class="th-stat-label">总留言</div>
            <div class="th-stat-value" id="thStatTotal">-</div>
        </div>
    </div>
    <div class="th-stat-card">
        <div class="th-stat-icon th-stat-icon--visible" aria-hidden="true">✓</div>
        <div class="th-stat-info">
            <div class="th-stat-label">展示中</div>
            <div class="th-stat-value" id="thStatVisible">-</div>
        </div>
    </div>
    <div class="th-stat-card">
        <div class="th-stat-icon th-stat-icon--pending" aria-hidden="true">⏳</div>
        <div class="th-stat-info">
            <div class="th-stat-label">待审核</div>
            <div class="th-stat-value th-stat-value--pending" id="thStatPending">-</div>
        </div>
    </div>
    <div class="th-stat-card">
        <div class="th-stat-icon th-stat-icon--likes" aria-hidden="true">♥</div>
        <div class="th-stat-info">
            <div class="th-stat-label">累计共鸣</div>
            <div class="th-stat-value" id="thStatLikes">-</div>
            <div class="th-stat-sub">累计遇见 <span id="thStatWatched">0</span> 次</div>
        </div>
    </div>
</div>

<div class="module-panel th-panel">
    <div class="th-toolbar">
        <div class="th-filter-tabs" role="tablist" aria-label="树洞状态筛选">
            <button type="button" class="th-tab is-active" data-status="all">全部</button>
            <button type="button" class="th-tab" data-status="pending">待审核 (<span id="thTabPending">0</span>)</button>
            <button type="button" class="th-tab" data-status="visible">已展示</button>
        </div>

        <div class="th-search-wrap">
            <input id="thSearchInput" type="search" placeholder="搜索内容、IP 或地区（如：杭州、浙江）" autocomplete="off"/>
        </div>

        <div class="th-toolbar-actions">
            <button type="button" class="th-btn th-btn--approve" id="thBatchApproveBtn">✓ 批量通过</button>
            <button type="button" class="th-btn th-btn--delete" id="thBatchDeleteBtn">🗑 批量删除</button>
            <button type="button" class="th-btn th-btn--secondary" id="thRefreshBtn">刷新</button>
        </div>
    </div>

    <div class="th-table-wrap">
        <table class="th-table">
            <thead>
            <tr>
                <th class="th-col-check"><input type="checkbox" id="thSelectAll" aria-label="全选本页"/></th>
                <th>留言内容</th>
                <th>提交时间</th>
                <th>IP 与归属地</th>
                <th class="th-col-center">遇见 / 点赞数</th>
                <th class="th-col-center">状态</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody id="thTableBody">
            <tr>
                <td colspan="7" style="text-align: center; color: #999; padding: 28px 0;">加载中...</td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="th-pagination" id="thPagination"></div>
</div>
${plugins}
