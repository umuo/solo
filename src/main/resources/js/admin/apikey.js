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
 * API Key management for admin.
 *
 * @version 1.0.0.0, Nov 18, 2026
 */
(function () {
  if (typeof window.admin === 'undefined') {
    return
  }

  if (admin.tools && admin.tools.indexOf('#apikey') === -1) {
    admin.tools.push('#apikey')
  }

  var pad = function (num) {
    return num < 10 ? '0' + num : String(num)
  }

  var formatTime = function (ts) {
    if (!ts || ts <= 0) {
      return '-'
    }
    var d = new Date(ts)
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
      ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
  }

  var formatRelative = function (ts) {
    if (!ts || ts <= 0) {
      return ''
    }
    var diff = Math.floor((Date.now() - ts) / 1000)
    if (diff < 60) {
      return '刚刚'
    }
    if (diff < 3600) {
      return Math.floor(diff / 60) + ' 分钟前'
    }
    if (diff < 86400) {
      return Math.floor(diff / 3600) + ' 小时前'
    }
    return Math.floor(diff / 86400) + ' 天前'
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

  admin.apikey = {
    _boundEvents: false,
    _lastRawKey: '',

    init: function () {
      $('#loadMsg').text('')
      admin.apikey.bindEvents()
      admin.apikey.getList()
    },

    refresh: function () {
      admin.clearTip()
      admin.apikey.bindEvents()
      admin.apikey.getList()
    },

    bindEvents: function () {
      var $panel = $('#tabsPanel_apikey')
      $panel.off('keypress.apikey', '#apiKeyNameInput').on('keypress.apikey', '#apiKeyNameInput', function (e) {
        if (e.which === 13) {
          e.preventDefault()
          admin.apikey.create()
        }
      })

      if (admin.apikey._boundEvents) {
        return
      }
      admin.apikey._boundEvents = true

      $panel.on('click.apikey', '.js-apikey-toggle', function () {
        var id = $(this).attr('data-id')
        var enabled = $(this).attr('data-enabled') === 'true'
        admin.apikey.toggleStatus(id, !enabled)
      })

      $panel.on('click.apikey', '.js-apikey-rename', function () {
        var id = $(this).attr('data-id')
        var currentName = $(this).attr('data-name') || ''
        admin.apikey.rename(id, currentName)
      })

      $panel.on('click.apikey', '.js-apikey-delete', function () {
        var id = $(this).attr('data-id')
        var name = $(this).attr('data-name') || ''
        admin.apikey.del(id, name)
      })
    },

    getList: function () {
      $('#loadMsg').text(Label.loadingLabel)
      $.ajax({
        url: Label.servePath + '/console/api-key',
        type: 'GET',
        cache: false,
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '加载 API Key 列表失败')
            return
          }
          admin.apikey.renderTable(result.apiKeys || [])
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('加载 API Key 列表失败')
        },
      })
    },

    renderTable: function (items) {
      var enabledCount = 0
      for (var i = 0; i < items.length; i++) {
        if (items[i].enabled) {
          enabledCount++
        }
      }
      $('#apiKeySummary').text('（共 ' + items.length + ' 个，已启用 ' + enabledCount + ' 个）')

      var $tbody = $('#apiKeyTableBody')
      if (!items.length) {
        $tbody.html('<tr><td colspan="8" style="text-align: center; color: #999;">暂无 API Key，请在上方创建。</td></tr>')
        return
      }

      var rowsHtml = ''
      for (var j = 0; j < items.length; j++) {
        var item = items[j]
        var idEsc = escapeHtml(item.id)
        var nameEsc = escapeHtml(item.name || 'API Key')
        var maskedEsc = escapeHtml(item.maskedKey || '')
        var enabled = !!item.enabled
        var badgeHtml = enabled
          ? '<span class="apikey-badge apikey-badge--enabled">已启用</span>'
          : '<span class="apikey-badge apikey-badge--disabled">已禁用</span>'

        var createdStr = formatTime(item.createdAt)
        var lastUsedHtml = '<span style="color: #999;">从未调用</span>'
        if (item.lastUsedAt && item.lastUsedAt > 0) {
          lastUsedHtml = escapeHtml(formatTime(item.lastUsedAt)) +
            '<span class="apikey-sub">' + escapeHtml(formatRelative(item.lastUsedAt)) + '</span>'
        }

        var detailHtml = '<span style="color: #999;">-</span>'
        if (item.lastUsedEndpoint || item.lastUsedIp) {
          detailHtml = '<span class="apikey-mono">' + escapeHtml(item.lastUsedEndpoint || '-') + '</span>' +
            '<span class="apikey-sub">IP: ' + escapeHtml(item.lastUsedIp || '-') + '</span>'
        }

        var toggleLabel = enabled ? '禁用' : '启用'
        var toggleClass = enabled ? 'apikey-btn-secondary' : ''

        rowsHtml += '<tr>' +
          '<td><strong>' + nameEsc + '</strong></td>' +
          '<td><span class="apikey-mono">' + maskedEsc + '</span></td>' +
          '<td>' + badgeHtml + '</td>' +
          '<td>' + escapeHtml(createdStr) + '</td>' +
          '<td>' + lastUsedHtml + '</td>' +
          '<td>' + detailHtml + '</td>' +
          '<td>' + (item.callCount || 0) + '</td>' +
          '<td class="apikey-actions">' +
          '<button type="button" class="js-apikey-toggle ' + toggleClass + '" data-id="' + idEsc + '" data-enabled="' + enabled + '">' + toggleLabel + '</button>' +
          '<button type="button" class="js-apikey-rename apikey-btn-secondary" data-id="' + idEsc + '" data-name="' + nameEsc + '">重命名</button>' +
          '<button type="button" class="js-apikey-delete apikey-btn-danger" data-id="' + idEsc + '" data-name="' + nameEsc + '">删除</button>' +
          '</td>' +
          '</tr>'
      }

      $tbody.html(rowsHtml)
    },

    create: function () {
      var name = $.trim($('#apiKeyNameInput').val())
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/api-key',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ name: name }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '创建 API Key 失败')
            return
          }
          $('#apiKeyNameInput').val('')
          admin.apikey._lastRawKey = result.apiKey || ''
          $('#apiKeyNewValue').text(admin.apikey._lastRawKey)
          $('#apiKeyNewBanner').removeClass('fn__none')
          $('#tipMsg').text(result.msg || '创建成功')
          admin.apikey.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('创建 API Key 失败')
        },
      })
    },

    toggleStatus: function (id, targetEnabled) {
      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/api-key/' + encodeURIComponent(id),
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ enabled: targetEnabled }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '更新状态失败')
            return
          }
          $('#tipMsg').text(targetEnabled ? '已启用 API Key' : '已禁用 API Key')
          admin.apikey.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('更新状态失败')
        },
      })
    },

    rename: function (id, currentName) {
      var newName = window.prompt('请输入新的 API Key 名称（最多 50 字符）：', currentName)
      if (newName === null) {
        return
      }
      newName = $.trim(newName)
      if (!newName) {
        $('#tipMsg').text('名称不能为空')
        return
      }

      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/api-key/' + encodeURIComponent(id),
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ name: newName }),
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '重命名失败')
            return
          }
          $('#tipMsg').text(result.msg || '重命名成功')
          admin.apikey.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('重命名失败')
        },
      })
    },

    del: function (id, name) {
      if (!window.confirm('确认删除 API Key「' + name + '」？删除后使用该密钥的客户端将立即失效。')) {
        return
      }

      $('#loadMsg').text(Label.loadingLabel)
      $('#tipMsg').text('')

      $.ajax({
        url: Label.servePath + '/console/api-key/' + encodeURIComponent(id),
        type: 'DELETE',
        success: function (result) {
          $('#loadMsg').text('')
          if (!result || result.code !== 0) {
            $('#tipMsg').text((result && result.msg) || '删除失败')
            return
          }
          $('#tipMsg').text(result.msg || '删除成功')
          admin.apikey.getList()
        },
        error: function () {
          $('#loadMsg').text('')
          $('#tipMsg').text('删除失败')
        },
      })
    },

    copyKey: function () {
      var raw = admin.apikey._lastRawKey || $('#apiKeyNewValue').text()
      if (!raw) {
        return
      }
      var onCopied = function () {
        $('#tipMsg').text('API Key 已复制到剪贴板')
      }
      var fallbackCopy = function (text) {
        var textarea = document.createElement('textarea')
        textarea.value = text
        textarea.style.position = 'fixed'
        textarea.style.top = '-9999px'
        textarea.style.left = '-9999px'
        document.body.appendChild(textarea)
        textarea.select()
        try {
          document.execCommand('copy')
          onCopied()
        } catch (e) {
          $('#tipMsg').text('自动复制失败，请手动复制密钥')
        }
        document.body.removeChild(textarea)
      }

      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(raw).then(onCopied, function () {
          fallbackCopy(raw)
        })
      } else {
        fallbackCopy(raw)
      }
    },

    hideNewBanner: function () {
      admin.apikey._lastRawKey = ''
      $('#apiKeyNewValue').text('')
      $('#apiKeyNewBanner').addClass('fn__none')
    },
  }

  admin.register.apikey = {
    obj: admin.apikey,
    init: admin.apikey.init,
    refresh: admin.apikey.refresh,
  }
})()
