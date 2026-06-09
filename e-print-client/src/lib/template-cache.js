'use strict';

const fs = require('node:fs/promises');
const path = require('node:path');

async function getTemplate(templateType, templateCode, config, options) {
  const opts = options || {};
  const cachePath = getTemplatePath(config.templateCacheDir, templateType, templateCode);

  if (!opts.forceRefresh) {
    const cached = await readIfExists(cachePath);
    if (cached) {
      return cached;
    }
  }

  const template = await fetchTemplate(templateType, templateCode, config, opts);
  await fs.mkdir(path.dirname(cachePath), { recursive: true });
  await fs.writeFile(cachePath, template, 'utf8');
  return template;
}

async function fetchTemplate(templateType, templateCode, config, options) {
  const fetchImpl = (options && options.fetch) || globalThis.fetch;
  if (!fetchImpl) {
    throw new Error('fetch is not available in this Node.js runtime');
  }

  const url = `${config.templateBaseUrl.replace(/\/$/, '')}/${encodeURIComponent(templateCode)}?templateType=${encodeURIComponent(templateType)}`;
  const response = await fetchImpl(url, {
    headers: buildTemplateHeaders(config)
  });

  if (!response.ok) {
    throw new Error(`template ${templateType}/${templateCode} download failed: HTTP ${response.status}`);
  }

  const contentType = response.headers && response.headers.get ? response.headers.get('content-type') : '';
  if (contentType && contentType.includes('application/json')) {
    const body = await response.json();
    return extractTemplateHtml(body);
  }

  return response.text();
}

function buildTemplateHeaders(config) {
  const headers = {
    accept: 'text/html, application/json'
  };

  const authorization = buildBasicAuthorization(config);
  if (authorization) {
    headers.authorization = authorization;
  }

  return headers;
}

function buildBasicAuthorization(config) {
  if (!config || !config.basicUsername || !config.basicPassword) {
    return '';
  }

  return `Basic ${Buffer.from(`${config.basicUsername}:${config.basicPassword}`, 'utf8').toString('base64')}`;
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

function getTemplatePath(cacheDir, templateType, templateCode) {
  return path.join(cacheDir, sanitizeTemplateCode(templateType), `${sanitizeTemplateCode(templateCode)}.html`);
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
  buildBasicAuthorization,
  buildTemplateHeaders,
  fetchTemplate,
  getTemplate,
  getTemplatePath,
  sanitizeTemplateCode
};
