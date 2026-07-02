(() => {
  'use strict';

  const backButton = document.querySelector('[data-error-back]');
  if (!backButton) {
    return;
  }
  if (window.history.length <= 1) {
    backButton.hidden = true;
    return;
  }
  backButton.addEventListener('click', () => window.history.back());
})();
