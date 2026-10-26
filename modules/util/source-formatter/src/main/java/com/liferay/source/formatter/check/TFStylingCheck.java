/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.check.util.TFSourceUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alan Huang
 */
public class TFStylingCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		String[] lines = content.split("\n", -1);

		for (int i = 0; i < lines.length; i++) {
			String heredocMarker = TFSourceUtil.getHeredocMarker(lines[i]);

			if (heredocMarker == null) {
				lines[i] = _quoteBlockLabels(lines[i]);

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

	private String _quoteBlockLabels(String line) {
		String trimmedLine = StringUtil.trim(line);

		if (!trimmedLine.endsWith(StringPool.OPEN_CURLY_BRACE)) {
			return line;
		}

		trimmedLine = StringUtil.trim(
			trimmedLine.substring(0, trimmedLine.length() - 1));

		String[] parts = trimmedLine.split("\\s+");

		Matcher matcher = _blockTypePattern.matcher(parts[0]);

		if ((parts.length == 1) || (parts.length > 3) || !matcher.matches()) {
			return line;
		}

		StringBundler sb = new StringBundler();

		sb.append(line.substring(0, line.indexOf(trimmedLine)));
		sb.append(parts[0]);

		boolean quoted = false;

		for (int i = 1; i < parts.length; i++) {
			String label = parts[i];

			matcher = _labelPattern.matcher(label);

			if (!matcher.matches()) {
				return line;
			}

			sb.append(CharPool.SPACE);

			if (label.startsWith(StringPool.QUOTE)) {
				sb.append(label);

				continue;
			}

			sb.append(CharPool.QUOTE);
			sb.append(label);
			sb.append(CharPool.QUOTE);

			quoted = true;
		}

		if (!quoted) {
			return line;
		}

		sb.append(" {");

		return sb.toString();
	}

	private static final Pattern _blockTypePattern = Pattern.compile("[\\w-]+");
	private static final Pattern _labelPattern = Pattern.compile(
		"\"[^\"]*\"|[\\w-]+");

}