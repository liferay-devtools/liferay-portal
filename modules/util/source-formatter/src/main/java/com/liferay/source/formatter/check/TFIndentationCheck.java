/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alan Huang
 */
public class TFIndentationCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		String[] lines = content.split("\n", -1);

		List<Integer> openIndentLevels = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			String trimmedLine = StringUtil.trim(lines[i]);

			if (Validator.isNull(trimmedLine)) {
				continue;
			}

			int expectedTabCount = 0;

			if (!openIndentLevels.isEmpty()) {
				expectedTabCount = openIndentLevels.get(
					openIndentLevels.size() - 1);

				if (_CLOSE_BRACKETS.indexOf(trimmedLine.charAt(0)) == -1) {
					expectedTabCount++;
				}
			}

			lines[i] = _getIndentedLine(trimmedLine, expectedTabCount);

			String brackets = _getBrackets(trimmedLine);

			for (char c : brackets.toCharArray()) {
				if (_OPEN_BRACKETS.indexOf(c) != -1) {
					openIndentLevels.add(expectedTabCount);
				}
				else if (openIndentLevels.isEmpty()) {
					return content;
				}
				else {
					openIndentLevels.remove(openIndentLevels.size() - 1);
				}
			}

			Matcher matcher = _heredocPattern.matcher(trimmedLine);

			if (!matcher.find()) {
				continue;
			}

			String heredocMarker = matcher.group(1);

			while (true) {
				i++;

				if (i == lines.length) {
					return content;
				}

				if (heredocMarker.equals(StringUtil.trim(lines[i]))) {
					break;
				}
			}
		}

		StringBundler sb = new StringBundler(lines.length * 2);

		for (String line : lines) {
			sb.append(line);
			sb.append(CharPool.NEW_LINE);
		}

		sb.setIndex(sb.index() - 1);

		return sb.toString();
	}

	private String _getBrackets(String line) {
		StringBundler sb = new StringBundler();

		int i = 0;

		while (i < line.length()) {
			char c = line.charAt(i);

			if (c == CharPool.QUOTE) {
				i = _getStringEndIndex(line, i + 1);

				continue;
			}

			if ((c == CharPool.POUND) || line.startsWith("//", i)) {
				break;
			}

			if ((_CLOSE_BRACKETS.indexOf(c) != -1) ||
				(_OPEN_BRACKETS.indexOf(c) != -1)) {

				sb.append(c);
			}

			i++;
		}

		return sb.toString();
	}

	private String _getIndentedLine(String trimmedLine, int expectedTabCount) {
		StringBundler sb = new StringBundler(expectedTabCount + 1);

		for (int i = 0; i < expectedTabCount; i++) {
			sb.append(CharPool.TAB);
		}

		sb.append(trimmedLine);

		return sb.toString();
	}

	private int _getStringEndIndex(String line, int index) {
		while (index < line.length()) {
			char c = line.charAt(index);

			if (c == CharPool.BACK_SLASH) {
				index += 2;

				continue;
			}

			if (c == CharPool.QUOTE) {
				return index + 1;
			}

			if (line.startsWith("${", index) || line.startsWith("%{", index)) {
				index = _getTemplateEndIndex(line, index + 2);

				continue;
			}

			index++;
		}

		return index;
	}

	private int _getTemplateEndIndex(String line, int index) {
		int level = 0;

		while (index < line.length()) {
			char c = line.charAt(index);

			if (c == CharPool.QUOTE) {
				index = _getStringEndIndex(line, index + 1);

				continue;
			}

			if (c == CharPool.OPEN_CURLY_BRACE) {
				level++;
			}
			else if (c == CharPool.CLOSE_CURLY_BRACE) {
				if (level == 0) {
					return index + 1;
				}

				level--;
			}

			index++;
		}

		return index;
	}

	private static final String _CLOSE_BRACKETS = ")]}";

	private static final String _OPEN_BRACKETS = "([{";

	private static final Pattern _heredocPattern = Pattern.compile(
		"<<-?(\\w+)$");

}