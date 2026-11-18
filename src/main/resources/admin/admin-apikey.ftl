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
  .apikey-banner {
    background-color: #f0fdfa;
    border: 1px solid #1fb5ad;
    border-radius: 4px;
    padding: 15px;
    margin-bottom: 20px;
  }
  .apikey-banner__title {
    color: #137a75;
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 8px;
  }
  .apikey-banner__hint {
    color: #555;
    margin-bottom: 10px;
    line-height: 1.6;
  }
  .apikey-banner__row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .apikey-banner__code {
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    font-size: 13px;
    background-color: #fff;
    border: 1px solid #cceae7;
    border-radius: 4px;
    padding: 7px 12px;
    color: #222;
    word-break: break-all;
    flex: 1;
    min-width: 240px;
  }
  .apikey-create-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .apikey-create-row input[type="text"] {
    flex: 1;
    min-width: 220px;
    max-width: 420px;
    height: 34px;
    padding: 6px 12px;
    box-sizing: border-box;
    border: 1px solid #e2e2e4;
    border-radius: 4px;
    font-size: 14px;
    color: #333;
    outline: none;
  }
  .apikey-create-row input[type="text"]:focus {
    border-color: #1fb5ad;
  }
  .apikey-usage-tip {
    margin-top: 12px;
    padding: 10px 12px;
    background-color: #fafafa;
    border-radius: 4px;
    border: 1px solid #eff2f7;
    color: #666;
    line-height: 1.7;
    word-break: break-all;
  }
  .apikey-usage-tip code {
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    background-color: #eee;
    padding: 2px 6px;
    border-radius: 3px;
    color: #333;
  }
  .apikey-header-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
  .apikey-summary {
    color: #888;
    font-size: 12px;
    margin-left: 10px;
  }
  .apikey-table-wrap {
    overflow-x: auto;
  }
  .apikey-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 820px;
  }
  .apikey-table th {
    background-color: #fafafa;
    border-bottom: 1px solid #ddd;
    padding: 12px 10px;
    text-align: left;
    font-weight: normal;
    color: #555;
    white-space: nowrap;
  }
  .apikey-table td {
    border-bottom: 1px solid #eee;
    padding: 12px 10px;
    vertical-align: middle;
    color: #555;
  }
  .apikey-table tbody tr:hover td {
    background-color: #fafafa;
  }
  .apikey-mono {
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    font-size: 12px;
    color: #444;
  }
  .apikey-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
    line-height: 1.5;
    white-space: nowrap;
  }
  .apikey-badge--enabled {
    background-color: #e6f7f5;
    color: #137a75;
    border: 1px solid #b5e6e1;
  }
  .apikey-badge--disabled {
    background-color: #f5f5f5;
    color: #888;
    border: 1px solid #ddd;
  }
  .apikey-sub {
    display: block;
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }
  .apikey-actions {
    white-space: nowrap;
  }
  .apikey-actions button {
    margin-right: 4px;
    padding: 3px 8px;
    font-size: 12px;
    line-height: 16px;
  }
  button.apikey-btn-secondary {
    background-color: #fff;
    border-color: #d9d9d9;
    color: #555;
  }
  button.apikey-btn-secondary:hover {
    background-color: #f5f5f5;
    border-color: #bbb;
    color: #333;
  }
  button.apikey-btn-danger {
    background-color: #fa8564;
    border-color: #fa8564;
    color: #fff;
  }
  button.apikey-btn-danger:hover {
    background-color: #ec6459;
    border-color: #ec6459;
  }
</style>

<div id="apiKeyNewBanner" class="apikey-banner fn__none">
    <div class="apikey-banner__title">API Key 创建成功</div>
    <div class="apikey-banner__hint">
        请立即复制并妥善保存此密钥，出于安全考虑，关闭或刷新页面后将不再显示完整明文：
    </div>
    <div class="apikey-banner__row">
        <code id="apiKeyNewValue" class="apikey-banner__code"></code>
        <button type="button" onclick="admin.apikey.copyKey()">复制密钥</button>
        <button type="button" class="apikey-btn-secondary" onclick="admin.apikey.hideNewBanner()">关闭</button>
    </div>
</div>

<div class="module-panel">
    <div class="module-header">
        <h2>创建 API Key</h2>
    </div>
    <div class="module-body">
        <div class="apikey-create-row">
            <input id="apiKeyNameInput" type="text" maxlength="50"
                   placeholder="请输入备注名称（例如：Antigravity Agent / Codex）"/>
            <button type="button" onclick="admin.apikey.create()">生成 API Key</button>
        </div>
        <div class="apikey-usage-tip">
            在请求控制台接口时携带请求头 <code>X-Solo-API-Key</code> 即可鉴权（默认拥有管理员权限）。调用示例：<br/>
            <code>curl -H "X-Solo-API-Key: sk-xxxx" ${servePath}/console/tags</code>
        </div>
    </div>
</div>

<div class="module-panel">
    <div class="module-header apikey-header-bar">
        <h2>
            API Key 列表
            <span id="apiKeySummary" class="apikey-summary"></span>
        </h2>
        <button type="button" class="apikey-btn-secondary small" onclick="admin.apikey.getList()">刷新</button>
    </div>
    <div class="module-body apikey-table-wrap">
        <table class="apikey-table">
            <thead>
            <tr>
                <th>名称</th>
                <th>密钥 (脱敏)</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>最近调用时间</th>
                <th>最近调用详情</th>
                <th>累计调用次数</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody id="apiKeyTableBody">
            <tr>
                <td colspan="8" style="text-align: center; color: #999;">加载中...</td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
${plugins}
