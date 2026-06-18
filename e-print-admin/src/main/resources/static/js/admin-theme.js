(function () {
  'use strict';

  const storageKey = 'lte-theme';
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
  });
})();
