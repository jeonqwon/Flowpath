import { test, expect } from '@playwright/test';

test.beforeEach(async ({ request }) => {
  const response = await request.post('/api/reset');
  expect(response.ok()).toBeTruthy();
});

test('creating a task updates backend state and appears in tasks and planner', async ({ page, request }) => {
  await page.goto('/create_task.html?mode=task&from=tasks.html');
  await page.waitForFunction(() => document.getElementById('screenTitle')?.textContent.includes('Create Task'));
  await page.locator('#titleInput').fill('Server-backed task');
  await page.locator('#notesInput').fill('Created from the browser flow.');
  await page.getByRole('button', { name: 'Schedule Task' }).click();

  await expect(page).toHaveURL(/tasks\.html/);

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.tasks.some((task) => task.title === 'Server-backed task')).toBeTruthy();

  await page.goto('/planner.html');
  await expect(page.getByText('Server-backed task')).toBeVisible();
});

test('creating a task with a custom duration persists duration and end time', async ({ page, request }) => {
  await page.goto('/create_task.html?mode=task&from=tasks.html');
  await page.waitForFunction(() => document.getElementById('screenTitle')?.textContent.includes('Create Task'));
  await page.locator('#titleInput').fill('Long focus task');
  await page.locator('#durationHours').selectOption('2');
  await page.locator('#durationMinutes').selectOption('0');
  await page.getByRole('button', { name: 'Schedule Task' }).click();

  await expect(page).toHaveURL(/tasks\.html/);

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  const createdTask = state.tasks.find((task) => task.title === 'Long focus task');
  expect(createdTask).toBeTruthy();
  expect(createdTask.durationMinutes).toBe(120);
  expect(createdTask.preferredPeriod).toBe('Night');
  expect(createdTask.start).toBe('20:00');
  expect(createdTask.end).toBe('22:00');
});

test('creating a task with a selected preferred period persists the dropdown choice', async ({ page, request }) => {
  await page.goto('/create_task.html?mode=task&from=tasks.html');
  await page.waitForFunction(() => document.getElementById('screenTitle')?.textContent.includes('Create Task'));
  await page.locator('#titleInput').fill('Morning review task');
  await page.locator('#preferredPeriodButton').click();
  await page.getByRole('button', { name: /Morning/i }).click();
  await page.getByRole('button', { name: 'Schedule Task' }).click();

  await expect(page).toHaveURL(/tasks\.html/);

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  const createdTask = state.tasks.find((task) => task.title === 'Morning review task');
  expect(createdTask).toBeTruthy();
  expect(createdTask.preferredPeriod).toBe('Morning');
  expect(createdTask.start).toBe('08:00');
});

test('creating a reminder in reminder mode updates backend state and reminders page', async ({ page, request }) => {
  await page.goto('/create_task.html?mode=reminder&from=reminders.html');
  await page.waitForFunction(() => document.getElementById('screenTitle')?.textContent.includes('Create Reminder'));
  await page.locator('#titleInput').fill('Backend reminder');
  await page.getByRole('button', { name: 'Save Reminder' }).click();

  await expect(page).toHaveURL(/reminders\.html/);

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.reminders.some((reminder) => reminder.title === 'Backend reminder')).toBeTruthy();

  await expect(page.getByText('Backend reminder')).toBeVisible();
});

test('adding a life period updates backend state and the tasks timeline', async ({ page, request }) => {
  await page.goto('/add_new_period_dynamic.html?start=18:00&end=18:30');
  await page.locator('#periodLabel').fill('Tea Break');
  await page.getByRole('button', { name: 'Life' }).click();
  await page.getByRole('button', { name: 'Save Period' }).click();

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.periods.some((period) => period.name === 'Tea Break' && period.type === 'life')).toBeTruthy();

  await page.goto('/tasks.html');
  const teaBreak = page.getByText('Tea Break');
  await teaBreak.scrollIntoViewIfNeeded();
  await expect(teaBreak).toBeVisible();
});

test('editing an existing life period updates backend state and the tasks timeline', async ({ page, request }) => {
  await page.goto('/add_new_period_dynamic.html?id=p2&name=Lunch%20Break&type=life&start=12:00&end=13:00');
  await page.locator('#periodLabel').fill('Brunch');
  await page.getByRole('button', { name: 'Save Period' }).click();

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.periods.some((period) => period.id === 'p2' && period.name === 'Brunch' && period.type === 'life')).toBeTruthy();

  await page.goto('/tasks.html');
  await expect(page.getByText('Brunch')).toBeVisible();
  await expect(page.getByText('Lunch Break')).toHaveCount(0);
});

test('deleting an existing life period removes it from backend state and the tasks timeline', async ({ page, request }) => {
  await page.goto('/combined_settings_edit.html');
  await page.getByRole('button', { name: 'Edit Flow' }).click();
  const lunchCard = page.getByText('Lunch Break').locator('..').locator('..');
  await lunchCard.getByRole('button', { name: 'Delete' }).click();

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.periods.some((period) => period.id === 'p2')).toBeFalsy();

  await page.goto('/tasks.html');
  await expect(page.getByText('Lunch Break')).toHaveCount(0);
});

test('task detail can create a linked reminder and persist it to the backend', async ({ page, request }) => {
  await page.goto('/create_task.html?mode=task&from=tasks.html');
  await page.waitForFunction(() => document.getElementById('screenTitle')?.textContent.includes('Create Task'));
  await page.locator('#titleInput').fill('Detail reminder task');
  await page.getByRole('button', { name: 'Schedule Task' }).click();
  await expect(page).toHaveURL(/tasks\.html/);

  await page.getByText('Detail reminder task').click();
  await expect(page).toHaveURL(/task_detail\.html/);
  await page.getByRole('button', { name: 'Make Reminder' }).click();
  await expect(page.getByRole('button', { name: 'Reminder Added' })).toBeVisible();

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  const createdTask = state.tasks.find((task) => task.title === 'Detail reminder task');
  expect(createdTask).toBeTruthy();
  expect(state.reminders.some((reminder) => reminder.linkedTaskId === createdTask.id)).toBeTruthy();
});

test('marking a task done hides the task and its linked reminder', async ({ page, request }) => {
  await page.goto('/task_detail.html?id=t2&from=tasks.html');
  await page.getByRole('button', { name: 'Done' }).click();
  await expect(page).toHaveURL(/tasks\.html/);

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.uiState.hiddenTaskIds).toContain('t2');
  expect(state.uiState.hiddenReminderIds).toContain('r2');

  await page.goto('/tasks.html');
  await expect(page.getByText('Midday Focus Task')).toHaveCount(0);
  await page.goto('/reminders.html');
  await expect(page.getByText('Prep focus block')).toHaveCount(0);
});

test('snoozing a reminder updates backend state and keeps it visible as upcoming', async ({ page, request }) => {
  await page.goto('/reminders.html');
  await page.getByRole('button', { name: 'Snooze' }).first().click();

  const stateResponse = await request.get('/api/state');
  const state = await stateResponse.json();
  expect(state.uiState.snoozedReminders.r1?.date).toBe('2026-10-07');

  await expect(page.getByText('Slides follow-up')).toBeVisible();
});
