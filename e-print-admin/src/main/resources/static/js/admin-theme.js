(function () {
  'use strict';

  const storageKey = 'lte-theme';
  const loadingDebugKey = 'admin.loading.debug';
  const mediaQuery = globalThis.matchMedia('(prefers-color-scheme: dark)');

  function getStoredTheme() {
    try {
      return localStorage.getItem(storageKey);
    } catch (error) {
      return null;
    }
  }

  function setStoredTheme(theme) {
    try {
      localStorage.setItem(storageKey, theme);
    } catch (error) {
      // Ignore storage failures in private or restricted browser contexts.
    }
  }

  function getPreferredTheme() {
    const storedTheme = getStoredTheme();
    if (storedTheme === 'light' || storedTheme === 'dark' || storedTheme === 'auto') {
      return storedTheme;
    }
    return 'auto';
  }

  function resolveTheme(theme) {
    if (theme === 'dark') {
      return 'dark';
    }
    if (theme === 'light') {
      return 'light';
    }
    return mediaQuery.matches ? 'dark' : 'light';
  }

  function setTheme(theme) {
    const resolvedTheme = resolveTheme(theme);
    document.documentElement.setAttribute('data-bs-theme', resolvedTheme);
    document.documentElement.style.colorScheme = resolvedTheme;
  }

  function showActiveTheme(theme, focus) {
    const themeSwitcher = document.getElementById('bd-theme');
    if (!themeSwitcher) {
      return;
    }

    const activeButton = document.querySelector(`[data-bs-theme-value="${theme}"]`);
    const activeLabel = activeButton ? activeButton.dataset.bsThemeLabel : '跟随系统';

    document.querySelectorAll('[data-bs-theme-value]').forEach((button) => {
      button.classList.remove('active');
      button.setAttribute('aria-pressed', 'false');
      const check = button.querySelector('.theme-check');
      if (check) {
        check.classList.add('d-none');
      }
    });

    if (activeButton) {
      activeButton.classList.add('active');
      activeButton.setAttribute('aria-pressed', 'true');
      const check = activeButton.querySelector('.theme-check');
      if (check) {
        check.classList.remove('d-none');
      }
    }

    themeSwitcher.querySelectorAll('[data-lte-theme-icon]').forEach((icon) => {
      icon.classList.toggle('d-none', icon.dataset.lteThemeIcon !== theme);
    });
    themeSwitcher.setAttribute('aria-label', `切换颜色模式，当前为${activeLabel}`);

    if (focus) {
      themeSwitcher.focus();
    }
  }

  function ensurePageLoadingOverlay() {
    let overlay = document.getElementById('adminPageLoading');
    if (overlay) {
      return overlay;
    }
    overlay = document.createElement('div');
    overlay.id = 'adminPageLoading';
    overlay.className = 'admin-page-loading';
    overlay.setAttribute('role', 'status');
    overlay.setAttribute('aria-live', 'polite');
    overlay.innerHTML = '<div class="admin-page-loading-box"><svg class="material-spinner" viewBox="0 0 50 50" aria-hidden="true"><circle class="material-spinner-circle" cx="25" cy="25" r="20"></circle></svg><span class="admin-page-loading-text">加载中...</span></div>';
    document.body.appendChild(overlay);
    return overlay;
  }

  function showPageLoading() {
    ensurePageLoadingOverlay().classList.add('is-active');
  }

  function hidePageLoading() {
    const overlay = document.getElementById('adminPageLoading');
    if (overlay) {
      overlay.classList.remove('is-active');
    }
  }

  function isPageLoadingDebug() {
    try {
      return localStorage.getItem(loadingDebugKey) === '1';
    } catch (error) {
      return false;
    }
  }

  function isPlainLeftClick(event) {
    return event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey;
  }

  function isNavigableLink(link) {
    if (!link || link.hasAttribute('download')) {
      return false;
    }
    const target = (link.getAttribute('target') || '').toLowerCase();
    if (target && target !== '_self') {
      return false;
    }
    const rawHref = link.getAttribute('href') || '';
    if (!rawHref || rawHref === '#' || rawHref.startsWith('#')) {
      return false;
    }
    if (/^(javascript:|mailto:|tel:|data:|blob:)/i.test(rawHref)) {
      return false;
    }
    const url = new URL(link.href, window.location.href);
    return url.origin === window.location.origin;
  }

  function isStaticSubmit(form) {
    if (!form || form.matches('[data-ajax-save], [data-confirm]')) {
      return false;
    }
    const target = (form.getAttribute('target') || '').toLowerCase();
    return !target || target === '_self';
  }

  globalThis.showPageLoading = showPageLoading;
  globalThis.hidePageLoading = hidePageLoading;

  mediaQuery.addEventListener('change', () => {
    const theme = getPreferredTheme();
    if (theme === 'auto') {
      setTheme(theme);
    }
  });

  document.addEventListener('DOMContentLoaded', () => {
    const theme = getPreferredTheme();
    setTheme(theme);
    showActiveTheme(theme, false);

    document.querySelectorAll('[data-bs-theme-value]').forEach((toggle) => {
      toggle.addEventListener('click', () => {
        const selectedTheme = toggle.getAttribute('data-bs-theme-value') || 'auto';
        setStoredTheme(selectedTheme);
        setTheme(selectedTheme);
        showActiveTheme(selectedTheme, true);
      });
    });

    document.addEventListener('click', (event) => {
      if (event.defaultPrevented || !isPlainLeftClick(event)) {
        return;
      }
      const target = event.target instanceof Element ? event.target : event.target.parentElement;
      const link = target ? target.closest('a[href]') : null;
      if (isNavigableLink(link)) {
        showPageLoading();
        if (isPageLoadingDebug()) {
          event.preventDefault();
        }
      }
    });

    document.addEventListener('submit', (event) => {
      if (event.defaultPrevented || !isStaticSubmit(event.target)) {
        return;
      }
      if (event.target.checkValidity && !event.target.checkValidity()) {
        return;
      }
      showPageLoading();
      if (isPageLoadingDebug()) {
        event.preventDefault();
      }
    });

    window.addEventListener('pageshow', hidePageLoading);
  });
})();
