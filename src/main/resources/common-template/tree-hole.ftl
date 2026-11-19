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
<#include "macro-common_head.ftl">
<!DOCTYPE html>
<html>
<head>
    <@head title="树洞 - ${blogTitle}">
        <style>
            * { box-sizing: border-box; }
            body {
                margin: 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
                background: #f4f7f9;
                color: #1e293b;
                min-height: 100vh;
                display: flex;
                flex-direction: column;
            }
            .th-fallback-hero {
                background: linear-gradient(135deg, #1b2838 0%, #243b55 55%, #16222a 100%);
                color: #fff;
                text-align: center;
                padding: 36px 16px 104px;
            }
            .th-fallback-nav {
                max-width: 960px;
                margin: 0 auto 28px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: 14px;
            }
            .th-fallback-nav a { color: rgba(255,255,255,0.88); text-decoration: none; margin-right: 16px; }
            .th-fallback-nav a:hover { color: #fff; }
            .th-fallback-hero h1 { margin: 0 0 8px; font-size: 32px; letter-spacing: 2px; }
            .th-fallback-hero p { margin: 0 0 18px; color: rgba(255,255,255,0.85); font-size: 14px; }
            .th-fallback-cta {
                background: linear-gradient(135deg, #29b6f6, #0288d1);
                color: #fff; border: none; border-radius: 999px; padding: 10px 24px;
                font-size: 14px; cursor: pointer;
            }
            .th-fallback-main {
                width: 100%; max-width: 740px; margin: -68px auto 48px; padding: 0 16px; flex: 1;
            }
            .th-fallback-card {
                background: #fff; border-radius: 16px; padding: 28px 32px;
                box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
            }
            .th-fallback-meta {
                display: flex; justify-content: space-between; align-items: center;
                margin-bottom: 20px; font-size: 13px; color: #64748b;
            }
            .th-fallback-no {
                background: #f1f5f9; color: #475569; padding: 4px 12px; border-radius: 999px; font-weight: 600;
            }
            .th-fallback-body {
                min-height: 150px; font-size: 16px; line-height: 1.9; color: #2c353d;
            }
            .th-fallback-body p { margin: 0 0 12px; }
            .th-fallback-footer {
                display: flex; justify-content: space-between; align-items: center;
                border-top: 1px solid #f1f5f9; margin-top: 22px; padding-top: 18px;
            }
            .th-fallback-btn {
                border-radius: 999px; padding: 8px 18px; font-size: 14px; cursor: pointer; border: 1px solid #f4c2c2; background: #fff; color: #e25555;
            }
            .th-fallback-btn.is-liked { background: #fff1f2; border-color: #fb7185; font-weight: 600; }
            .th-fallback-next {
                border: none; background: linear-gradient(135deg, #3b9eff, #2575fc); color: #fff; border-radius: 999px; padding: 8px 20px; font-size: 14px; cursor: pointer;
            }
            .treehole-modal { position: fixed; inset: 0; display: none; align-items: center; justify-content: center; background: rgba(15,23,42,0.5); z-index: 99; padding: 16px; }
            .treehole-modal.is-open { display: flex; }
            .treehole-modal__dialog { width: 100%; max-width: 480px; background: #fff; border-radius: 14px; padding: 20px; box-shadow: 0 20px 50px rgba(0,0,0,0.2); }
            .treehole-modal__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
            .treehole-modal__title { margin: 0; font-size: 17px; }
            .treehole-modal__close { border: none; background: transparent; font-size: 22px; cursor: pointer; color: #64748b; }
            .treehole-textarea-wrap { position: relative; }
            .treehole-textarea-wrap textarea { width: 100%; min-height: 136px; border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px 12px 28px; font-size: 14px; resize: vertical; }
            .treehole-char-count { position: absolute; right: 12px; bottom: 8px; font-size: 12px; color: #94a3b8; }
            .treehole-modal__note { font-size: 12px; color: #64748b; margin: 10px 0 8px; }
            .treehole-modal__error { color: #dc2626; font-size: 13px; min-height: 18px; }
            .treehole-modal__footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px; }
            .treehole-modal-btn { border-radius: 8px; padding: 8px 18px; font-size: 14px; cursor: pointer; }
            .treehole-modal-btn--cancel { background: #fff; border: 1px solid #cbd5e1; color: #475569; }
            .treehole-modal-btn--submit { background: #159e96; border: none; color: #fff; }
            .treehole-toast { position: fixed; left: 50%; bottom: 32px; transform: translateX(-50%); background: rgba(15,23,42,0.9); color: #fff; padding: 10px 18px; border-radius: 999px; font-size: 13px; display: none; z-index: 120; }
            .treehole-toast.is-show { display: block; }
        </style>
    </@head>
</head>
<body>
<div class="th-fallback-hero">
    <nav class="th-fallback-nav">
        <div>
            <a href="${servePath}">${blogTitle}</a>
            <#list pageNavigations as page>
                <a href="${page.pagePermalink}" target="${page.pageOpenTarget}">${page.pageTitle}</a>
            </#list>
        </div>
    </nav>
    <h1>树洞</h1>
    <p>把秘密留在这里，或偶遇一段陌生人的心事</p>
    <button type="button" class="th-fallback-cta" id="treeHoleOpenModalBtn">+ 投递树洞</button>
</div>

<main id="treeHoleApp" class="th-fallback-main" data-serve-path="${servePath}">
    <section class="th-fallback-card">
        <div class="th-fallback-meta">
            <span class="th-fallback-no" id="treeHoleNo">NO. ------</span>
            <span id="treeHoleWatched">已被 <strong id="treeHoleWatchedNum">0</strong> 人遇见</span>
        </div>
        <div class="th-fallback-body" id="treeHoleContent">
            <p>正在翻阅树洞里的心事...</p>
        </div>
        <div class="th-fallback-footer">
            <button type="button" class="th-fallback-btn" id="treeHoleLikeBtn">♥ 共鸣 (<span id="treeHoleLikeCount">0</span>)</button>
            <button type="button" class="th-fallback-next" id="treeHoleNextBtn">🎲 再抽一条</button>
        </div>
    </section>

    <div class="treehole-modal" id="treeHoleModal" role="dialog" aria-modal="true" aria-hidden="true">
        <div class="treehole-modal__dialog">
            <div class="treehole-modal__header">
                <h3 class="treehole-modal__title">✏️ 写给树洞</h3>
                <button type="button" class="treehole-modal__close" id="treeHoleCloseModalBtn">&times;</button>
            </div>
            <div class="treehole-textarea-wrap">
                <textarea id="treeHoleInput" maxlength="5000" rows="6" placeholder="写下你想说的话，这里无人知晓你的名字……"></textarea>
                <span class="treehole-char-count" id="treeHoleCharCount">0 / 5000</span>
            </div>
            <p class="treehole-modal__note">ⓘ 完全匿名展示 · 提交后经博主审核将随缘漂流给有缘人</p>
            <div class="treehole-modal__error" id="treeHoleFormError"></div>
            <div class="treehole-modal__footer">
                <button type="button" class="treehole-modal-btn treehole-modal-btn--cancel" id="treeHoleCancelBtn">取消</button>
                <button type="button" class="treehole-modal-btn treehole-modal-btn--submit" id="treeHoleSubmitBtn">投入树洞</button>
            </div>
        </div>
    </div>
    <div class="treehole-toast" id="treeHoleToast"></div>
</main>

<script>
(function () {
  var app = document.getElementById('treeHoleApp')
  if (!app) return
  var servePath = (app.getAttribute('data-serve-path') || '').replace(/\/$/, '')
  var currentId = ''
  var noEl = document.getElementById('treeHoleNo')
  var watchedNumEl = document.getElementById('treeHoleWatchedNum')
  var bodyEl = document.getElementById('treeHoleContent')
  var likeBtn = document.getElementById('treeHoleLikeBtn')
  var likeCountEl = document.getElementById('treeHoleLikeCount')
  var nextBtn = document.getElementById('treeHoleNextBtn')
  var modal = document.getElementById('treeHoleModal')
  var openBtn = document.getElementById('treeHoleOpenModalBtn')
  var closeBtn = document.getElementById('treeHoleCloseModalBtn')
  var cancelBtn = document.getElementById('treeHoleCancelBtn')
  var submitBtn = document.getElementById('treeHoleSubmitBtn')
  var inputEl = document.getElementById('treeHoleInput')
  var countEl = document.getElementById('treeHoleCharCount')
  var errEl = document.getElementById('treeHoleFormError')
  var toastEl = document.getElementById('treeHoleToast')

  function decodeOnce (str) {
    return String(str || '').replace(/&(lt|gt|quot|#39|amp);/g, function (m, k) {
      return k === 'lt' ? '<' : k === 'gt' ? '>' : k === 'quot' ? '"' : k === '#39' ? "'" : '&'
    })
  }
  function renderBody (text) {
    var parts = decodeOnce(text).replace(/\r\n/g, '\n').split(/\n{2,}/)
    bodyEl.innerHTML = ''
    for (var i = 0; i < parts.length; i++) {
      var p = document.createElement('p')
      var lines = parts[i].split('\n')
      for (var j = 0; j < lines.length; j++) {
        if (j > 0) p.appendChild(document.createElement('br'))
        p.appendChild(document.createTextNode(lines[j]))
      }
      bodyEl.appendChild(p)
    }
  }
  function showToast (msg) {
    toastEl.textContent = msg
    toastEl.classList.add('is-show')
    setTimeout(function () { toastEl.classList.remove('is-show') }, 2600)
  }
  function loadRandom () {
    fetch(servePath + '/hole/message/random?_t=' + Date.now(), { credentials: 'same-origin' })
      .then(function (r) { return r.json() })
      .then(function (data) {
        var msg = data && (data.randomMessage || data.randomTreeHole)
        if (!msg || !msg.oId) {
          currentId = ''
          noEl.textContent = 'NO. ------'
          watchedNumEl.textContent = '0'
          likeCountEl.textContent = '0'
          bodyEl.innerHTML = '<p>树洞里暂时还是空空的，点击上方写下第一份心事吧。</p>'
          likeBtn.disabled = true
          return
        }
        currentId = String(msg.oId)
        noEl.textContent = 'NO. ' + (currentId.length > 6 ? currentId.slice(-6) : currentId)
        watchedNumEl.textContent = String(msg.watchedCount || 0)
        likeCountEl.textContent = String(msg.likeCount || 0)
        likeBtn.disabled = false
        renderBody(msg.content || '')
      })
  }
  nextBtn.addEventListener('click', loadRandom)
  likeBtn.addEventListener('click', function () {
    if (!currentId) return
    fetch(servePath + '/hole/message/addLike/' + encodeURIComponent(currentId), { credentials: 'same-origin' })
      .then(function (r) { return r.json() })
      .then(function (res) {
        if (res && res.code === 0) {
          likeCountEl.textContent = String(parseInt(likeCountEl.textContent || '0', 10) + 1)
          likeBtn.classList.add('is-liked')
        } else {
          showToast((res && res.msg) || '已经点过共鸣了')
        }
      })
  })
  openBtn.addEventListener('click', function () {
    errEl.textContent = ''
    countEl.textContent = (inputEl.value || '').length + ' / 5000'
    modal.classList.add('is-open')
    modal.setAttribute('aria-hidden', 'false')
  })
  function closeModal () {
    modal.classList.remove('is-open')
    modal.setAttribute('aria-hidden', 'true')
  }
  closeBtn.addEventListener('click', closeModal)
  cancelBtn.addEventListener('click', closeModal)
  inputEl.addEventListener('input', function () {
    countEl.textContent = inputEl.value.length + ' / 5000'
  })
  submitBtn.addEventListener('click', function () {
    var val = (inputEl.value || '').trim()
    if (!val) {
      errEl.textContent = '写点什么再投入树洞吧 ~'
      return
    }
    fetch(servePath + '/hole/message/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ hole: { content: val } })
    }).then(function (r) { return r.json() }).then(function (res) {
      if (res && res.code === 0) {
        inputEl.value = ''
        countEl.textContent = '0 / 5000'
        closeModal()
        showToast('已投入树洞，等待博主审核通过后漂流给有缘人')
      } else {
        errEl.textContent = (res && res.msg) || '投递失败，请稍后重试'
      }
    })
  })
  loadRandom()
})()
</script>
</body>
</html>
