/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.source.formatter.check.util.TFSourceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alan Huang
 */
public class TFIndentationCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		String[] lines = content.split("\n", -1);

		boolean blockComment = false;
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

			if (!blockComment) {
				lines[i] = _getIndentedLine(trimmedLine, expectedTabCount);
			}

			StringBundler sb = new StringBundler();

			blockComment = _appendBrackets(trimmedLine, blockComment, sb);

			String brackets = sb.toString();

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

			if (blockComment) {
				continue;
			}

			String heredocMarker = TFSourceUtil.getHeredocMarker(trimmedLine);

			if (heredocMarker == null) {
				continue;
			}

			i = TFSourceUtil.getHeredocEndLineIndex(lines, i, heredocMarker);

			if (i == -1) {
				return content;
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

	private boolean _appendBrackets(
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
				i = TFSourceUtil.getStringEndIndex(line, i + 1);

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

	private String _getIndentedLine(String trimmedLine, int expectedTabCount) {
		StringBundler sb = new StringBundler(expectedTabCount + 1);

		for (int i = 0; i < expectedTabCount; i++) {
			sb.append(CharPool.TAB);
		}

		sb.append(trimmedLine);

		return sb.toString();
	}

	private static final String _CLOSE_BRACKETS = ")]}";

	private static final String _OPEN_BRACKETS = "([{";

}