/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check.util;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alan Huang
 */
public class TFSourceUtil {

	public static boolean appendBrackets(
		String line, boolean blockComment, StringBundler sb) {

		int i = 0;

		if (blockComment) {
			i = line.indexOf("*/");

			if (i == -1) {
				return true;
			}

			i += 2;
		}

		while (i < line.length()) {
			char c = line.charAt(i);

			if (c == CharPool.QUOTE) {
				i = getStringEndIndex(line, i + 1);

				continue;
			}

			if ((c == CharPool.POUND) || line.startsWith("//", i)) {
				return false;
			}

			if (line.startsWith("/*", i)) {
				i = line.indexOf("*/", i + 2);

				if (i == -1) {
					return true;
				}

				i += 2;

				continue;
			}

			if ((_CLOSE_BRACKETS.indexOf(c) != -1) ||
				(_OPEN_BRACKETS.indexOf(c) != -1)) {

				sb.append(c);
			}

			i++;
		}

		return false;
	}

	public static int getBlockEndLineIndex(
		String[] lines, int startLineIndex, int level) {

		boolean blockComment = false;

		for (int i = startLineIndex; i < lines.length; i++) {
			StringBundler sb = new StringBundler();

			blockComment = appendBrackets(lines[i], blockComment, sb);

			String brackets = sb.toString();

			for (char c : brackets.toCharArray()) {
				if (_OPEN_BRACKETS.indexOf(c) != -1) {
					level++;
				}
				else {
					level--;
				}

				if (level < 0) {
					return -1;
				}
			}

			if (blockComment) {
				continue;
			}

			String heredocMarker = getHeredocMarker(lines[i]);

			if (heredocMarker != null) {
				i = getHeredocEndLineIndex(lines, i, heredocMarker);

				if (i == -1) {
					return -1;
				}
			}

			if (level == 0) {
				return i;
			}
		}

		return -1;
	}

	public static int getHeredocEndLineIndex(
		String[] lines, int lineIndex, String heredocMarker) {

		for (int i = lineIndex + 1; i < lines.length; i++) {
			if (heredocMarker.equals(StringUtil.trim(lines[i]))) {
				return i;
			}
		}

		return -1;
	}

	public static String getHeredocMarker(String line) {
		Matcher matcher = _heredocPattern.matcher(
			StringUtil.trim(removeComment(line)));

		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}

	public static int getStringEndIndex(String line, int index) {
		while (index < line.length()) {
			char c = line.charAt(index);

			if (c == CharPool.BACK_SLASH) {
				index += 2;

				continue;
			}

			if (c == CharPool.QUOTE) {
				return index + 1;
			}

			if (line.startsWith("$${", index) ||
				line.startsWith("%%{", index)) {

				index += 3;

				continue;
			}

			if (line.startsWith("${", index) || line.startsWith("%{", index)) {
				index = _getTemplateEndIndex(line, index + 2);

				continue;
			}

			index++;
		}

		return Math.min(index, line.length());
	}

	public static String removeComment(String line) {
		int i = 0;

		while (i < line.length()) {
			char c = line.charAt(i);

			if (c == CharPool.QUOTE) {
				i = getStringEndIndex(line, i + 1);

				continue;
			}

			if ((c == CharPool.POUND) || line.startsWith("//", i) ||
				line.startsWith("/*", i)) {

				return line.substring(0, i);
			}

			i++;
		}

		return line;
	}

	private static int _getTemplateEndIndex(String line, int index) {
		int level = 0;

		while (index < line.length()) {
			char c = line.charAt(index);

			if (c == CharPool.QUOTE) {
				index = getStringEndIndex(line, index + 1);

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
		"<<-?([A-Za-z_][\\w-]*)$");

}