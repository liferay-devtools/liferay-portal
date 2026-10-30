/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export {ApiHelpers} from './ApiHelpers';
export type {RequestOptions, TDataApiHelpersData} from './ApiHelpers';
export {FeatureFlagApiHelper} from './FeatureFlagApiHelper';
export {clearAuthToken, getHeader, readAuthToken} from './authToken';
export type {ContentType} from './authToken';
export {backendPageTest} from './fixtures/backendPageTest';
export type {BackendPage} from './fixtures/backendPageTest';
export {dataApiHelpersTest} from './fixtures/dataApiHelpersTest';
export {featureFlagsTest} from './fixtures/featureFlagsTest';
export type {
	FeatureFlag,
	FeatureFlags,
	FeatureFlagsOptions,
} from './fixtures/featureFlagsTest';
export {loginTest} from './fixtures/loginTest';
export type {Login, LoginOptions} from './fixtures/loginTest';
export {liferayConfig} from './liferayConfig';
export {
	performLogin,
	performLoginViaApi,
	performLogout,
	performUserSwitch,
	performUserSwitchViaApi,
	userData,
} from './login';
export type {LoginScreenName} from './login';
export {clickAndExpectToBeHidden} from './utils/clickAndExpectToBeHidden';
export {clickAndExpectToBeVisible} from './utils/clickAndExpectToBeVisible';
export {getRandomInt} from './utils/getRandomInt';
export {default as getRandomString} from './utils/getRandomString';
export {hoverAndExpectToBeVisible} from './utils/hoverAndExpectToBeVisible';
export {trackPendingApiRequests} from './utils/trackPendingApiRequests';
export {waitForAlert} from './utils/waitForAlert';
export {waitForPageToBeLoaded} from './utils/waitForPageToBeLoaded';
export {waitForSPAToBeLoaded} from './utils/waitForSPAToBeLoaded';
