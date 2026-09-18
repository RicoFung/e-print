'use strict';

const { buildBasicAuthorization } = require('./template-cache');

async function reportPrintResult(result, config, options) {
  const fetchImpl = (options && options.fetch) || globalThis.fetch;
  if (!fetchImpl) {
    throw new Error('fetch is not available in this Node.js runtime');
  }

  const response = await fetchImpl(buildResultUrl(config.templateBaseUrl, result.taskId), {
    method: 'POST',
    headers: buildResultHeaders(config),
    body: JSON.stringify({
      status: String(result.status).toUpperCase(),
      templateType: result.templateType,
      templateCode: result.templateCode,
      message: result.message
    })
  });

  if (!response.ok) {
    throw new Error(`print result report failed: HTTP ${response.status}`);
  }
}

function buildResultUrl(templateBaseUrl, taskId) {
  const url = new URL(templateBaseUrl);
  url.pathname = `${url.pathname.replace(/\/(?:minio|qiniu)\/template\/?$/, '').replace(/\/?template\/?$/, '')}/task/${encodeURIComponent(taskId)}/result`;
  url.search = '';
  url.hash = '';
  return url.toString();
}

function buildResultHeaders(config) {
  const headers = {
    'content-type': 'application/json',
    accept: 'application/json'
  };
  const authorization = buildBasicAuthorization(config);
  if (authorization) {
    headers.authorization = authorization;
  }
  return headers;
}

module.exports = {
  buildResultHeaders,
  buildResultUrl,
  reportPrintResult
};
