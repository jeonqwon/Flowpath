import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';

const PORT = 4317;
const BASE_URL = `http://127.0.0.1:${PORT}`;

async function startServer() {
  const child = spawn(process.execPath, ['stitch_preview/server.js'], {
    cwd: process.cwd(),
    env: { ...process.env, PORT: String(PORT) },
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let stderr = '';
  child.stderr.on('data', (chunk) => {
    stderr += String(chunk);
  });

  for (let i = 0; i < 20; i += 1) {
    if (child.exitCode !== null) break;
    try {
      const response = await fetch(`${BASE_URL}/api/state`);
      if (response.ok) return { child, stderrRef: () => stderr };
    } catch {}
    await delay(150);
  }

  throw new Error(`Server did not start. stderr: ${stderr}`);
}

test('backend state API resets and persists tasks and reminders', async (t) => {
  const { child, stderrRef } = await startServer();
  t.after(() => {
    child.kill();
  });

  let response = await fetch(`${BASE_URL}/api/reset`, { method: 'POST' });
  assert.equal(response.status, 200, stderrRef());

  response = await fetch(`${BASE_URL}/api/state`);
  assert.equal(response.status, 200, stderrRef());
  const initialState = await response.json();
  assert.ok(Array.isArray(initialState.periods));
  assert.ok(Array.isArray(initialState.tasks));
  assert.ok(Array.isArray(initialState.reminders));

  const nextState = {
    ...initialState,
    tasks: [...initialState.tasks, {
      id: 't-test',
      title: 'Backend task',
      description: 'Created through API test',
      priority: 'normal',
      dueDate: '2026-10-08',
      dueTime: '11:00',
      preferredPeriod: 'Morning',
      scheduleDate: '2026-10-08',
      repeatMode: 'none',
      repeatDays: [],
      repeatStartDate: '2026-10-08',
      start: '09:00',
      end: '10:00'
    }],
    reminders: [...initialState.reminders, {
      id: 'r-test',
      title: 'Backend reminder',
      description: '',
      date: '2026-10-08',
      time: '11:30',
      repeatMode: 'none',
      repeatDays: [],
      repeatStartDate: '2026-10-08',
      linkedTaskId: 't-test'
    }]
  };

  response = await fetch(`${BASE_URL}/api/state`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(nextState)
  });
  assert.equal(response.status, 200, stderrRef());

  response = await fetch(`${BASE_URL}/api/state`);
  const persistedState = await response.json();
  assert.ok(persistedState.tasks.some((task) => task.id === 't-test'));
  assert.ok(persistedState.reminders.some((reminder) => reminder.id === 'r-test'));
});
