/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {loginTest} from '@liferay/playwright-core';
import {expect} from '@playwright/test';

const test = loginTest();

test(
	'home page renders for a signed in user',
	{tag: '@LPD-110761'},
	async ({page}) => {
		await page.goto('/');

		await expect(page).toHaveTitle(/Liferay/);
		await expect(page.getByLabel('Test Test')).toBeVisible();
	}
);
