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
 * TreeHole management for admin console.
 *
 * @version 1.0.0.0, Nov 19, 2026
 */
(function () {
  if (typeof window.admin === 'undefined') {
    return
  }

  if (typeof Label !== 'undefined' && Label.userRole !== 'adminRole') {
    $('#navTreeHoleItem').remove()
    return
  }

  var pad = function (n) {
    return n < 10 ? '0' + n : String(n)
  }

  var formatTime = function (raw) {
    var ts = parseInt(raw, 10)
    if (!ts || isNaN(ts) || ts <= 0) {
      return '-'
    }
    var d = new Date(ts)
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
      ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
  }

  var formatLocation = function (rawLoc) {
    if (!rawLoc) {
      return '未知地区'
    }
    var parts = String(rawLoc).split('|')
    var cleaned = []
    for (var i = 0; i < parts.length; i++) {
      var p = $.trim(parts[i])
      if (p && p !== '0') {
        cleaned.push(p)
      }
    }
    return cleaned.length ? cleaned.join('·') : '未知地区'
  }

  var decodeEntitiesOnce = function (str) {
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

  var escapeHtml = function (str) {
    if (str === null || str === undefined) {
      return ''
    }
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
  }

  var searchTimer = null

  admin.treehole = {
    _boundEvents: false,
    _status: 'all',
    _keyword: '',
    _page: 1,
    _pageSize: 15,

    init: function (page) {
      $('#loadMsg').text('')
      var parsed = parseInt(page, 10)
      if (!isNaN(parsed) && parsed > 0) {
        admin.treehole._page = parsed
      }
      admin.treehole.bindEvents()
      admin.treehole.getList()
    },

    refresh: function (page) {
      admin.clearTip()
      var parsed = parseInt(page, 10)
      if (!isNaN(parsed) && parsed > 0) {
        admin.treehole._page = parsed
      }
      admin.treehole.bindEvents()
      admin.treehole.getList()
    },

    loadBadge: function () {
      $.ajax({
        url: Label.servePath + '/console/tree-hole/stats',
        type: 'GET',
        cache: false,
        success: function (result) {
          if (result && result.code === 0 && result.stats) {
            admin.treehole.updateBadgeCount(result.stats.pendingCount || 0)
          }
        },
      })
    },

    updateBadgeCount: function (pendingCount) {
      var count = parseInt(pendingCount, 10) || 0
      var $badge = $('#treeHoleNavBadge')
      if (count > 0) {
        $badge.text(count).removeClass('fn__none')
      } else {
        $badge.text('0').addClass('fn__none')
      }
    },

    bindEvents: function () {
      var $panel = $('#tabsPanel_treehole')
      if (admin.treehole._boundEvents) {
        return
      }
      admin.treehole._boundEvents = true

      $panel.on('click.treehole', '.th-tab', function () {
        var nextStatus = $(this).attr('data-status') || 'all'
        $panel.find('.th-tab').removeClass('is-active')
        $(this).addClass('is-active')
        admin.treehole._status = nextStatus
        admin.treehole._page = 1
        admin.treehole.getList()
      })

      $panel.on('input.treehole', '#thSearchInput', function () {
        var val = $.trim($(this).val())
        if (searchTimer) {
          clearTimeout(searchTimer)
        }
        searchTimer = setTimeout(function () {
          admin.treehole._keyword = val
          admin.treehole._page = 1
          admin.treehole.getList()
        }, 300)
      })

      $panel.on('keypress.treehole', '#thSearchInput', function (e) {
        if (e.which === 13) {
          e.preventDefault()
          if (searchTimer) {
            clearTimeout(searchTimer)
          }
          admin.treehole._keyword = $.trim($(this).val())
          admin.treehole._page = 1
          admin.treehole.getList()
        }
      })

      $panel.on('click.treehole', '#thRefreshBtn', function () {
        admin.treehole.getList()
      })

      $panel.on('change.treehole', '#thSelectAll', function () {
        var checked = $(this).prop('checked')
        $panel.find('.js-th-row-check').prop('checked', checked)
      })

      $panel.on('change.treehole', '.js-th-row-check', function () {
        var total = $panel.find('.js-th-row-check').length
        var checkedCount = $panel.find('.js-th-row-check:checked').length
        $('#thSelectAll').prop('checked', total > 0 && checkedCount === total)
      })

      $panel.on('click.treehole', '.js-th-toggle-expand', function () {
        $(this).toggleClass('is-expanded')
      })

      $panel.on('click.treehole', '.js-th-switch', function () {
        var id = $(this).attr('data-id')
        var curShow = parseInt($(this).attr('data-show'), 10) === 1 ? 1 : 0
        var nextShow = curShow === 1 ? 0 : 1
        admin.treehole.toggleStatus(id, nextShow)
      })

      $panel.on('click.treehole', '.js-th-delete', function () {
        var id = $(this).attr('data-id')
        admin.treehole.delSingle(id)
      })

      $panel.on('click.treehole', '#thBatchApproveBtn', function () {
        admin.treehole.batchApprove()
      })

      $panel.on('click.treehole', '#thBatchDeleteBtn', function () {
        admin.treehole.batchDelete()
      })

      $panel.on('click.treehole', '.js-th-page', function () {
        var targetPage = parseInt($(this).attr('data-page'), 10)
        if (!isNaN(targetPage) && targetPage > 0 && targetPage !== admin.treehole._page) {
          admin.treehole._page = targetPage
          admin.treehole.getList()
        }
      })
    },

    getSelectedIds: function () {
      var ids = []
      $('#tabsPanel_treehole .js-th-row-check:checked').each(function () {
        var id = $(this).val()
        if (id) {
          ids.push(id)
        }
      })
      return ids
    },

    getList: function () {
      $('#loadMsg').text(Label.loadingLabel)
      var queryUrl = Label.servePath + '/console/tree-hole/list?page=' + admin.treehole._page +
        '&pageSize=' + admin.treehole._pageSize +
        '&status=' + encodeURIComponent(admin.treehole._status)
      if (admin.treehole._keyword) {
        queryUrl += '&keyword=' + encodeURIComponent(admin.treehole._keyword)
      }

      $.ajax({
        url: queryUrl,
        type: 'GET',
        cache: false,
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '加载树洞列表失败')
            return
          }
          admin.treehole.renderStats(result.stats || {})
          admin.treehole.renderTable(result.holes || [])
          admin.treehole.renderPagination(result.pagination || {})
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('加载树洞列表失败')
        },
      })
    },

    renderStats: function (stats) {
      $('#thStatTotal').text(stats.totalCount !== undefined ? stats.totalCount : 0)
      $('#thStatVisible').text(stats.visibleCount !== undefined ? stats.visibleCount : 0)
      var pending = stats.pendingCount !== undefined ? stats.pendingCount : 0
      $('#thStatPending').text(pending)
      $('#thTabPending').text(pending)
      $('#thStatLikes').text(stats.totalLikeCount !== undefined ? stats.totalLikeCount : 0)
      $('#thStatWatched').text(stats.totalWatchedCount !== undefined ? stats.totalWatchedCount : 0)
      admin.treehole.updateBadgeCount(pending)
    },

    renderTable: function (items) {
      $('#thSelectAll').prop('checked', false)
      var $tbody = $('#thTableBody')
      if (!items.length) {
        $tbody.html('<tr><td colspan="7" style="text-align: center; color: #999; padding: 32px 0;">暂无符合条件的树洞留言</td></tr>')
        return
      }

      var rowsHtml = ''
      for (var i = 0; i < items.length; i++) {
        var item = items[i]
        var id = escapeHtml(item.oId || '')
        var decodedContent = decodeEntitiesOnce(item.content || '')
        var contentEsc = escapeHtml(decodedContent)
        var timeStr = escapeHtml(formatTime(item.createTime || item.oId))
        var ipEsc = escapeHtml(item.clientIp || '-')
        var locEsc = escapeHtml(formatLocation(item.location))
        var watchedCount = item.watchedCount || 0
        var likeCount = item.likeCount || 0
        var isShow = parseInt(item.isShow, 10) === 1

        var badgeHtml = isShow
          ? '<span class="th-badge th-badge--visible">已公开</span>'
          : '<span class="th-badge th-badge--pending">待审核</span>'

        var switchClass = isShow ? 'th-switch is-on js-th-switch' : 'th-switch js-th-switch'
        var switchText = isShow ? '隐藏' : '公开'

        rowsHtml += '<tr>' +
          '<td class="th-col-check"><input type="checkbox" class="js-th-row-check" value="' + id + '"/></td>' +
          '<td>' +
          '<div class="th-content-cell js-th-toggle-expand" title="点击展开或收起全文">' +
          '<div class="th-content-text th-content-text--collapsed">' + contentEsc + '</div>' +
          '<button type="button" class="th-expand-btn" aria-label="展开或收起">&#8964;</button>' +
          '</div>' +
          '<span class="th-id-tag">#' + id + '</span>' +
          '</td>' +
          '<td style="white-space: nowrap;">' + timeStr + '</td>' +
          '<td><div class="th-ip">' + ipEsc + '</div><div class="th-loc">' + locEsc + '</div></td>' +
          '<td class="th-col-center"><span class="th-metric">' + watchedCount + ' / ' + likeCount + '</span></td>' +
          '<td class="th-col-center">' + badgeHtml + '</td>' +
          '<td>' +
          '<div class="th-actions">' +
          '<span class="' + switchClass + '" data-id="' + id + '" data-show="' + (isShow ? 1 : 0) + '">' +
          '<span class="th-switch-track"><span class="th-switch-thumb"></span></span>' +
          '<span class="th-switch-label">' + switchText + '</span>' +
          '</span>' +
          '<button type="button" class="th-action-del js-th-delete" data-id="' + id + '">删除</button>' +
          '</div>' +
          '</td>' +
          '</tr>'
      }
      $tbody.html(rowsHtml)
    },

    renderPagination: function (pagination) {
      var $el = $('#thPagination')
      var currentPage = pagination.paginationCurrentPageNum || admin.treehole._page || 1
      var pageCount = pagination.paginationPageCount || 1
      var recordCount = pagination.paginationRecordCount || 0
      var nums = pagination.paginationPageNums || [1]

      var html = '<span>共 ' + recordCount + ' 条记录（第 ' + currentPage + ' / ' + pageCount + ' 页）</span>'
      html += '<div class="th-page-btns">'
      html += '<button type="button" class="th-page-btn js-th-page" data-page="' + (currentPage - 1) + '"' +
        (currentPage <= 1 ? ' disabled' : '') + '>上一页</button>'

      for (var i = 0; i < nums.length; i++) {
        var num = nums[i]
        html += '<button type="button" class="th-page-btn js-th-page' +
          (num === currentPage ? ' is-current' : '') + '" data-page="' + num + '">' + num + '</button>'
      }

      html += '<button type="button" class="th-page-btn js-th-page" data-page="' + (currentPage + 1) + '"' +
        (currentPage >= pageCount ? ' disabled' : '') + '>下一页</button>'
      html += '</div>'
      $el.html(html)
    },

    toggleStatus: function (id, targetShow) {
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/tree-hole/' + encodeURIComponent(id),
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ isShow: targetShow }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '更新状态失败')
            return
          }
          $('#tipMsg').text(targetShow === 1 ? '已审核公开该条树洞' : '已隐藏该条树洞')
          admin.treehole.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('更新状态失败')
        },
      })
    },

    delSingle: function (id) {
      if (!window.confirm('确认删除该条树洞留言（ID: ' + id + '）？删除后不可恢复。')) {
        return
      }
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/tree-hole/' + encodeURIComponent(id),
        type: 'DELETE',
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '删除失败')
            return
          }
          $('#tipMsg').text(result.msg || '删除成功')
          admin.treehole.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('删除失败')
        },
      })
    },

    batchApprove: function () {
      var ids = admin.treehole.getSelectedIds()
      if (!ids.length) {
        $('#tipMsg').text('请先勾选需要通过审核的树洞留言')
        return
      }
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/tree-hole/batch/status',
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ ids: ids, isShow: 1 }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '批量通过失败')
            return
          }
          $('#tipMsg').text('已批量公开 ' + (result.updatedCount || ids.length) + ' 条树洞留言')
          admin.treehole.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('批量通过失败')
        },
      })
    },

    batchDelete: function () {
      var ids = admin.treehole.getSelectedIds()
      if (!ids.length) {
        $('#tipMsg').text('请先勾选需要删除的树洞留言')
        return
      }
      if (!window.confirm('确认批量删除选中的 ' + ids.length + ' 条树洞留言？删除后不可恢复。')) {
        return
      }
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/tree-hole/batch/delete',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ ids: ids }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '批量删除失败')
            return
          }
          $('#tipMsg').text('已删除 ' + (result.deletedCount || ids.length) + ' 条树洞留言')
          admin.treehole.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('批量删除失败')
        },
      })
    },
  }

  admin.register.treehole = {
    obj: admin.treehole,
    init: admin.treehole.init,
    refresh: admin.treehole.refresh,
  }

  $(function () {
    admin.treehole.loadBadge()
  })
})()
