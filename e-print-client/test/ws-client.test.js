'use strict';

const assert = require('node:assert/strict');
const EventEmitter = require('node:events');
const test = require('node:test');
const { buildClientUrl, startPrintClient } = require('../src/lib/ws-client');

test('handles websocket connection errors without throwing and schedules reconnect', () => {
  const sockets = [];
  const warnings = [];
  let scheduledDelay;
  let scheduledCallback;

  class FakeWebSocket extends EventEmitter {
    constructor(url) {
      super();
      this.url = url;
      this.sent = [];
      sockets.push(this);
    }

    send(payload) {
      this.sent.push(payload);
    }

    close() {
      this.emit('close');
    }
  }

  const client = startPrintClient({
    clientId: 'CLIENT-001',
    serverUrl: 'ws://localhost:9090/ws/print'
  }, {
    WebSocket: FakeWebSocket,
    reconnectDelayMs: 25,
    logger: {
      warn: (message) => warnings.push(message)
    },
    timers: {
      setTimeout: (callback, delay) => {
        scheduledCallback = callback;
        scheduledDelay = delay;
        return 1;
      },
      clearTimeout: () => {}
    }
  });

  sockets[0].emit('error', new Error('connect ECONNREFUSED 127.0.0.1:9090'));
  sockets[0].emit('close');

  assert.equal(warnings.length, 1);
  assert.match(warnings[0], /ECONNREFUSED/);
  assert.equal(scheduledDelay, 25);

  scheduledCallback();
  assert.equal(sockets.length, 2);

  client.stop();
});

test('connects with clientId query parameter', () => {
  const sockets = [];

  class FakeWebSocket extends EventEmitter {
    constructor(url) {
      super();
      this.url = url;
      this.sent = [];
      sockets.push(this);
    }

    send(payload) {
      this.sent.push(JSON.parse(payload));
    }

    close() {}
  }

  startPrintClient({
    clientId: 'CLIENT-001',
    serverUrl: 'ws://localhost:9090/ws/print'
  }, {
    WebSocket: FakeWebSocket
  });

  sockets[0].emit('open');
  assert.equal(sockets[0].url, 'ws://localhost:9090/ws/print?clientId=CLIENT-001');
  assert.equal(sockets[0].sent.length, 0);
});

test('reports websocket connection status changes', () => {
  const sockets = [];
  const statuses = [];

  class FakeWebSocket extends EventEmitter {
    constructor() {
      super();
      sockets.push(this);
    }

    send() {}
    close() {}
  }

  const client = startPrintClient({
    clientId: 'CLIENT-001',
    serverUrl: 'ws://localhost:9090/ws/print'
  }, {
    WebSocket: FakeWebSocket,
    logger: {
      warn: () => {}
    },
    onStatusChange: (status) => statuses.push(status)
  });

  sockets[0].emit('open');
  sockets[0].emit('error', { code: 'ECONNREFUSED' });

  assert.deepEqual(statuses.map((status) => status.state), [
    'connecting',
    'connected',
    'error'
  ]);
  assert.equal(client.getStatus().message, 'ECONNREFUSED');
});

test('keeps existing query parameters when adding clientId', () => {
  assert.equal(
    buildClientUrl('ws://localhost:9090/ws/print?token=abc', 'CLIENT-001'),
    'ws://localhost:9090/ws/print?token=abc&clientId=CLIENT-001'
  );
});

test('ignores server connected control message', () => {
  const sockets = [];
  let runCount = 0;

  class FakeWebSocket extends EventEmitter {
    constructor() {
      super();
      sockets.push(this);
    }

    send() {}
    close() {}
  }

  startPrintClient({
    clientId: 'CLIENT-001',
    serverUrl: 'ws://localhost:9090/ws/print'
  }, {
    WebSocket: FakeWebSocket,
    runPrintTask: () => {
      runCount += 1;
    }
  });

  sockets[0].emit('message', JSON.stringify({
    type: 'CONNECTED',
    clientId: 'CLIENT-001'
  }));

  assert.equal(runCount, 0);
});
