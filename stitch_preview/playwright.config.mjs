import { defineConfig } from '@playwright/test';

const PORT = 4318;

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60000,
  use: {
    baseURL: `http://127.0.0.1:${PORT}`,
    browserName: 'chromium',
    channel: 'chrome',
    headless: true
  },
  webServer: {
    command: 'node stitch_preview/server.js',
    cwd: process.cwd(),
    env: {
      ...process.env,
      PORT: String(PORT)
    },
    port: PORT,
    reuseExistingServer: false,
    timeout: 60000
  }
});
