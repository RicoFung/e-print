'use strict';

const fs = require('node:fs/promises');
const path = require('node:path');

async function getTemplate(templateCode, config, options) {
  const opts = options || {};
  const cachePath = getTemplatePath(config.templateCacheDir, templateCode);

  if (!opts.forceRefresh) {
    const cached = await readIfExists(cachePath);
    if (cached) {
      return cached;
    }
  }

  const template = await fetchTemplate(templateCode, config, opts);
  await fs.mkdir(path.dirname(cachePath), { recursive: true });
  await fs.writeFile(cachePath, template, 'utf8');
  return template;
}

async function fetchTemplate(templateCode, config, options) {
  const fetchImpl = (options && options.fetch) || globalThis.fetch;
  if (!fetchImpl) {
    throw new Error('fetch is not available in this Node.js runtime');
  }

  const url = `${config.templateBaseUrl.replace(/\/$/, '')}/${encodeURIComponent(templateCode)}`;
  const response = await fetchImpl(url, {
    headers: {
      accept: 'text/html, application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`template ${templateCode} download failed: HTTP ${response.status}`);
  }

  const contentType = response.headers && response.headers.get ? response.headers.get('content-type') : '';
  if (contentType && contentType.includes('application/json')) {
    const body = await response.json();
    return extractTemplateHtml(body);
  }

  return response.text();
}

function extractTemplateHtml(body) {
  const source = body && body.data && typeof body.data === 'object'
    ? body.data
    : body;

  return source && (
    source.html ||
    source.content ||
    source.templateHtml
  ) || '';
}

function getTemplatePath(cacheDir, templateCode) {
  return path.join(cacheDir, `${sanitizeTemplateCode(templateCode)}.html`);
}

function sanitizeTemplateCode(templateCode) {
  return String(templateCode).replace(/[^a-zA-Z0-9._-]/g, '_');
}

async function readIfExists(filePath) {
  try {
    return await fs.readFile(filePath, 'utf8');
  } catch (error) {
    if (error && error.code === 'ENOENT') {
      return null;
    }

    throw error;
  }
}

module.exports = {
  extractTemplateHtml,
  fetchTemplate,
  getTemplate,
  getTemplatePath,
  sanitizeTemplateCode
};
