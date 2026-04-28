/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.upgrade.v3_5_2.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeStep;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;

import java.util.Hashtable;
import java.util.Objects;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Anthony Chu
 */
@RunWith(Arquillian.class)
public class CETConfigurationCleanupUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@After
	public void tearDown() throws Exception {
		_deleteConfiguration(_STALE_CET_PID_1);
		_deleteConfiguration(_STALE_CET_PID_2);
		_deleteConfiguration(_TRACKER_CET_PID);
		_deleteConfiguration(_UNRELATED_PID);
	}

	@Test
	public void testUpgrade() throws Exception {
		_createConfiguration(_STALE_CET_PID_1, new Hashtable<>());
		_createConfiguration(_STALE_CET_PID_2, new Hashtable<>());

		Hashtable<String, Object> trackerProperties = new Hashtable<>();

		trackerProperties.put(_BUNDLE_ID_PROPERTY_KEY, 1L);

		_createConfiguration(_TRACKER_CET_PID, trackerProperties);

		_createConfiguration(_UNRELATED_PID, new Hashtable<>());

		Configuration[] cetConfigurationsBeforeUpgrade =
			_configurationAdmin.listConfigurations(
				StringBundler.concat(
					"(", Constants.SERVICE_PID, "=",
					_CET_CONFIGURATION_PID_PREFIX, "*)"));

		Assert.assertNotNull(
			"CET configurations must exist before upgrade",
			cetConfigurationsBeforeUpgrade);
		Assert.assertEquals(
			"Two stale and one tracker-installed CET configuration must " +
				"exist before upgrade",
			3, cetConfigurationsBeforeUpgrade.length);

		UpgradeProcess upgradeProcess = _getUpgradeProcess();

		Assert.assertNotNull(
			"CET configuration cleanup upgrade step must be registered",
			upgradeProcess);

		upgradeProcess.upgrade();

		Configuration[] cetConfigurationsAfterUpgrade =
			_configurationAdmin.listConfigurations(
				StringBundler.concat(
					"(", Constants.SERVICE_PID, "=",
					_CET_CONFIGURATION_PID_PREFIX, "*)"));

		Assert.assertNotNull(
			"Tracker-installed CET configuration must survive upgrade",
			cetConfigurationsAfterUpgrade);
		Assert.assertEquals(
			"Only the tracker-installed CET configuration should remain", 1,
			cetConfigurationsAfterUpgrade.length);
		Assert.assertEquals(
			_TRACKER_CET_PID, cetConfigurationsAfterUpgrade[0].getPid());

		Assert.assertNotNull(
			"Unrelated configuration should survive",
			_configurationAdmin.listConfigurations(
				StringBundler.concat(
					"(", Constants.SERVICE_PID, "=", _UNRELATED_PID, ")")));
	}

	private void _createConfiguration(
			String pid, Hashtable<String, Object> properties)
		throws Exception {

		Configuration configuration = _configurationAdmin.getConfiguration(
			pid, "?");

		configuration.update(properties);
	}

	private void _deleteConfiguration(String pid) throws Exception {
		Configuration[] configurations = _configurationAdmin.listConfigurations(
			StringBundler.concat("(", Constants.SERVICE_PID, "=", pid, ")"));

		if (configurations == null) {
			return;
		}

		for (Configuration configuration : configurations) {
			configuration.delete();
		}
	}

	private UpgradeProcess _getUpgradeProcess() {
		UpgradeProcess[] upgradeProcesses = new UpgradeProcess[1];

		_upgradeStepRegistrator.register(
			(fromSchemaVersionString, toSchemaVersionString, upgradeSteps) -> {
				if (!Objects.equals(fromSchemaVersionString, "3.5.1") ||
					!Objects.equals(toSchemaVersionString, "3.5.2")) {

					return;
				}

				for (UpgradeStep upgradeStep : upgradeSteps) {
					Class<?> upgradeStepClass = upgradeStep.getClass();

					if (Objects.equals(
							upgradeStepClass.getName(),
							_CLASS_NAME_CET_CONFIGURATION_UPGRADE_PROCESS)) {

						upgradeProcesses[0] = (UpgradeProcess)upgradeStep;

						return;
					}
				}
			});

		return upgradeProcesses[0];
	}

	private static final String _BUNDLE_ID_PROPERTY_KEY =
		".client.extension.config.bundle.id";

	private static final String _CET_CONFIGURATION_PID_PREFIX =
		"com.liferay.client.extension.type.configuration.CETConfiguration~";

	private static final String _CLASS_NAME_CET_CONFIGURATION_UPGRADE_PROCESS =
		"com.liferay.client.extension.internal.upgrade.v3_5_2." +
			"CETConfigurationUpgradeProcess";

	private static final String _STALE_CET_PID_1 =
		_CET_CONFIGURATION_PID_PREFIX + "upgrade-test-cet-1/liferay.com";

	private static final String _STALE_CET_PID_2 =
		_CET_CONFIGURATION_PID_PREFIX + "upgrade-test-cet-2/liferay.com";

	private static final String _TRACKER_CET_PID =
		_CET_CONFIGURATION_PID_PREFIX + "upgrade-test-tracker/liferay.com";

	private static final String _UNRELATED_PID =
		"com.liferay.client.extension.upgrade.test.unrelated";

	@Inject
	private ConfigurationAdmin _configurationAdmin;

	@Inject(
		filter = "component.name=com.liferay.client.extension.internal.upgrade.registry.ClientExtensionUpgradeStepRegistrator"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}