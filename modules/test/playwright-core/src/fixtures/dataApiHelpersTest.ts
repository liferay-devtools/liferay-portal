/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Page, mergeTests} from '@playwright/test';

import {ApiHelpers} from '../ApiHelpers';
import {trackPendingApiRequests} from '../utils/trackPendingApiRequests';
import {BackendPage, backendPageTest} from './backendPageTest';

const test = mergeTests(backendPageTest);

function dataApiHelpersTest<T extends ApiHelpers>(
	ApiHelpersClass: new (page: Page) => T
) {
	return test.extend<{
		apiHelpers: T;
		backendPage: BackendPage;
	}>({
		apiHelpers: async ({backendPage, page}, use) => {
			const dataApiHelpers = new ApiHelpersClass(page);

			const waitForPendingApiRequests = trackPendingApiRequests(page);

			try {
				await use(dataApiHelpers);
			}
			finally {
				await waitForPendingApiRequests();

				// @ts-ignore

				const adminDataApiHelpers = new ApiHelpersClass(backendPage);

				adminDataApiHelpers.setData(dataApiHelpers.data);

				await adminDataApiHelpers.clearData();
			}
		},
	});
}

export {dataApiHelpersTest};
