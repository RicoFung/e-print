'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { buildBasicAuthorization, buildTemplateHeaders, extractTemplateHtml, getTemplatePath } = require('../src/lib/template-cache');

test('extracts template content from niko result data', () => {
  assert.equal(extractTemplateHtml({
    code: 200,
    msg: 'ok',
    data: {
      content: '<div>{{productName}}</div>'
    }
  }), '<div>{{productName}}</div>');
});

test('extracts template content from plain response body', () => {
  assert.equal(extractTemplateHtml({
    content: '<div>plain</div>'
  }), '<div>plain</div>');
});

test('builds basic authorization header for template requests', () => {
  assert.equal(
    buildBasicAuthorization({ basicUsername: 'eprint', basicPassword: 'eprint123' }),
    'Basic ZXByaW50OmVwcmludDEyMw=='
  );

  assert.deepEqual(buildTemplateHeaders({
    basicUsername: 'eprint',
    basicPassword: 'eprint123'
  }), {
    accept: 'text/html, application/json',
    authorization: 'Basic ZXByaW50OmVwcmludDEyMw=='
  });
});

test('builds cache path with template type namespace', () => {
  assert.match(
    getTemplatePath('/tmp/cache', 'sales_receipt', '01'),
    /sales_receipt[\\/]01\.html$/
  );
});
