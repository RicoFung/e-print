'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { extractTemplateHtml } = require('../src/lib/template-cache');

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
