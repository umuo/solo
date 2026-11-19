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

  function initEnhancements () {
    enhanceCodeBlocks()
    bindCodeObserver()
    bindBottomObserver()
    bindTocBounds()
    bindMobileNav()
    bindTagFilter()
    ensureBackToTop()
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initEnhancements)
  } else {
    initEnhancements()
  }

  if (typeof window.$ !== 'undefined') {
    $(function () {
      initEnhancements()
      $('#pjax').on('pjax.end', function () {
        initEnhancements()
      })
    })
  }
})()
