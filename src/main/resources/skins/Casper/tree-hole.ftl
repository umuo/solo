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
<#include "../../common-template/macro-common_head.ftl">
<!DOCTYPE html>
<html>
<head>
<@head title="树洞 - ${blogTitle}">
    <link rel="stylesheet" href="${staticServePath}/skins/${skinDirName}/css/base.css?${staticResourceVersion}"/>
    <link rel="stylesheet" href="${servePath}/skins/${skinDirName}/css/custom.css?${staticResourceVersion}"/>
</@head>
</head>
<body class="fn__flex-column">
<div id="pjax" class="fn__flex-1">
    <#if pjax><!---- pjax {#pjax} start ----><title>树洞 - ${blogTitle}</title></#if>
    <#include "macro-header.ftl">
    <@header type='other'></@header>

    <main id="treeHoleApp" class="wrapper treehole-page" data-serve-path="${servePath}">
        <div class="treehole-hero">
            <h1 class="treehole-hero__title">树洞</h1>
            <p class="treehole-hero__subtitle">把秘密留在这里，或偶遇一段陌生人的心事</p>
            <button type="button" class="treehole-hero__cta" id="treeHoleOpenModalBtn">+ 投递树洞</button>
        </div>

        <section class="treehole-card" aria-live="polite">
            <div class="treehole-card__meta">
                <span class="treehole-card__no" id="treeHoleNo">NO. ------</span>
                <span class="treehole-card__watched" id="treeHoleWatched">
                    <svg class="treehole-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 5C6.5 5 2.1 8.4 0.5 12c1.6 3.6 6 7 11.5 7s9.9-3.4 11.5-7C21.9 8.4 17.5 5 12 5zm0 11.5c-2.5 0-4.5-2-4.5-4.5S9.5 7.5 12 7.5s4.5 2 4.5 4.5-2 4.5-4.5 4.5zm0-7a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z"/>
                    </svg>
                    <span>已被 <strong id="treeHoleWatchedNum">0</strong> 人遇见</span>
                </span>
            </div>

            <div class="treehole-card__body" id="treeHoleContent">
                <p class="treehole-card__loading">正在翻阅树洞里的心事...</p>
            </div>

            <div class="treehole-card__footer">
                <button type="button" class="treehole-btn treehole-btn--like" id="treeHoleLikeBtn">
                    <span class="treehole-heart" aria-hidden="true">♥</span>
                    <span>共鸣 (<span id="treeHoleLikeCount">0</span>)</span>
                </button>
                <button type="button" class="treehole-btn treehole-btn--next" id="treeHoleNextBtn">
                    <svg class="treehole-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM7.5 18c-.83 0-1.5-.67-1.5-1.5S6.67 15 7.5 15s1.5.67 1.5 1.5S8.33 18 7.5 18zm0-9C6.67 9 6 8.33 6 7.5S6.67 6 7.5 6 9 6.67 9 7.5 8.33 9 7.5 9zm4.5 4.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4.5 4.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm0-9c-.83 0-1.5-.67-1.5-1.5S15.67 6 16.5 6s1.5.67 1.5 1.5S17.33 9 16.5 9z"/>
                    </svg>
                    <span>再抽一条</span>
                </button>
            </div>
        </section>

        <div class="treehole-modal" id="treeHoleModal" role="dialog" aria-modal="true" aria-labelledby="treeHoleModalTitle" aria-hidden="true">
            <div class="treehole-modal__backdrop" id="treeHoleModalBackdrop"></div>
            <div class="treehole-modal__dialog">
                <div class="treehole-modal__header">
                    <h3 class="treehole-modal__title" id="treeHoleModalTitle">
                        <span aria-hidden="true">✏️</span> 写给树洞
                    </h3>
                    <button type="button" class="treehole-modal__close" id="treeHoleCloseModalBtn" aria-label="关闭弹窗">&times;</button>
                </div>
                <div class="treehole-modal__body">
                    <div class="treehole-textarea-wrap">
                        <textarea id="treeHoleInput" maxlength="5000" rows="6"
                                  placeholder="写下你想说的话，这里无人知晓你的名字……"></textarea>
                        <span class="treehole-char-count" id="treeHoleCharCount">0 / 5000</span>
                    </div>
                    <p class="treehole-modal__note">
                        <svg class="treehole-icon" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                        </svg>
                        <span>完全匿名展示 · 提交后经博主审核将随缘漂流给有缘人</span>
                    </p>
                    <div class="treehole-modal__error" id="treeHoleFormError" role="alert"></div>
                </div>
                <div class="treehole-modal__footer">
                    <button type="button" class="treehole-modal-btn treehole-modal-btn--cancel" id="treeHoleCancelBtn">取消</button>
                    <button type="button" class="treehole-modal-btn treehole-modal-btn--submit" id="treeHoleSubmitBtn">投入树洞</button>
                </div>
            </div>
        </div>

        <div class="treehole-toast" id="treeHoleToast" role="status" aria-live="polite"></div>
    </main>

    <#if pjax><!---- pjax {#pjax} end ----></#if>
</div>
<#include "footer.ftl">
</body>
</html>
