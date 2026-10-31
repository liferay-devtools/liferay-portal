/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {liferayConfig} from '@liferay/playwright-core';
import {defineConfig, devices} from '@playwright/test';

const {baseUrl, port} = liferayConfig.environment;

export default defineConfig({
	expect: {
		timeout: 15 * 1000,
	},
	forbidOnly: !!process.env.CI,
	reporter: [['html', {open: 'never'}]],
	retries: process.env.CI ? 1 : 0,
	testDir: './tests',
	timeout: 90 * 1000,
	use: {
		...devices['Desktop Chrome'],
		baseURL: baseUrl,
		screenshot: 'only-on-failure',
		trace: 'retain-on-failure',
	},
	webServer: {
		command: './start_portal.sh',
		env: {LIFERAY_HTTP_PORT: port},
		gracefulShutdown: {signal: 'SIGTERM', timeout: 60 * 1000},
		reuseExistingServer: true,
		stdout: 'pipe',
		timeout: 20 * 60 * 1000,
		url: `${baseUrl}/c/portal/status`,
	},
	workers: 1,
});
