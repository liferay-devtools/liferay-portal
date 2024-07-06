/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.portal.json.JSONArrayImpl;
import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.NaturalOrderStringComparator;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Qi Zhang
 */
public class JSONBatchEngineDataFileCheck extends BaseFileCheck {

	@Override
	public boolean isLiferaySourceCheck() {
		return true;
	}

	@Override
	protected String doProcess(
			String fileName, String absolutePath, String content)
		throws JSONException {

		if (!absolutePath.endsWith(".batch-engine-data.json")) {
			return content;
		}

		JSONObject jsonObject = new JSONObjectImpl(content);

		jsonObject.remove("actions");
		jsonObject.remove("facets");

		JSONObject configurationJSONObject = jsonObject.getJSONObject(
			"configuration");

		if (configurationJSONObject != null) {
			configurationJSONObject.remove("companyId");

			boolean multiCompany = configurationJSONObject.getBoolean(
				"multiCompany");

			if (!multiCompany) {
				configurationJSONObject.remove("multiCompany");
			}

			configurationJSONObject.remove("userId");
			configurationJSONObject.remove("version");

			jsonObject.put("configuration", configurationJSONObject);
		}

		jsonObject = _sortByExternalReferenceCode(jsonObject, "items");

		return JSONUtil.toString(jsonObject);
	}

	private JSONObject _sortByExternalReferenceCode(
		JSONObject jsonObject, String name) {

		if (!jsonObject.has(name)) {
			return jsonObject;
		}

		JSONArray jsonArray = jsonObject.getJSONArray(name);

		List<Object> objectList = JSONUtil.toObjectList(jsonArray);

		Collections.sort(objectList, new BatchEngineDataComparator());

		jsonArray = new JSONArrayImpl();

		for (Object object : objectList) {
			JSONObject innerJSONObject = (JSONObject)object;

			innerJSONObject = _sortByExternalReferenceCode(
				innerJSONObject, "listTypeEntries");

			jsonArray.put(innerJSONObject);
		}

		jsonObject.put(name, jsonArray);

		return jsonObject;
	}

	private class BatchEngineDataComparator implements Comparator<Object> {

		@Override
		public int compare(Object object1, Object object2) {
			JSONObject jsonObject1 = (JSONObject)object1;
			JSONObject jsonObject2 = (JSONObject)object2;

			String externalReferenceCode1 = jsonObject1.getString(
				"externalReferenceCode");
			String externalReferenceCode2 = jsonObject2.getString(
				"externalReferenceCode");

			if (externalReferenceCode1.matches(
					"(?i)[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}") &&
				externalReferenceCode2.matches(
					"(?i)[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}")) {

				String hex1 = StringUtil.removeChar(
					externalReferenceCode1, CharPool.DASH);
				String hex2 = StringUtil.removeChar(
					externalReferenceCode2, CharPool.DASH);

				return hex1.compareToIgnoreCase(hex2);
			}

			NaturalOrderStringComparator comparator =
				new NaturalOrderStringComparator(true, true);

			return comparator.compare(
				externalReferenceCode1, externalReferenceCode2);
		}

	}

}