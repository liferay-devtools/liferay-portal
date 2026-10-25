/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.source.formatter.check.util.TFSourceUtil;

import java.util.ArrayList;
import java.util.List;
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

		List<String> newLines = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			String heredocMarker = TFSourceUtil.getHeredocMarker(lines[i]);

			if (heredocMarker != null) {
				int heredocEndLineIndex = TFSourceUtil.getHeredocEndLineIndex(
					lines, i, heredocMarker);

				if (heredocEndLineIndex == -1) {
					return content;
				}

				for (int j = i; j <= heredocEndLineIndex; j++) {
					newLines.add(lines[j]);
				}

				i = heredocEndLineIndex;

				continue;
			}

			if (Validator.isNull(StringUtil.trim(lines[i])) &&
				(i < (lines.length - 1))) {

				continue;
			}

			newLines.add(_quoteBlockLabels(lines[i]));
		}

		StringBundler sb = new StringBundler(newLines.size() * 2);

		for (String line : newLines) {
			sb.append(line);
			sb.append(CharPool.NEW_LINE);
		}

		sb.setIndex(sb.index() - 1);

		return sb.toString();
	}

	private String _quoteBlockLabels(String line) {
		Matcher matcher = _blockHeaderPattern.matcher(line);

		if (!matcher.matches()) {
			return line;
		}

		StringBundler sb = new StringBundler();

		sb.append(matcher.group(1));

		boolean quoted = false;

		Matcher labelMatcher = _labelPattern.matcher(matcher.group(2));

		while (labelMatcher.find()) {
			String label = labelMatcher.group();

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

	private static final Pattern _blockHeaderPattern = Pattern.compile(
		"(\\s*[\\w-]+)((\\s+(\"[^\"]*\"|[\\w-]+))+)\\s*\\{");
	private static final Pattern _labelPattern = Pattern.compile(
		"\"[^\"]*\"|[\\w-]+");

}