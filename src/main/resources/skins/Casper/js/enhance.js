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
/**
 * Casper skin UI/UX enhancement script.
 *
 * @version 1.0.0.0, Nov 19, 2026
 */
(function () {
  var codeObserver = null
  var bottomObserver = null
  var tocBound = false

  function extractLanguage (codeEl) {
    if (!codeEl || !codeEl.className) {
      return 'TEXT'
    }
    var match = codeEl.className.match(/language-([a-zA-Z0-9_+#-]+)/)
    if (match && match[1]) {
      return match[1].toUpperCase()
    }
    return 'TEXT'
  }

  function extractPlainCode (codeEl) {
    var clone = codeEl.cloneNode(true)
    var noise = clone.querySelectorAll('.vditor-linenumber__rows, .vditor-copy')
    for (var i = 0; i < noise.length; i++) {
      if (noise[i].parentNode) {
        noise[i].parentNode.removeChild(noise[i])
      }
    }
    return (clone.textContent || '').replace(/\n$/, '')
  }

  function copyText (text, onSuccess, onFail) {
    var fallback = function () {
      var ta = document.createElement('textarea')
      ta.value = text
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.top = '-9999px'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      try {
        var ok = document.execCommand('copy')
        document.body.removeChild(ta)
        if (ok) {
          onSuccess()
        } else if (onFail) {
          onFail()
        }
      } catch (e) {
        document.body.removeChild(ta)
        if (onFail) {
          onFail()
        }
      }
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(onSuccess, fallback)
    } else {
      fallback()
    }
  }

  function enhanceCodeBlocks () {
    var pres = document.querySelectorAll('.vditor-reset pre')
    for (var i = 0; i < pres.length; i++) {
      var pre = pres[i]
      if (pre.getAttribute('data-code-enhanced') === '1') {
        continue
      }
      var codeEl = pre.querySelector('code')
      if (!codeEl) {
        continue
      }

      pre.setAttribute('data-code-enhanced', '1')
      var header = document.createElement('div')
      header.className = 'solo-code-header'

      var langSpan = document.createElement('span')
      langSpan.className = 'solo-code-lang'
      langSpan.textContent = extractLanguage(codeEl)

      var copyBtn = document.createElement('button')
      copyBtn.type = 'button'
      copyBtn.className = 'solo-code-copy'
      copyBtn.textContent = '复制'

      ;(function (targetCode, btn) {
        btn.addEventListener('click', function () {
          var raw = extractPlainCode(targetCode)
          copyText(raw, function () {
            btn.textContent = '已复制'
            btn.classList.add('solo-code-copy--copied')
            setTimeout(function () {
              btn.textContent = '复制'
              btn.classList.remove('solo-code-copy--copied')
            }, 1800)
          })
        })
      })(codeEl, copyBtn)

      header.appendChild(langSpan)
      header.appendChild(copyBtn)
      pre.insertBefore(header, pre.firstChild)
    }
  }

  function bindCodeObserver () {
    if (codeObserver) {
      codeObserver.disconnect()
      codeObserver = null
    }
    var container = document.querySelector('.vditor-reset.item__content--article')
    if (!container || typeof MutationObserver === 'undefined') {
      return
    }
    codeObserver = new MutationObserver(function () {
      enhanceCodeBlocks()
    })
    codeObserver.observe(container, { childList: true, subtree: true })
  }

  function refreshBottomEmptyState () {
    var bottom = document.querySelector('.article__bottom')
    if (!bottom) {
      return
    }
    var items = bottom.querySelectorAll('.item')
    if (!items.length) {
      bottom.classList.add('article__bottom--empty')
      return
    }
    var hasContent = false
    for (var i = 0; i < items.length; i++) {
      if (items[i].innerHTML.replace(/\s+/g, '') !== '') {
        hasContent = true
        break
      }
    }
    if (hasContent) {
      bottom.classList.remove('article__bottom--empty')
    } else {
      bottom.classList.add('article__bottom--empty')
    }
  }

  function bindBottomObserver () {
    if (bottomObserver) {
      bottomObserver.disconnect()
      bottomObserver = null
    }
    refreshBottomEmptyState()
    var bottom = document.querySelector('.article__bottom')
    if (!bottom || typeof MutationObserver === 'undefined') {
      return
    }
    bottomObserver = new MutationObserver(function () {
      refreshBottomEmptyState()
    })
    bottomObserver.observe(bottom, { childList: true, subtree: true })
  }

  function adjustTocBounds () {
    var tocWrap = document.querySelector('.post__toc')
    if (!tocWrap || window.innerWidth <= 876) {
      return
    }
    var hasItems = tocWrap.querySelectorAll('.article__toc li').length > 0
    if (!hasItems) {
      tocWrap.style.display = 'none'
      return
    }

    var contentEl = document.querySelector('.article .item__content')
    if (contentEl) {
      var rect = contentEl.getBoundingClientRect()
      var leftPos = Math.round(rect.left + rect.width - 80)
      var availableRight = window.innerWidth - leftPos - 16
      if (availableRight < 180) {
        tocWrap.style.display = 'none'
        return
      }
      tocWrap.style.left = leftPos + 'px'
      tocWrap.style.maxWidth = Math.min(240, availableRight) + 'px'
    }

    var articleEl = document.querySelector('.article')
    var footerEl = document.querySelector('.footer')
    var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0
    if (articleEl && footerEl) {
      var footerRect = footerEl.getBoundingClientRect()
      var articleBottom = articleEl.offsetTop + articleEl.offsetHeight
      var tocVisibleHeight = Math.min(tocWrap.scrollHeight || 300, window.innerHeight - 180)
      var wouldOverlapFooter = footerRect.top <= (84 + tocVisibleHeight + 16)
      if (scrollTop > 975 && scrollTop < articleBottom + 100 && !wouldOverlapFooter) {
        tocWrap.style.display = 'block'
      } else {
        tocWrap.style.display = 'none'
      }
    }
  }

  function scheduleTocAdjust () {
    if (window.requestAnimationFrame) {
      window.requestAnimationFrame(adjustTocBounds)
    } else {
      setTimeout(adjustTocBounds, 16)
    }
  }

  function bindTocBounds () {
    scheduleTocAdjust()
    if (tocBound) {
      return
    }
    tocBound = true
    window.addEventListener('scroll', scheduleTocAdjust, { passive: true })
    window.addEventListener('resize', scheduleTocAdjust, { passive: true })
  }

  function bindMobileNav () {
    if (document.body.getAttribute('data-nav-bound') === '1') {
      return
    }
    document.body.setAttribute('data-nav-bound', '1')

    document.addEventListener('click', function (e) {
      var toggle = e.target.closest ? e.target.closest('.header__nav-toggle') : null
      var nav = document.querySelector('.header__nav')
      if (!nav) {
        return
      }
      if (toggle) {
        var opened = nav.classList.toggle('header__nav--open')
        toggle.setAttribute('aria-expanded', opened ? 'true' : 'false')
        return
      }
      if (nav.classList.contains('header__nav--open')) {
        if (!e.target.closest('.header__nav') || e.target.closest('.header__nav-links a')) {
          nav.classList.remove('header__nav--open')
          var btn = nav.querySelector('.header__nav-toggle')
          if (btn) {
            btn.setAttribute('aria-expanded', 'false')
          }
        }
      }
    })

    window.addEventListener('resize', function () {
      if (window.innerWidth > 768) {
        var nav = document.querySelector('.header__nav')
        if (nav && nav.classList.contains('header__nav--open')) {
          nav.classList.remove('header__nav--open')
          var btn = nav.querySelector('.header__nav-toggle')
          if (btn) {
            btn.setAttribute('aria-expanded', 'false')
          }
        }
      }
    }, { passive: true })
  }

  function bindTagFilter () {
    var input = document.getElementById('tagFilterInput')
    if (!input || input.getAttribute('data-filter-bound') === '1') {
      return
    }
    input.setAttribute('data-filter-bound', '1')
    var countEl = document.getElementById('tagFilterCount')
    var items = document.querySelectorAll('.other__content--tags .other__item')

    function updateFilter () {
      var kw = (input.value || '').trim().toLowerCase()
      var visible = 0
      for (var i = 0; i < items.length; i++) {
        var text = (items[i].textContent || '').toLowerCase()
        var show = !kw || text.indexOf(kw) !== -1
        items[i].style.display = show ? 'inline-flex' : 'none'
        if (show) {
          visible++
        }
      }
      if (countEl) {
        countEl.textContent = kw ? ('匹配 ' + visible + ' / ' + items.length + ' 个标签') : ''
      }
    }

    input.addEventListener('input', updateFilter)
    updateFilter()
  }

  function ensureBackToTop () {
    var btn = document.querySelector('.solo-back-top')
    if (!btn) {
      btn = document.createElement('button')
      btn.type = 'button'
      btn.className = 'solo-back-top'
      btn.setAttribute('aria-label', '回到顶部')
      btn.title = '回到顶部'
      btn.innerHTML = '&#8593;'
      btn.addEventListener('click', function () {
        if (window.scrollTo) {
          try {
            window.scrollTo({ top: 0, behavior: 'smooth' })
          } catch (e) {
            window.scrollTo(0, 0)
          }
        }
      })
      document.body.appendChild(btn)

      window.addEventListener('scroll', function () {
        var top = window.pageYOffset || document.documentElement.scrollTop || 0
        if (top > 400) {
          btn.classList.add('solo-back-top--visible')
        } else {
          btn.classList.remove('solo-back-top--visible')
        }
      }, { passive: true })
    }
  }

  function decodeTreeHoleEntitiesOnce (str) {
    if (!str) {
      return ''
    }
    return String(str).replace(/&(lt|gt|quot|#39|amp);/g, function (match, entity) {
      if (entity === 'lt') {
        return '<'
      }
      if (entity === 'gt') {
        return '>'
      }
      if (entity === 'quot') {
        return '"'
      }
      if (entity === '#39') {
        return '\''
      }
      if (entity === 'amp') {
        return '&'
      }
      return match
    })
  }

  function getTodayLikeStorageKey () {
    var d = new Date()
    var y = d.getFullYear()
    var m = d.getMonth() + 1
    var day = d.getDate()
    return 'solo_th_liked_' + y + '-' + (m < 10 ? '0' + m : m) + '-' + (day < 10 ? '0' + day : day)
  }

  function getLikedSetForToday () {
    var result = {}
    if (!window.localStorage) {
      return result
    }
    try {
      var raw = window.localStorage.getItem(getTodayLikeStorageKey())
      if (raw) {
        var arr = JSON.parse(raw)
        if (Array.isArray(arr)) {
          for (var i = 0; i < arr.length; i++) {
            result[String(arr[i])] = true
          }
        }
      }
    } catch (e) {
      // Ignore storage parse error
    }
    return result
  }

  function markLikedForToday (oId) {
    if (!window.localStorage || !oId) {
      return
    }
    try {
      var key = getTodayLikeStorageKey()
      var raw = window.localStorage.getItem(key)
      var arr = raw ? JSON.parse(raw) : []
      if (!Array.isArray(arr)) {
        arr = []
      }
      var idStr = String(oId)
      if (arr.indexOf(idStr) === -1) {
        arr.push(idStr)
        window.localStorage.setItem(key, JSON.stringify(arr))
      }
    } catch (e) {
      // Ignore storage write error
    }
  }

  function bindTreeHoleEsc () {
    if (document.body.getAttribute('data-th-esc-bound') === '1') {
      return
    }
    document.body.setAttribute('data-th-esc-bound', '1')
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' || e.keyCode === 27) {
        var modal = document.getElementById('treeHoleModal')
        if (modal && modal.classList.contains('is-open')) {
          modal.classList.remove('is-open')
          modal.setAttribute('aria-hidden', 'true')
        }
      }
    })
  }

  function ensureTreeHoleFonts () {
    var head = document.head || document.getElementsByTagName('head')[0]
    if (!head) {
      return
    }
    var fonts = [
      { id: 'soloTreeHoleFontEn', href: 'https://cdn.jsdelivr.net/npm/@fontsource/caveat@5.0.8/500.css' },
      { id: 'soloTreeHoleFontZh', href: 'https://cdn.jsdelivr.net/npm/lxgw-wenkai-webfont@1.7.0/lxgwwenkai-regular.css' }
    ]
    for (var i = 0; i < fonts.length; i++) {
      if (!document.getElementById(fonts[i].id)) {
        var link = document.createElement('link')
        link.id = fonts[i].id
        link.rel = 'stylesheet'
        link.href = fonts[i].href
        head.appendChild(link)
      }
    }
  }

  var TREE_HOLE_THEMES = ['sky', 'sakura', 'mint', 'amber', 'lavender', 'peach', 'sage', 'aqua']

  function applyRandomCardTheme (cardEl) {
    if (!cardEl) {
      return
    }
    var currentTheme = cardEl.getAttribute('data-card-theme') || ''
    var candidates = []
    for (var i = 0; i < TREE_HOLE_THEMES.length; i++) {
      if (TREE_HOLE_THEMES[i] !== currentTheme) {
        candidates.push(TREE_HOLE_THEMES[i])
      }
    }
    if (!candidates.length) {
      candidates = TREE_HOLE_THEMES
    }
    var picked = candidates[Math.floor(Math.random() * candidates.length)]
    cardEl.setAttribute('data-card-theme', picked)
  }

  function formatTreeHoleTime (msg) {
    if (!msg) {
      return ''
    }
    if (msg.createTimeStr) {
      return String(msg.createTimeStr)
    }
    if (!msg.createTime) {
      return ''
    }
    var ts = parseInt(String(msg.createTime), 10)
    if (isNaN(ts) || ts <= 0) {
      return ''
    }
    var d = new Date(ts)
    if (isNaN(d.getTime())) {
      return ''
    }
    var pad = function (n) { return n < 10 ? '0' + n : String(n) }
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
      ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
  }

  function initTreeHolePage () {
    var app = document.getElementById('treeHoleApp')
    if (!app || app.getAttribute('data-th-bound') === '1') {
      return
    }
    app.setAttribute('data-th-bound', '1')
    ensureTreeHoleFonts()
    bindTreeHoleEsc()

    var servePath = (app.getAttribute('data-serve-path') || (window.Label && window.Label.servePath) || '').replace(/\/$/, '')
    var cardEl = document.getElementById('treeHoleCard') || app.querySelector('.treehole-card')
    var noEl = document.getElementById('treeHoleNo')
    var watchedNumEl = document.getElementById('treeHoleWatchedNum')
    var bodyEl = document.getElementById('treeHoleContent')
    var timeWrapEl = document.getElementById('treeHoleTimeWrap')
    var timeEl = document.getElementById('treeHoleTime')
    var likeBtn = document.getElementById('treeHoleLikeBtn')
    var likeCountEl = document.getElementById('treeHoleLikeCount')
    var nextBtn = document.getElementById('treeHoleNextBtn')
    var modal = document.getElementById('treeHoleModal')
    var backdrop = document.getElementById('treeHoleModalBackdrop')
    var openModalBtn = document.getElementById('treeHoleOpenModalBtn')
    var closeModalBtn = document.getElementById('treeHoleCloseModalBtn')
    var cancelBtn = document.getElementById('treeHoleCancelBtn')
    var submitBtn = document.getElementById('treeHoleSubmitBtn')
    var inputEl = document.getElementById('treeHoleInput')
    var charCountEl = document.getElementById('treeHoleCharCount')
    var formErrorEl = document.getElementById('treeHoleFormError')
    var toastEl = document.getElementById('treeHoleToast')

    var currentId = ''
    var isDrawing = false
    var isLiking = false
    var isSubmitting = false
    var hasLoadedFirstMessage = false
    var toastTimer = null

    applyRandomCardTheme(cardEl)

    function showToast (text) {
      if (!toastEl) {
        return
      }
      toastEl.textContent = text
      toastEl.classList.add('is-show')
      if (toastTimer) {
        clearTimeout(toastTimer)
      }
      toastTimer = setTimeout(function () {
        toastEl.classList.remove('is-show')
      }, 2800)
    }

    function renderLetterBody (rawContent) {
      if (!bodyEl) {
        return
      }
      var decoded = decodeTreeHoleEntitiesOnce(rawContent)
      var normalized = decoded.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      var paragraphs = normalized.split(/\n{2,}/)
      bodyEl.scrollTop = 0
      bodyEl.innerHTML = ''
      var letterWrap = document.createElement('div')
      letterWrap.className = 'treehole-card__letter'
      for (var i = 0; i < paragraphs.length; i++) {
        var pEl = document.createElement('p')
        var lines = paragraphs[i].split('\n')
        for (var j = 0; j < lines.length; j++) {
          if (j > 0) {
            pEl.appendChild(document.createElement('br'))
          }
          pEl.appendChild(document.createTextNode(lines[j]))
        }
        letterWrap.appendChild(pEl)
      }
      bodyEl.appendChild(letterWrap)
    }

    function applyMessage (msg) {
      if (!msg || !msg.oId) {
        currentId = ''
        hasLoadedFirstMessage = true
        if (noEl) {
          noEl.textContent = 'NO. ------'
        }
        if (watchedNumEl) {
          watchedNumEl.textContent = '0'
        }
        if (likeCountEl) {
          likeCountEl.textContent = '0'
        }
        if (timeWrapEl) {
          timeWrapEl.classList.add('is-empty')
        }
        if (timeEl) {
          timeEl.textContent = '--'
        }
        if (likeBtn) {
          likeBtn.classList.remove('is-liked')
          likeBtn.disabled = true
        }
        if (bodyEl) {
          bodyEl.innerHTML = '<p class="treehole-card__empty">树洞里暂时还是空的，点击上方「+ 投递树洞」留下第一份心事吧。</p>'
        }
        return
      }

      if (hasLoadedFirstMessage) {
        applyRandomCardTheme(cardEl)
      }
      hasLoadedFirstMessage = true

      currentId = String(msg.oId)
      if (noEl) {
        noEl.textContent = 'NO. ' + (currentId.length > 6 ? currentId.slice(-6) : currentId)
      }
      if (watchedNumEl) {
        watchedNumEl.textContent = String(msg.watchedCount || 0)
      }
      if (likeCountEl) {
        likeCountEl.textContent = String(msg.likeCount || 0)
      }
      var timeStr = formatTreeHoleTime(msg)
      if (timeEl && timeWrapEl) {
        if (timeStr) {
          timeEl.textContent = '留于 ' + timeStr
          timeEl.setAttribute('datetime', timeStr)
          timeWrapEl.classList.remove('is-empty')
        } else {
          timeEl.textContent = '--'
          timeEl.removeAttribute('datetime')
          timeWrapEl.classList.add('is-empty')
        }
      }
      if (likeBtn) {
        likeBtn.disabled = false
        var likedMap = getLikedSetForToday()
        if (likedMap[currentId]) {
          likeBtn.classList.add('is-liked')
        } else {
          likeBtn.classList.remove('is-liked')
        }
      }
      renderLetterBody(msg.content || '')
    }

    function fetchRandomMessage (avoidDuplicate, retryDone) {
      if (isDrawing && !retryDone) {
        return
      }
      isDrawing = true
      if (nextBtn) {
        nextBtn.disabled = true
      }
      if (bodyEl && !retryDone) {
        bodyEl.classList.add('is-switching')
      }

      var previousId = currentId
      fetch(servePath + '/hole/message/random?_t=' + Date.now(), {
        method: 'GET',
        credentials: 'same-origin'
      }).then(function (resp) {
        return resp.json()
      }).then(function (data) {
        var msg = data && (data.randomMessage || data.randomTreeHole) ? (data.randomMessage || data.randomTreeHole) : null
        if (avoidDuplicate && !retryDone && msg && msg.oId && String(msg.oId) === previousId) {
          fetchRandomMessage(false, true)
          return
        }
        applyMessage(msg)
        if (bodyEl) {
          bodyEl.classList.remove('is-switching')
        }
        isDrawing = false
        if (nextBtn) {
          nextBtn.disabled = false
        }
      }).catch(function () {
        if (bodyEl) {
          bodyEl.classList.remove('is-switching')
          if (!currentId) {
            bodyEl.innerHTML = '<p class="treehole-card__empty">暂时未能连接到树洞，请稍后再试 ~</p>'
          }
        }
        isDrawing = false
        if (nextBtn) {
          nextBtn.disabled = false
        }
      })
    }

    function openModal () {
      if (!modal) {
        return
      }
      if (formErrorEl) {
        formErrorEl.textContent = ''
      }
      if (charCountEl && inputEl) {
        charCountEl.textContent = inputEl.value.length + ' / 5000'
      }
      modal.classList.add('is-open')
      modal.setAttribute('aria-hidden', 'false')
      if (inputEl) {
        setTimeout(function () {
          inputEl.focus()
        }, 50)
      }
    }

    function closeModal () {
      if (!modal) {
        return
      }
      modal.classList.remove('is-open')
      modal.setAttribute('aria-hidden', 'true')
    }

    if (nextBtn) {
      nextBtn.addEventListener('click', function () {
        fetchRandomMessage(true, false)
      })
    }

    if (likeBtn) {
      likeBtn.addEventListener('click', function () {
        if (!currentId || isLiking) {
          return
        }
        var targetId = currentId
        isLiking = true
        fetch(servePath + '/hole/message/addLike/' + encodeURIComponent(targetId), {
          method: 'GET',
          credentials: 'same-origin'
        }).then(function (resp) {
          return resp.json()
        }).then(function (res) {
          if (res && res.code === 0) {
            if (currentId === targetId && likeCountEl) {
              var prev = parseInt(likeCountEl.textContent || '0', 10)
              likeCountEl.textContent = String((isNaN(prev) ? 0 : prev) + 1)
            }
            likeBtn.classList.add('is-liked')
            markLikedForToday(targetId)
          } else {
            var msg = (res && res.msg) || ''
            if (msg.indexOf('已经') !== -1) {
              likeBtn.classList.add('is-liked')
              markLikedForToday(targetId)
              showToast('今天已经为这条心事点过共鸣啦')
            } else {
              showToast(msg || '共鸣失败，请稍后重试')
            }
          }
        }).catch(function () {
          showToast('网络开小差了，请稍后重试')
        }).finally(function () {
          isLiking = false
        })
      })
    }

    if (openModalBtn) {
      openModalBtn.addEventListener('click', openModal)
    }
    if (closeModalBtn) {
      closeModalBtn.addEventListener('click', closeModal)
    }
    if (cancelBtn) {
      cancelBtn.addEventListener('click', closeModal)
    }
    if (backdrop) {
      backdrop.addEventListener('click', closeModal)
    }
    if (inputEl && charCountEl) {
      inputEl.addEventListener('input', function () {
        charCountEl.textContent = inputEl.value.length + ' / 5000'
        if (formErrorEl && formErrorEl.textContent) {
          formErrorEl.textContent = ''
        }
      })
    }
    if (submitBtn) {
      submitBtn.addEventListener('click', function () {
        if (isSubmitting || !inputEl) {
          return
        }
        var content = inputEl.value.trim()
        if (!content) {
          if (formErrorEl) {
            formErrorEl.textContent = '写点什么再投入树洞吧 ~'
          }
          inputEl.focus()
          return
        }
        if (content.length > 5000) {
          if (formErrorEl) {
            formErrorEl.textContent = '内容最多不能超过 5000 字哦'
          }
          return
        }

        isSubmitting = true
        submitBtn.disabled = true
        submitBtn.textContent = '投递中...'
        if (formErrorEl) {
          formErrorEl.textContent = ''
        }

        fetch(servePath + '/hole/message/add', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'same-origin',
          body: JSON.stringify({ hole: { content: content } })
        }).then(function (resp) {
          return resp.json()
        }).then(function (res) {
          if (res && res.code === 0) {
            inputEl.value = ''
            if (charCountEl) {
              charCountEl.textContent = '0 / 5000'
            }
            closeModal()
            showToast('✨ 已投入树洞，经博主审核后将随缘漂流给有缘人')
          } else {
            var errMsg = (res && res.msg) || '投递失败，请稍后重试'
            if (errMsg === 'not allowed') {
              errMsg = '今日投递次数已达上限，明天再来吧 ~'
            }
            if (formErrorEl) {
              formErrorEl.textContent = errMsg
            }
          }
        }).catch(function () {
          if (formErrorEl) {
            formErrorEl.textContent = '网络请求失败，请检查网络后重试'
          }
        }).finally(function () {
          isSubmitting = false
          submitBtn.disabled = false
          submitBtn.textContent = '投入树洞'
        })
      })
    }

    fetchRandomMessage(false, false)
  }

  var initialJq = typeof window.$ !== 'undefined' ? window.$ : null
  var pjaxDomObserver = null

  function bindPjaxObserver () {
    var pjaxEl = document.getElementById('pjax')
    if (!pjaxEl || pjaxEl.getAttribute('data-pjax-observed') === '1') {
      return
    }
    pjaxEl.setAttribute('data-pjax-observed', '1')
    if (typeof MutationObserver !== 'undefined') {
      pjaxDomObserver = new MutationObserver(function () {
        initEnhancements()
      })
      pjaxDomObserver.observe(pjaxEl, { childList: true, subtree: false })
    }
  }

  function initEnhancements () {
    bindPjaxObserver()
    enhanceCodeBlocks()
    bindCodeObserver()
    bindBottomObserver()
    bindTocBounds()
    bindMobileNav()
    bindTagFilter()
    ensureBackToTop()
    initTreeHolePage()
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initEnhancements)
  } else {
    initEnhancements()
  }

  if (initialJq) {
    initialJq('#pjax').on('pjax.end', function () {
      initEnhancements()
    })
  }

  if (typeof window.$ !== 'undefined') {
    window.$(function () {
      initEnhancements()
      window.$('#pjax').on('pjax.end', function () {
        initEnhancements()
      })
    })
  }
})()
