/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.validator;

import com.liferay.gradle.plugins.workspace.internal.client.extension.ClientExtension;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.HashMap;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import org.junit.Assert;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Anderson Luiz
 */
public class ClientExtensionValidatorTest {

	@Test
	public void testValidate() throws IOException {
		ClientExtension clientExtension = new ClientExtension();

		clientExtension.type = "themeCSS";

		Project project = Mockito.mock(Project.class);

		_clientExtensionValidator.validate(clientExtension, project);

		clientExtension.typeSettings = new HashMap<String, Object>() {
			{
				put(
					"frontendTokenDefinitionJSON",
					"frontend-token-definition.json");
			}
		};

		Mockito.when(
			project.file(Mockito.anyString())
		).thenReturn(
			new File("")
		);

		try {
			_clientExtensionValidator.validate(clientExtension, project);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String exceptionMessage = gradleException.getMessage();

			Assert.assertTrue(exceptionMessage.contains("Unable to find file"));
		}

		File file = File.createTempFile("frontend-token-definition", ".json");

		file.deleteOnExit();

		String invalidJSON = "{[/][i}";

		Files.write(file.toPath(), invalidJSON.getBytes());

		Mockito.when(
			project.file(Mockito.anyString())
		).thenReturn(
			file
		);

		try {
			_clientExtensionValidator.validate(clientExtension, project);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String exceptionMessage = gradleException.getMessage();

			Assert.assertTrue(
				exceptionMessage.contains("Unable to parse file"));
		}
	}

	private final ClientExtensionValidator _clientExtensionValidator =
		new ClientExtensionValidator();

}