/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Page} from '@playwright/test';

import {clearAuthToken, getCSRFTokenHeader, getHeader} from './authToken';
import {liferayConfig} from './liferayConfig';

export type TDataApiHelpersData = {
	applicationName?: string;
	id: any;
	type: string;
};

export interface RequestOptions<T> {
	data?: T;
	failOnStatusCode?: boolean;
	headers?: {[key: string]: string};
	multipart?: {[key: string]: any};
}

export class ApiHelpers {
	readonly baseUrl: string;
	readonly data: TDataApiHelpersData[];
	readonly page: Page;

	protected static readonly _authorization = `Basic ${btoa(
		`test@liferay.com:test`
	)}`;

	constructor(page: Page, baseUrl?: string) {
		this.baseUrl = baseUrl
			? baseUrl + '/o/'
			: liferayConfig.environment.baseUrl + '/o/';
		this.data = [];
		this.page = page;
	}

	async clearData() {
		const failures: string[] = [];

		for await (const item of this.data.reverse()) {
			try {
				await this.deleteData(item);
			}
			catch (error) {
				failures.push(`${item.type} ${item.id}: ${error.message}`);
			}
		}

		if (failures.length) {
			console.warn(
				`Unable to delete ${failures.length} created entities:\n${failures.join('\n')}`
			);
		}
	}

	async delete<T>(
		url: string,
		{data, failOnStatusCode, headers}: RequestOptions<T> = {}
	) {
		return this._sendRequest(
			'delete',
			url,
			{data, failOnStatusCode: failOnStatusCode || false},
			undefined,
			headers
		);
	}

	async get(
		url: string,
		failOnStatusCode?: boolean,
		headers?: {[key: string]: string}
	) {
		const response = await this.getResponse(url, failOnStatusCode, headers);

		return response.json();
	}

	getAuthorizationHeader() {
		return ApiHelpers._authorization;
	}

	async getCSRFTokenHeader() {
		return getCSRFTokenHeader(this.page);
	}

	async getResponse(
		url: string,
		failOnStatusCode?: boolean,
		headers?: {[key: string]: string}
	) {
		return await this._sendRequest(
			'get',
			url,
			{failOnStatusCode: failOnStatusCode || false},
			headers
		);
	}

	async patch<T>(url: string, data: T) {
		const response = await this._sendRequest('patch', url, {data});

		const text = await response.text();

		if (!text) {
			return response;
		}

		return response.json();
	}

	async post<T>(url: string, options: RequestOptions<T> = {}) {
		const response = await this.postResponse(url, options);
		const status = response.status();

		if (!response.ok()) {
			const error = await response.text();

			throw new Error(
				`POST request to ${url} failed with code ${status}:\n\n${error}`
			);
		}

		if (status === 204) {
			return;
		}

		return response.json();
	}

	async postResponse<T>(
		url: string,
		{data, failOnStatusCode, headers, multipart}: RequestOptions<T> = {}
	) {
		return await this._sendRequest(
			'post',
			url,
			{data, failOnStatusCode: failOnStatusCode || false, multipart},
			headers
		);
	}

	async put<T>(url: string, options: RequestOptions<T> = {}) {
		const response = await this.putResponse(url, options);

		if (response.status() === 204) {
			return;
		}

		return response.json();
	}

	async putResponse<T>(
		url: string,
		{data, failOnStatusCode, headers, multipart}: RequestOptions<T> = {}
	) {
		return await this._sendRequest(
			'put',
			url,
			{data, failOnStatusCode: failOnStatusCode || false, multipart},
			headers
		);
	}

	setData(data: TDataApiHelpersData[]) {
		this.data.length = 0;

		while (data.length) {
			this.data.unshift(data.pop());
		}
	}

	protected async deleteData(_item: TDataApiHelpersData) {}

	protected async _sendRequest(
		method: 'delete' | 'get' | 'patch' | 'post' | 'put',
		url: string,
		options: {[key: string]: unknown} = {},
		headers?: {[key: string]: string},
		extraHeaders?: {[key: string]: string}
	) {
		const buildHeaders = async () =>
			headers || {
				...(await getHeader(this.page)),
				...(extraHeaders || {}),
			};

		const response = await this.page.request[method](url, {
			...options,
			headers: await buildHeaders(),
		});

		if (headers || response.status() !== 403) {
			return response;
		}

		clearAuthToken(this.page);

		return await this.page.request[method](url, {
			...options,
			headers: await buildHeaders(),
		});
	}
}
