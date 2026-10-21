/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.NaturalOrderStringComparator;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alan Huang
 */
public class TFBlockOrderCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
		String fileName, String absolutePath, String content) {

		content = _sortBlocks(content);

		return _sortEntries(content);
	}

	private List<Entry> _getEntries(String[] lines, int openLineIndex) {
		List<Entry> entries = new ArrayList<>();

		String childIndent = null;
		Entry entry = null;
		int group = 0;
		String openIndent = _getIndent(lines[openLineIndex]);

		for (int i = openLineIndex + 1; i < lines.length; i++) {
			String trimmedLine = StringUtil.trim(lines[i]);

			if (Validator.isNull(trimmedLine)) {
				entry = null;
				group++;

				continue;
			}

			String indent = _getIndent(lines[i]);

			if (indent.equals(openIndent) && _isCloseLine(trimmedLine)) {
				return entries;
			}

			if (childIndent == null) {
				if (indent.length() <= openIndent.length()) {
					return null;
				}

				childIndent = indent;
			}

			if (indent.equals(childIndent) && !_isCloseLine(trimmedLine)) {
				entry = new Entry(group, i, trimmedLine);

				entries.add(entry);
			}
			else if ((entry == null) ||
					 (indent.length() < childIndent.length())) {

				return null;
			}

			String heredocMarker = _getHeredocMarker(trimmedLine);

			if (heredocMarker != null) {
				i = _getHeredocEndLineIndex(lines, i, heredocMarker);

				if (i == -1) {
					return null;
				}
			}

			entry.setEndLineIndex(i);
		}

		return null;
	}

	private int _getHeredocEndLineIndex(
		String[] lines, int lineIndex, String heredocMarker) {

		for (int i = lineIndex + 1; i < lines.length; i++) {
			if (heredocMarker.equals(StringUtil.trim(lines[i]))) {
				return i;
			}
		}

		return -1;
	}

	private String _getHeredocMarker(String trimmedLine) {
		Matcher matcher = _heredocPattern.matcher(trimmedLine);

		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}

	private String _getIndent(String line) {
		Matcher matcher = _indentPattern.matcher(line);

		matcher.find();

		return matcher.group();
	}

	private String _getKey(String trimmedLine) {
		Matcher matcher = _keyPattern.matcher(trimmedLine);

		if (matcher.find()) {
			return StringUtil.removeChars(matcher.group(1), CharPool.QUOTE);
		}

		matcher = _nestedBlockPattern.matcher(trimmedLine);

		if (matcher.matches()) {
			return StringUtil.removeChars(matcher.group(1), CharPool.QUOTE);
		}

		return null;
	}

	private String _getSortKey(Entry entry, boolean list) {
		if (!list) {
			return _getKey(entry.getFirstLine());
		}

		if (entry.getStartLineIndex() != entry.getEndLineIndex()) {
			return null;
		}

		Matcher matcher = _stringValuePattern.matcher(entry.getFirstLine());

		if (matcher.matches()) {
			return matcher.group(1);
		}

		return null;
	}

	private boolean _isCloseLine(String trimmedLine) {
		char c = trimmedLine.charAt(0);

		if ((c == CharPool.CLOSE_BRACKET) ||
			(c == CharPool.CLOSE_CURLY_BRACE) ||
			(c == CharPool.CLOSE_PARENTHESIS)) {

			return true;
		}

		return false;
	}

	private String _sortBlocks(String content) {
		String[] lines = content.split("\n", -1);

		List<Entry> blocks = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			Matcher matcher = _blockPattern.matcher(lines[i]);

			if (!matcher.matches()) {
				continue;
			}

			Entry block = new Entry(0, i, lines[i]);

			for (i = i + 1; i < lines.length; i++) {
				String heredocMarker = _getHeredocMarker(
					StringUtil.trim(lines[i]));

				if (heredocMarker != null) {
					i = _getHeredocEndLineIndex(lines, i, heredocMarker);

					if (i == -1) {
						return content;
					}
				}
				else if (lines[i].equals(StringPool.CLOSE_CURLY_BRACE)) {
					break;
				}
			}

			if (i == lines.length) {
				return content;
			}

			block.setEndLineIndex(i);

			blocks.add(block);
		}

		NaturalOrderStringComparator naturalOrderStringComparator =
			new NaturalOrderStringComparator();

		for (int i = 1; i < blocks.size(); i++) {
			Entry block1 = blocks.get(i - 1);
			Entry block2 = blocks.get(i);

			int value = naturalOrderStringComparator.compare(
				_getKey(block1.getFirstLine()), _getKey(block2.getFirstLine()));

			if (value > 0) {
				return _swapEntries(lines, block1, block2);
			}
		}

		return content;
	}

	private String _sortEntries(String content) {
		String[] lines = content.split("\n", -1);

		for (int i = 0; i < lines.length; i++) {
			String trimmedLine = StringUtil.trim(lines[i]);

			String heredocMarker = _getHeredocMarker(trimmedLine);

			if (heredocMarker != null) {
				int heredocEndLineIndex = _getHeredocEndLineIndex(
					lines, i, heredocMarker);

				if (heredocEndLineIndex == -1) {
					return content;
				}

				String nextTrimmedLine = StringUtil.trim(lines[i + 1]);

				if (!nextTrimmedLine.equals(StringPool.OPEN_BRACKET) &&
					!nextTrimmedLine.equals(StringPool.OPEN_CURLY_BRACE)) {

					i = heredocEndLineIndex;
				}

				continue;
			}

			boolean list = false;

			if (trimmedLine.endsWith(StringPool.OPEN_BRACKET)) {
				if (!ArrayUtil.contains(
						_SORTABLE_LIST_KEYS, _getKey(trimmedLine))) {

					continue;
				}

				list = true;
			}
			else if (!trimmedLine.endsWith(StringPool.OPEN_CURLY_BRACE)) {
				continue;
			}

			List<Entry> entries = _getEntries(lines, i);

			if (entries == null) {
				continue;
			}

			String newContent = _sortEntries(lines, entries, list);

			if (newContent != null) {
				return newContent;
			}
		}

		return content;
	}

	private String _sortEntries(
		String[] lines, List<Entry> entries, boolean list) {

		List<String> sortKeys = new ArrayList<>();

		for (Entry entry : entries) {
			String sortKey = _getSortKey(entry, list);

			if (sortKey == null) {
				return null;
			}

			sortKeys.add(sortKey);
		}

		NaturalOrderStringComparator naturalOrderStringComparator =
			new NaturalOrderStringComparator();

		for (int i = 1; i < entries.size(); i++) {
			Entry entry1 = entries.get(i - 1);
			Entry entry2 = entries.get(i);

			if (entry1.getGroup() != entry2.getGroup()) {
				continue;
			}

			int value = naturalOrderStringComparator.compare(
				sortKeys.get(i - 1), sortKeys.get(i));

			if (value > 0) {
				return _swapEntries(lines, entry1, entry2);
			}
		}

		return null;
	}

	private String _swapEntries(String[] lines, Entry entry1, Entry entry2) {
		List<String> lineList = Arrays.asList(lines);

		List<String> lines1 = new ArrayList<>(
			lineList.subList(
				entry1.getStartLineIndex(), entry1.getEndLineIndex() + 1));
		List<String> lines2 = new ArrayList<>(
			lineList.subList(
				entry2.getStartLineIndex(), entry2.getEndLineIndex() + 1));

		String lastLine1 = lines1.get(lines1.size() - 1);
		String lastLine2 = lines2.get(lines2.size() - 1);

		if (lastLine1.endsWith(StringPool.COMMA) &&
			!lastLine2.endsWith(StringPool.COMMA)) {

			lines1.set(
				lines1.size() - 1,
				lastLine1.substring(0, lastLine1.length() - 1));
			lines2.set(lines2.size() - 1, lastLine2 + StringPool.COMMA);
		}

		List<String> newLines = new ArrayList<>(
			lineList.subList(0, entry1.getStartLineIndex()));

		newLines.addAll(lines2);
		newLines.addAll(
			lineList.subList(
				entry1.getEndLineIndex() + 1, entry2.getStartLineIndex()));
		newLines.addAll(lines1);
		newLines.addAll(
			lineList.subList(entry2.getEndLineIndex() + 1, lines.length));

		StringBundler sb = new StringBundler(newLines.size() * 2);

		for (String line : newLines) {
			sb.append(line);
			sb.append(CharPool.NEW_LINE);
		}

		sb.setIndex(sb.index() - 1);

		return sb.toString();
	}

	private static final String[] _SORTABLE_LIST_KEYS = {"Action", "NotAction"};

	private static final Pattern _blockPattern = Pattern.compile("\\w.*\\{");
	private static final Pattern _heredocPattern = Pattern.compile(
		"<<-?(\\w+)$");
	private static final Pattern _indentPattern = Pattern.compile("^\\s*");
	private static final Pattern _keyPattern = Pattern.compile(
		"^(\"[^\"]+\"|[\\w.-]+)\\s*[:=](?!=)");
	private static final Pattern _nestedBlockPattern = Pattern.compile(
		"([\\w-]+(\\s+\"[^\"]*\")*)\\s*\\{");
	private static final Pattern _stringValuePattern = Pattern.compile(
		"\"([^\"]*)\",?");

	private class Entry {

		public Entry(int group, int startLineIndex, String firstLine) {
			_group = group;
			_startLineIndex = startLineIndex;
			_firstLine = firstLine;

			_endLineIndex = startLineIndex;
		}

		public int getEndLineIndex() {
			return _endLineIndex;
		}

		public String getFirstLine() {
			return _firstLine;
		}

		public int getGroup() {
			return _group;
		}

		public int getStartLineIndex() {
			return _startLineIndex;
		}

		public void setEndLineIndex(int endLineIndex) {
			_endLineIndex = endLineIndex;
		}

		private int _endLineIndex;
		private final String _firstLine;
		private final int _group;
		private final int _startLineIndex;

	}

}