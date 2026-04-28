/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.internal.upgrade.v3_5_2;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Anthony Chu
 */
public class CETConfigurationUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		if (!hasTable("Configuration_")) {
			return;
		}

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				StringBundler.concat(
					"select configurationId from Configuration_ where ",
					"configurationId like 'com.liferay.client.extension.type.",
					"configuration.CETConfiguration~%' and dictionary not ",
					"like '%.client.extension.config.bundle.id=%'"));

			PreparedStatement preparedStatement =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection,
					"delete from Configuration_ where configurationId = ?")) {

			while (resultSet.next()) {
				String configurationId = resultSet.getString("configurationId");

				if (_log.isInfoEnabled()) {
					_log.info(
						"Deleting persisted CET configuration " +
							configurationId);
				}

				preparedStatement.setString(1, configurationId);

				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CETConfigurationUpgradeProcess.class);

}