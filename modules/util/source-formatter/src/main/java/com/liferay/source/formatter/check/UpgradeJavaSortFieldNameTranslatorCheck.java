/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.source.formatter.parser.JavaClass;
import com.liferay.source.formatter.parser.JavaClassParser;
import com.liferay.source.formatter.parser.JavaTerm;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kyle Miho
 */
public class UpgradeJavaSortFieldNameTranslatorCheck extends BaseUpgradeCheck {

	@Override
	protected String format(
			String fileName, String absolutePath, String content)
		throws Exception {

		JavaClass javaClass = JavaClassParser.parseJavaClass(fileName, content);

		for (JavaTerm javaTerm : javaClass.getChildJavaTerms()) {
			if (Objects.equals(javaTerm.getName(), "getEntityClass")) {
				return content;
			}
		}

		for (String implementedClassName :
				javaClass.getImplementedClassNames()) {

			if (implementedClassName.equals("SortFieldNameTranslator")) {
				return _getNewContent(content);
			}
		}

		return content;
	}

	private String _getNewContent(String content) throws Exception {
		String method = _getNewMethod(content);

		return content.replaceFirst(
			"(public class .*?\\s*implements SortFieldNameTranslator \\{)",
			"$1\n\n" + method);
	}

	private String _getNewMethod(String content) throws Exception {
		Matcher matcher = _sortFieldNameClassPattern.matcher(content);

		if (matcher.find()) {
			String clazz = matcher.group(1);

			return _joinLines(
				"\t@Override", "\tpublic Class<?> getEntityClass() {",
				String.format("\t\treturn %s.class;", clazz), "\t}");
		}

		throw new Exception(
			"Unable to find class that implements SortFieldNameTranslator");
	}

	private String _joinLines(String... lines) {
		StringBundler sb = new StringBundler((lines.length * 2) - 1);

		for (String line : lines) {
			if (sb.index() > 0) {
				sb.append(StringPool.NEW_LINE);
			}

			sb.append(line);
		}

		return sb.toString();
	}

	private static final Pattern _sortFieldNameClassPattern = Pattern.compile(
		"public class (\\w+)SortFieldNameTranslator.*?" +
			"\\s*implements SortFieldNameTranslator");

}