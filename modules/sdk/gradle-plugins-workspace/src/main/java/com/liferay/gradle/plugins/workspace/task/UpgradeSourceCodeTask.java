/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.liferay.gradle.plugins.source.formatter.FormatSourceTask;
import com.liferay.release.util.ReleaseEntry;
import com.liferay.release.util.ReleaseUtil;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.options.Option;

/**
 * @author Kyle Miho
 */
public class UpgradeSourceCodeTask extends FormatSourceTask {

	@Override
	public void exec() {
		String sourceFormatterPropertiesString = String.valueOf(
			getSourceFormatterProperties());

		if (!sourceFormatterPropertiesString.contains(
				"upgrade.to.release.version")) {

			throw new GradleException(
				"Please set the to-version property to set a valid Liferay " +
					"version");
		}

		super.exec();
	}

	@Option(
		description = "The version of Liferay to target when upgrading the source code.",
		option = "to-version"
	)
	public void setToVersion(String toVersion) {
		List<ReleaseEntry> releaseEntries = ReleaseUtil.getReleaseEntries();

		List<String> targetPlatformVersions = new ArrayList<>();

		for (ReleaseEntry releaseEntry : releaseEntries) {
			targetPlatformVersions.add(releaseEntry.getTargetPlatformVersion());
		}

		if (!targetPlatformVersions.contains(toVersion)) {
			throw new GradleException(
				toVersion +
					" is an invalid Liferay version to upgrade to. Please " +
						"use a valid Liferay version.");
		}

		addSourceFormatterProperty("upgrade.to.release.version", toVersion);
	}

}