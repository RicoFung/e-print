(function () {
  function confirmAction(message, options = {}) {
    if (!window.Swal) {
      return Promise.resolve(false);
    }

    return window.Swal.fire({
      title: options.title || '确认操作',
      text: message || '确认继续执行该操作？',
      icon: options.icon || 'warning',
      showCancelButton: true,
      confirmButtonText: options.okText || '确认',
      cancelButtonText: '取消',
      reverseButtons: true,
      focusCancel: true,
      buttonsStyling: false,
      customClass: {
        popup: 'admin-swal',
        confirmButton: `btn ${options.okClass || 'btn-primary'}`,
        cancelButton: 'btn btn-outline-secondary'
      }
    }).then((result) => result.isConfirmed);
  }

  document.addEventListener('submit', async (event) => {
    const form = event.target.closest('form[data-confirm]');
    if (!form) {
      return;
    }
    event.preventDefault();

    const confirmed = await confirmAction(form.getAttribute('data-confirm'), {
      title: form.getAttribute('data-confirm-title') || '确认操作',
      okText: form.getAttribute('data-confirm-ok') || '确认',
      okClass: form.getAttribute('data-confirm-class') || 'btn-primary'
    });
    if (confirmed) {
      HTMLFormElement.prototype.submit.call(form);
    }
  });

  document.querySelectorAll('.auto-dismiss-alert').forEach((alert) => {
    window.setTimeout(() => {
      alert.classList.add('alert-dismissing');
      window.setTimeout(() => alert.remove(), 260);
    }, 2600);
  });

  const templateCode = document.getElementById('templateCode');
  const templateType = document.getElementById('templateType');
  const objectName = document.getElementById('objectName');
  if (templateCode && objectName) {
    templateCode.addEventListener('blur', () => {
      if (objectName.value.trim()) {
        return;
      }
      const code = templateCode.value.trim();
      if (!code) {
        return;
      }
      const prefix = templateCode.getAttribute('data-object-prefix') || 'templates/print';
      const type = templateType ? templateType.value.trim() : '';
      objectName.value = type ? `${prefix}/${type}/${code}.html` : `${prefix}/${code}.html`;
    });
  }

  const fileInput = document.getElementById('templateFile');
  const content = document.getElementById('content');
  if (fileInput && content) {
    fileInput.addEventListener('change', () => {
      const file = fileInput.files && fileInput.files[0];
      if (!file) {
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        content.value = reader.result || '';
        content.focus();
      };
      reader.readAsText(file, 'UTF-8');
    });
  }

  const legacyPreviewWorkbench = document.querySelector('.preview-workbench[data-render-url]');
  const previewModalElement = document.getElementById('templatePreviewModal');
  const previewModal = previewModalElement && window.bootstrap
    ? window.bootstrap.Modal.getOrCreateInstance(previewModalElement)
    : null;
  const sampleData = document.getElementById('sampleData');
  const renderPreviewBtn = document.getElementById('renderPreviewBtn');
  const previewFrame = document.getElementById('previewFrame');
  const previewError = document.getElementById('previewError');

  const previewSubtitle = document.getElementById('templatePreviewSubtitle');
  let previewRequest = null;

  async function renderPreview() {
    if (!previewRequest || !sampleData || !previewFrame) {
      return;
    }
    const body = new URLSearchParams();
    body.set('sampleData', sampleData.value);
    if (previewRequest.contentProvider) {
      body.set('content', previewRequest.contentProvider());
    }

    if (previewError) {
      previewError.hidden = true;
      previewError.textContent = '';
    }
    if (renderPreviewBtn) {
      renderPreviewBtn.disabled = true;
    }

    try {
      const response = await fetch(previewRequest.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body
      });
      const html = await response.text();
      if (!response.ok) {
        throw new Error(html || 'Preview render failed');
      }
      previewFrame.srcdoc = html;
    } catch (error) {
      if (previewError) {
        previewError.textContent = error.message || 'Preview render failed';
        previewError.hidden = false;
      }
    } finally {
      if (renderPreviewBtn) {
        renderPreviewBtn.disabled = false;
      }
    }
  }

  if (renderPreviewBtn) {
    renderPreviewBtn.addEventListener('click', renderPreview);
  }

  if (legacyPreviewWorkbench) {
    previewRequest = { url: legacyPreviewWorkbench.getAttribute('data-render-url') };
    renderPreview();
  }

  function openPreview(options) {
    if (!previewModal) {
      return;
    }
    previewRequest = options;
    if (previewSubtitle) {
      previewSubtitle.textContent = options.subtitle || '使用模拟数据渲染模板';
    }
    previewFrame.removeAttribute('src');
    previewFrame.srcdoc = '';
    previewModal.show();
    renderPreview();
  }

  document.addEventListener('click', (event) => {
    const button = event.target.closest('[data-template-preview-id]');
    if (!button) {
      return;
    }
    const id = encodeURIComponent(button.getAttribute('data-template-preview-id'));
    openPreview({
      url: `/admin/templates/${id}/preview/render`,
      subtitle: button.getAttribute('data-template-preview-name') || '已保存模板'
    });
  });

  const previewCurrentTemplate = document.getElementById('previewCurrentTemplate');
  if (previewCurrentTemplate && content) {
    previewCurrentTemplate.addEventListener('click', () => openPreview({
      url: '/admin/templates/preview/render',
      subtitle: '当前编辑内容（无需保存）',
      contentProvider: () => content.value
    }));
  }

  const templateTable = document.getElementById('templateTable');
  if (templateTable && window.jQuery && typeof window.jQuery.fn.bootstrapTable === 'function') {
    const $table = window.jQuery(templateTable);
    const filterForm = document.getElementById('templateFilterForm');
    const resetFilter = document.getElementById('resetTemplateFilter');
    const bulkDisable = document.getElementById('disableSelectedTemplates');
    const bulkEnable = document.getElementById('enableSelectedTemplates');
    const bulkDelete = document.getElementById('deleteSelectedTemplates');
    const bulkButtons = [bulkDisable, bulkEnable, bulkDelete].filter(Boolean);
    const selectionSummary = document.getElementById('templateSelectionSummary');
    const templateTypeFilter = document.getElementById('templateTypeFilter');
    const templateCodeFilter = document.getElementById('templateCodeFilter');
    const statusFilter = document.getElementById('statusFilter');
    const createTemplateLink = document.getElementById('createTemplateLink');
    const initialPage = Math.max(1, Number(templateTable.dataset.initialPage) || 1);
    const initialPageSize = Math.max(1, Number(templateTable.dataset.initialPageSize) || 10);
    const tableWrap = templateTable.closest('.list-table-wrap');
    const sortableFields = new Set(['templateType', 'templateCode', 'bucketName', 'objectName', 'status']);
    const parseSortState = () => {
      const params = new URLSearchParams(window.location.search);
      const seenFields = new Set();
      return (params.get('sort') || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
        .map((item) => {
          const separatorIndex = item.lastIndexOf('.');
          if (separatorIndex <= 0 || separatorIndex >= item.length - 1) {
            return null;
          }
          return {
            field: item.slice(0, separatorIndex),
            order: item.slice(separatorIndex + 1).toLowerCase()
          };
        })
        .filter((item) => item
          && sortableFields.has(item.field)
          && (item.order === 'asc' || item.order === 'desc')
          && !seenFields.has(item.field)
          && seenFields.add(item.field));
    };
    let sortState = parseSortState();

    const encodeSortState = () => sortState
      .map((item) => `${item.field}.${item.order}`)
      .join(',');

    const nextSortOrder = (field) => {
      const current = sortState.find((item) => item.field === field);
      if (!current) {
        return 'asc';
      }
      return current.order === 'asc' ? 'desc' : '';
    };

    const syncSortHeaders = () => {
      if (!tableWrap) {
        return;
      }
      tableWrap.querySelectorAll('th[data-field]').forEach((header) => {
        const field = header.getAttribute('data-field');
        const inner = header.querySelector('.th-inner') || header;
        const existingIndicator = inner.querySelector('.admin-sort-indicator');
        if (existingIndicator) {
          existingIndicator.remove();
        }

        header.classList.remove('admin-sortable', 'admin-sort-active', 'admin-sort-asc', 'admin-sort-desc');
        header.removeAttribute('aria-sort');
        header.removeAttribute('title');
        if (!sortableFields.has(field)) {
          return;
        }

        header.classList.add('admin-sortable');
        header.setAttribute('title', '点击加入多列排序，Ctrl/⌘+点击仅排序当前列');
        const sortIndex = sortState.findIndex((item) => item.field === field);
        if (sortIndex < 0) {
          return;
        }

        const state = sortState[sortIndex];
        const indicator = document.createElement('span');
        indicator.className = 'admin-sort-indicator';
        indicator.innerHTML = '<span class="admin-sort-triangle" aria-hidden="true"></span>';
        if (sortState.length > 1) {
          const priority = document.createElement('span');
          priority.className = 'admin-sort-priority';
          priority.textContent = String(sortIndex + 1);
          indicator.appendChild(priority);
        }
        inner.appendChild(indicator);
        header.classList.add('admin-sort-active', `admin-sort-${state.order}`);
        header.setAttribute('aria-sort', state.order === 'asc' ? 'ascending' : 'descending');
      });
    };

    const applySort = (field, singleColumn) => {
      if (!sortableFields.has(field)) {
        return;
      }
      const order = nextSortOrder(field);
      if (singleColumn) {
        sortState = order ? [{ field, order }] : [];
      } else if (!order) {
        sortState = sortState.filter((item) => item.field !== field);
      } else {
        const existing = sortState.find((item) => item.field === field);
        if (existing) {
          existing.order = order;
        } else {
          sortState.push({ field, order });
        }
      }
      syncSortHeaders();
      $table.bootstrapTable('refresh', { pageNumber: 1 });
    };

    const currentListUrl = (pageNumber, pageSize) => {
      const params = new URLSearchParams();
      const templateType = templateTypeFilter ? templateTypeFilter.value : '';
      const templateCode = templateCodeFilter ? templateCodeFilter.value.trim() : '';
      const status = statusFilter ? statusFilter.value : '';
      const sort = encodeSortState();
      if (templateType) {
        params.set('templateType', templateType);
      }
      if (templateCode) {
        params.set('templateCode', templateCode);
      }
      if (status !== '') {
        params.set('status', status);
      }
      if (sort) {
        params.set('sort', sort);
      }
      params.set('page', String(pageNumber));
      params.set('pageSize', String(pageSize));
      return `/admin/templates?${params.toString()}`;
    };

    const syncListUrl = () => {
      const options = $table.bootstrapTable('getOptions');
      const returnUrl = currentListUrl(options.pageNumber || 1, options.pageSize || initialPageSize);
      window.templateListReturnUrl = returnUrl;
      window.history.replaceState(null, '', returnUrl);
      if (createTemplateLink) {
        createTemplateLink.href = `/admin/templates/new?returnUrl=${encodeURIComponent(returnUrl)}`;
      }
      document.querySelectorAll('[data-template-edit-id]').forEach((link) => {
        const id = encodeURIComponent(link.getAttribute('data-template-edit-id'));
        link.href = `/admin/templates/${id}/edit?returnUrl=${encodeURIComponent(returnUrl)}`;
      });
    };

    const getSelectedIds = () => $table.bootstrapTable('getSelections')
      .map((row) => row.id)
      .filter((id) => id != null);

    const syncBulkState = () => {
      const count = getSelectedIds().length;
      if (selectionSummary) {
        selectionSummary.textContent = count > 0 ? `已选择 ${count} 项` : '未选择';
      }
      bulkButtons.forEach((button) => {
        button.disabled = count === 0;
      });
      if (bulkDisable) {
        bulkDisable.textContent = '批量禁用';
      }
      if (bulkEnable) {
        bulkEnable.textContent = '批量启用';
      }
      if (bulkDelete) {
        bulkDelete.textContent = '批量删除';
      }
    };

    const resizeTable = () => {
      const tableWrap = templateTable.closest('.list-table-wrap');
      const actionbar = tableWrap.querySelector('.table-actionbar');
      const rect = tableWrap.getBoundingClientRect();
      const actionbarHeight = actionbar ? actionbar.getBoundingClientRect().height : 0;
      const height = Math.max(280, Math.floor(rect.height - actionbarHeight));
      $table.bootstrapTable('resetView', { height });
    };

    async function postSelected(url, ids) {
      const body = new URLSearchParams();
      ids.forEach((id) => body.append('ids', id));
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body
      });
      if (!response.ok) {
        throw new Error('操作失败，请稍后重试');
      }
    }

    async function runBulkAction(options) {
      const ids = getSelectedIds();
      if (ids.length === 0) {
        return;
      }
      const confirmed = await confirmAction(options.message(ids.length), {
        title: options.title,
        okText: options.okText,
        okClass: options.okClass
      });
      if (!confirmed) {
        return;
      }

      bulkButtons.forEach((button) => {
        button.disabled = true;
      });
      try {
        await postSelected(options.url, ids);
        $table.bootstrapTable('refresh');
      } catch (error) {
        window.alert(error.message || '操作失败，请稍后重试');
        syncBulkState();
      }
    }

    $table.bootstrapTable({
      locale: 'zh-CN',
      height: Math.max(280, Math.floor(templateTable.closest('.list-table-wrap').getBoundingClientRect().height - 52)),
      stickyHeader: true,
      fixedColumns: true,
      fixedNumber: 2,
      fixedRightNumber: 1,
      mobileResponsive: true,
      sidePagination: 'server',
      pagination: true,
      pageNumber: initialPage,
      pageSize: initialPageSize,
      pageList: [10, 20, 50, 100],
      search: false,
      searchOnEnterKey: false,
      showRefresh: false,
      queryParams(params) {
        return {
          limit: params.limit,
          offset: params.offset,
          search: '',
          templateType: templateTypeFilter ? templateTypeFilter.value : '',
          templateCode: templateCodeFilter ? templateCodeFilter.value.trim() : '',
          status: statusFilter ? statusFilter.value : '',
          sort: encodeSortState()
        };
      },
      classes: 'table table-hover align-middle',
      undefinedText: '',
      formatLoadingMessage() {
        return '<span class="table-loading-text">加载中...</span>';
      },
      formatNoMatches() {
        return '暂无匹配的模板';
      },
      formatShowingRows(pageFrom, pageTo, totalRows) {
        return `第 ${pageFrom}-${pageTo} 条，共 ${totalRows} 条`;
      },
      formatRecordsPerPage(pageNumber) {
        return `${pageNumber} 条/页`;
      }
    });

    $table.on('check.bs.table uncheck.bs.table check-all.bs.table uncheck-all.bs.table load-success.bs.table post-body.bs.table', syncBulkState);
    $table.on('load-success.bs.table page-change.bs.table', syncListUrl);
    $table.on('post-header.bs.table post-body.bs.table reset-view.bs.table load-success.bs.table', syncSortHeaders);

    if (tableWrap) {
      tableWrap.addEventListener('click', (event) => {
        const header = event.target.closest('th[data-field]');
        if (!header || !tableWrap.contains(header)) {
          return;
        }
        const field = header.getAttribute('data-field');
        if (!sortableFields.has(field)) {
          return;
        }
        event.preventDefault();
        event.stopPropagation();
        applySort(field, event.ctrlKey || event.metaKey);
      }, true);
    }

    if (filterForm) {
      filterForm.addEventListener('submit', (event) => {
        event.preventDefault();
        $table.bootstrapTable('refresh', { pageNumber: 1 });
      });
    }

    if (resetFilter) {
      resetFilter.addEventListener('click', () => {
        if (templateTypeFilter) {
          templateTypeFilter.value = '';
        }
        if (templateCodeFilter) {
          templateCodeFilter.value = '';
        }
        if (statusFilter) {
          statusFilter.value = '';
        }
        $table.bootstrapTable('refresh', { pageNumber: 1 });
      });
    }

    if (bulkDisable) {
      bulkDisable.addEventListener('click', () => runBulkAction({
        url: '/admin/templates/disable',
        title: '批量禁用',
        okText: '禁用',
        okClass: 'btn-warning',
        message: (count) => `确认禁用选中的 ${count} 个模板？`
      }));
    }

    if (bulkEnable) {
      bulkEnable.addEventListener('click', () => runBulkAction({
        url: '/admin/templates/enable',
        title: '批量启用',
        okText: '启用',
        okClass: 'btn-success',
        message: (count) => `确认启用选中的 ${count} 个模板？`
      }));
    }

    if (bulkDelete) {
      bulkDelete.addEventListener('click', () => runBulkAction({
        url: '/admin/templates/delete',
        title: '批量删除',
        okText: '删除',
        okClass: 'btn-danger',
        message: (count) => `确认删除选中的 ${count} 个模板？`
      }));
    }

    window.addEventListener('resize', resizeTable);
    if (window.visualViewport) {
      window.visualViewport.addEventListener('resize', resizeTable);
    }
    if (window.ResizeObserver) {
      const tableWrapObserver = new ResizeObserver(resizeTable);
      tableWrapObserver.observe(templateTable.closest('.list-table-wrap'));
    }
    setTimeout(syncSortHeaders, 0);
    setTimeout(resizeTable, 0);
  }
})();

function escapeHtml(value) {
  return String(value == null ? '' : value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function objectNameFormatter(value) {
  return `<span class="object-name">${escapeHtml(value)}</span>`;
}

function templateTypeFormatter(value) {
  const labels = {
    sales_receipt: '销售小票',
    sales_receipt_ed: '销售小票-ed',
    sales_receipt_ed2: '销售小票-ed2',
    sales_receipt_o2o: '销售小票-o2o',
    shipping_label: '物流面单',
    shipping_label_o2o: '物流面单-o2o',
    shipping_label_transfer_out: '物流面单-横调出库',
    shipping_label_return_apply: '物流面单-退货申请'
  };
  return escapeHtml(labels[value] || value || '');
}

function statusFormatter(value) {
  if (Number(value) === 1) {
    return '<span class="badge status-badge status-on">启用</span>';
  }
  return '<span class="badge status-badge status-off">禁用</span>';
}

function actionFormatter(value, row) {
  const id = encodeURIComponent(row.id);
  const returnUrl = encodeURIComponent(window.templateListReturnUrl || `${window.location.pathname}${window.location.search}`);
  const previewName = escapeHtml(row.templateCode || '已保存模板');
  const preview = `<button class="btn btn-outline-secondary btn-sm" type="button" data-template-preview-id="${id}" data-template-preview-name="${previewName}">预览</button>`;
  const edit = `<a class="btn btn-outline-primary btn-sm" data-template-edit-id="${id}" href="/admin/templates/${id}/edit?returnUrl=${returnUrl}">编辑</a>`;
  const statusAction = Number(row.status) === 1
    ? `<form action="/admin/templates/${id}/disable" method="post" data-confirm="确认禁用该模板？" data-confirm-title="禁用模板" data-confirm-ok="禁用" data-confirm-class="btn-warning"><button class="btn btn-outline-warning btn-sm" type="submit">禁用</button></form>`
    : `<form action="/admin/templates/${id}/enable" method="post" data-confirm="确认启用该模板？" data-confirm-title="启用模板" data-confirm-ok="启用" data-confirm-class="btn-success"><button class="btn btn-outline-success btn-sm" type="submit">启用</button></form>`;
  const deleteAction = `<form action="/admin/templates/${id}/delete" method="post" data-confirm="确认删除该模板？" data-confirm-title="删除模板" data-confirm-ok="删除" data-confirm-class="btn-danger"><button class="btn btn-outline-danger btn-sm" type="submit">删除</button></form>`;
  return `<div class="row-actions">${preview}${edit}${statusAction}${deleteAction}</div>`;
}
