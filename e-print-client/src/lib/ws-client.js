'use strict';

const { parseTaskMessage } = require('./task');
const { runPrintTask } = require('./print-runner');

function startPrintClient(config, dependencies) {
  const deps = dependencies || {};
  const WebSocketImpl = deps.WebSocket || require('ws');
  const reconnectDelayMs = deps.reconnectDelayMs || config.reconnectDelayMs || 5000;
  const logger = deps.logger || console;
  const timers = deps.timers || globalThis;
  const onStatusChange = typeof deps.onStatusChange === 'function'
    ? deps.onStatusChange
    : () => {};
  let socket;
  let stopped = false;
  let reconnectTimer = null;
  let status = {
    state: 'idle',
    serverUrl: config.serverUrl,
    message: 'Not connected',
    updatedAt: new Date().toISOString()
  };

  connect();

  return {
    get socket() {
      return socket;
    },
    getStatus() {
      return status;
    },
    stop() {
      stopped = true;
      setStatus('stopped', 'Connection stopped');
      if (reconnectTimer) {
        timers.clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      if (socket && typeof socket.close === 'function') {
        socket.close();
      }
    }
  };

  function connect() {
    setStatus('connecting', 'Connecting to e-print server');
    socket = new WebSocketImpl(buildClientUrl(config.serverUrl, config.clientId), buildWebSocketOptions(config));

    socket.on('open', () => {
      setStatus('connected', 'Connected');
    });

    socket.on('message', async (message) => {
      if (isControlMessage(message)) {
        return;
      }

      let task;
      try {
        task = parseTaskMessage(message.toString());
      } catch (error) {
        sendJson(socket, {
          type: 'client-error',
          clientId: config.clientId,
          message: error.message
        });
        return;
      }

      await (deps.runPrintTask || runPrintTask)(task, config, {
        ...deps,
        reportResult: (result) => sendJson(socket, result)
      });
    });

    socket.on('error', (error) => {
      const message = formatConnectionError(error);
      setStatus('error', message);
      if (logger && typeof logger.warn === 'function') {
        logger.warn(`e-print server connection failed (${config.serverUrl}): ${message}`);
      }
    });

    socket.on('close', () => {
      if (!stopped) {
        setStatus('disconnected', 'Disconnected, waiting to reconnect');
      }
      scheduleReconnect();
    });
  }

  function scheduleReconnect() {
    if (stopped || reconnectTimer) {
      return;
    }

    reconnectTimer = timers.setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, reconnectDelayMs);
  }

  function setStatus(state, message) {
    status = {
      state,
      serverUrl: config.serverUrl,
      message,
      updatedAt: new Date().toISOString()
    };
    onStatusChange(status);
  }
}

function buildWebSocketOptions(config) {
  const authorization = buildBasicAuthorization(config);
  if (!authorization) {
    return undefined;
  }

  return {
    headers: {
      Authorization: authorization
    }
  };
}

function buildBasicAuthorization(config) {
  if (!config || !config.basicUsername || !config.basicPassword) {
    return '';
  }

  return `Basic ${Buffer.from(`${config.basicUsername}:${config.basicPassword}`, 'utf8').toString('base64')}`;
}

function sendJson(socket, payload) {
  socket.send(JSON.stringify(payload));
}

function buildClientUrl(serverUrl, clientId) {
  const url = new URL(serverUrl);
  if (clientId) {
    url.searchParams.set('clientId', clientId);
  }
  return url.toString();
}

function isControlMessage(message) {
  try {
    const payload = JSON.parse(message.toString());
    return payload && (
      payload.type === 'CONNECTED' ||
      payload.type === 'connected' ||
      payload.type === 'client-connected'
    );
  } catch {
    return false;
  }
}

function formatConnectionError(error) {
  if (!error) {
    return 'unknown error';
  }

  if (typeof error.message === 'string' && error.message.trim()) {
    return error.message;
  }

  if (typeof error.code === 'string' && error.code.trim()) {
    return error.code;
  }

  if (typeof error.reason === 'string' && error.reason.trim()) {
    return error.reason;
  }

  if (typeof error.type === 'string' && error.type.trim()) {
    return error.type;
  }

  return 'unknown error';
}

module.exports = {
  buildClientUrl,
  buildBasicAuthorization,
  buildWebSocketOptions,
  formatConnectionError,
  isControlMessage,
  sendJson,
  startPrintClient
};
