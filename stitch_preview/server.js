const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { URL } = require('node:url');

const ROOT = __dirname;
const DEFAULT_STATE_PATH = path.join(ROOT, 'default-state.json');
const DATA_DIR = path.join(ROOT, 'data');
const STATE_PATH = path.join(DATA_DIR, 'state.json');
const PORT = Number(process.env.PORT || 4173);

const CONTENT_TYPES = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp'
};

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function ensureDataDir() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function defaultState() {
  return readJson(DEFAULT_STATE_PATH);
}

function normalizeState(input) {
  const defaults = defaultState();
  const value = input && typeof input === 'object' ? input : {};
  return {
    defaultScheduleDate: typeof value.defaultScheduleDate === 'string' ? value.defaultScheduleDate : defaults.defaultScheduleDate,
    periods: Array.isArray(value.periods) ? value.periods : defaults.periods,
    tasks: Array.isArray(value.tasks) ? value.tasks : defaults.tasks,
    reminders: Array.isArray(value.reminders) ? value.reminders : defaults.reminders,
    uiState: {
      hiddenTaskIds: Array.isArray(value.uiState?.hiddenTaskIds) ? value.uiState.hiddenTaskIds : [],
      hiddenReminderIds: Array.isArray(value.uiState?.hiddenReminderIds) ? value.uiState.hiddenReminderIds : [],
      snoozedReminders: value.uiState?.snoozedReminders && typeof value.uiState.snoozedReminders === 'object'
        ? value.uiState.snoozedReminders
        : {}
    }
  };
}

function readState() {
  ensureDataDir();
  if (!fs.existsSync(STATE_PATH)) {
    const next = normalizeState(defaultState());
    fs.writeFileSync(STATE_PATH, JSON.stringify(next, null, 2));
    return next;
  }
  return normalizeState(readJson(STATE_PATH));
}

function writeState(state) {
  ensureDataDir();
  const normalized = normalizeState(state);
  fs.writeFileSync(STATE_PATH, JSON.stringify(normalized, null, 2));
  return normalized;
}

function resetState() {
  return writeState(defaultState());
}

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store'
  });
  response.end(JSON.stringify(payload));
}

function sendText(response, statusCode, message) {
  response.writeHead(statusCode, {
    'content-type': 'text/plain; charset=utf-8',
    'cache-control': 'no-store'
  });
  response.end(message);
}

function readRequestBody(request) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    request.on('data', (chunk) => chunks.push(chunk));
    request.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8').trim();
      if (!raw) {
        resolve(null);
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch (error) {
        reject(error);
      }
    });
    request.on('error', reject);
  });
}

function safeStaticPath(pathname) {
  const decoded = decodeURIComponent(pathname);
  const relative = decoded === '/' ? '/index.html' : decoded;
  const filePath = path.join(ROOT, relative);
  if (!filePath.startsWith(ROOT)) return null;
  return filePath;
}

function serveStatic(response, pathname) {
  const filePath = safeStaticPath(pathname);
  if (!filePath || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    sendText(response, 404, 'Not found');
    return;
  }

  const ext = path.extname(filePath).toLowerCase();
  response.writeHead(200, {
    'content-type': CONTENT_TYPES[ext] || 'application/octet-stream',
    'cache-control': 'no-store'
  });
  fs.createReadStream(filePath).pipe(response);
}

const server = http.createServer(async (request, response) => {
  try {
    const url = new URL(request.url, `http://${request.headers.host}`);

    if (url.pathname === '/api/state') {
      if (request.method === 'GET') {
        sendJson(response, 200, readState());
        return;
      }

      if (request.method === 'PUT') {
        const body = await readRequestBody(request);
        sendJson(response, 200, writeState(body));
        return;
      }
    }

    if (url.pathname === '/api/reset' && request.method === 'POST') {
      sendJson(response, 200, resetState());
      return;
    }

    if (request.method !== 'GET' && request.method !== 'HEAD') {
      sendText(response, 404, 'Not found');
      return;
    }

    serveStatic(response, url.pathname);
  } catch (error) {
    sendJson(response, 500, {
      error: error instanceof Error ? error.message : 'Unknown server error'
    });
  }
});

server.listen(PORT, '127.0.0.1', () => {
  process.stdout.write(`Focus preview server running on http://127.0.0.1:${PORT}\n`);
});
