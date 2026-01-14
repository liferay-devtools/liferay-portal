/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.liferay.gradle.plugins.source.formatter.FormatSourceTask;

import org.gradle.api.tasks.options.Option;

/**
 * @author Kyle Miho
 */
public class UpgradeSourceCodeTask extends FormatSourceTask {

	@Option(
		description = "Configures the liferay version to upgrade to",
		option = "upgrade-to-liferay-version"
	)
	public void setUpgradeToReleaseVersion(String upgradeToLiferayVersion) {
		addSourceFormatterProperty(
			"upgrade.to.release.version=" + upgradeToLiferayVersion);
	}

}