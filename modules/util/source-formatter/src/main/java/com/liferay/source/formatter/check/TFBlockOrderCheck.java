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
import com.liferay.source.formatter.check.util.TFSourceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

		String[] lines = content.split("\n", -1);

		_sortNestedEntries(lines);
		_sortTopLevelBlocks(lines);

		StringBundler sb = new StringBundler(lines.length * 2);

		for (String line : lines) {
			sb.append(line);
			sb.append(CharPool.NEW_LINE);
		}

		sb.setIndex(sb.index() - 1);

		return sb.toString();
	}

	private List<Entry> _getEntries(String[] lines, int openLineIndex) {
		int closeLineIndex = TFSourceUtil.getBlockEndLineIndex(
			lines, openLineIndex + 1, 1);

		if (closeLineIndex == -1) {
			return null;
		}

		List<Entry> entries = new ArrayList<>();

		int group = 0;

		for (int i = openLineIndex + 1; i < closeLineIndex; i++) {
			if (Validator.isNull(StringUtil.trim(lines[i]))) {
				group++;

				continue;
			}

			int endLineIndex = TFSourceUtil.getBlockEndLineIndex(lines, i, 0);

			if ((endLineIndex == -1) || (endLineIndex >= closeLineIndex)) {
				return null;
			}

			entries.add(new Entry(group, lines, i, endLineIndex));

			i = endLineIndex;
		}

		return entries;
	}

	private String _getKey(String trimmedLine) {
		Matcher matcher = _keyPattern.matcher(trimmedLine);

		if (matcher.find()) {
			return StringUtil.removeChars(matcher.group(1), CharPool.QUOTE);
		}

		matcher = _blockHeaderPattern.matcher(trimmedLine);

		if (matcher.matches()) {
			return StringUtil.removeChars(matcher.group(1), CharPool.QUOTE);
		}

		return null;
	}

	private List<String> _getLines(Entry sortedEntry, Entry entry) {
		List<String> lines = new ArrayList<>(sortedEntry.getLines());

		int lastIndex = lines.size() - 1;

		String lastLine = lines.get(lastIndex);

		if (lastLine.endsWith(StringPool.COMMA)) {
			lastLine = lastLine.substring(0, lastLine.length() - 1);
		}

		String originalLastLine = entry.getLastLine();

		if (originalLastLine.endsWith(StringPool.COMMA)) {
			lastLine += StringPool.COMMA;
		}

		lines.set(lastIndex, lastLine);

		return lines;
	}

	private String _getSortKey(Entry entry, boolean list) {
		if (!list) {
			return _getKey(entry.getFirstLine());
		}

		List<String> lines = entry.getLines();

		if (lines.size() != 1) {
			return null;
		}

		Matcher matcher = _stringValuePattern.matcher(entry.getFirstLine());

		if (matcher.matches()) {
			return matcher.group(1);
		}

		return null;
	}

	private boolean _isCommentLine(String line) {
		if (line.startsWith(StringPool.POUND) ||
			line.startsWith(StringPool.DOUBLE_SLASH)) {

			return true;
		}

		return false;
	}

	private boolean _setSortKeys(List<Entry> entries, boolean list) {
		for (Entry entry : entries) {
			String sortKey = _getSortKey(entry, list);

			if (sortKey == null) {
				return false;
			}

			entry.setSortKey(sortKey);
		}

		return true;
	}

	private void _sort(String[] lines, List<Entry> entries) {
		List<Entry> sortedEntries = new ArrayList<>(entries);

		Collections.sort(sortedEntries, new EntryComparator());

		if (sortedEntries.equals(entries)) {
			return;
		}

		String[] originalLines = lines.clone();

		Entry firstEntry = entries.get(0);

		int lineIndex = firstEntry.getStartLineIndex();

		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);

			if (i > 0) {
				Entry previousEntry = entries.get(i - 1);

				for (int j = previousEntry.getEndLineIndex() + 1;
					 j < entry.getStartLineIndex(); j++) {

					lines[lineIndex++] = originalLines[j];
				}
			}

			for (String line : _getLines(sortedEntries.get(i), entry)) {
				lines[lineIndex++] = line;
			}
		}
	}

	private void _sortNestedEntries(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			String heredocMarker = TFSourceUtil.getHeredocMarker(lines[i]);

			if (heredocMarker != null) {
				int heredocEndLineIndex = TFSourceUtil.getHeredocEndLineIndex(
					lines, i, heredocMarker);

				if (heredocEndLineIndex == -1) {
					return;
				}

				String nextTrimmedLine = StringUtil.trim(lines[i + 1]);

				if (!nextTrimmedLine.equals(StringPool.OPEN_BRACKET) &&
					!nextTrimmedLine.equals(StringPool.OPEN_CURLY_BRACE)) {

					i = heredocEndLineIndex;
				}

				continue;
			}

			String trimmedLine = StringUtil.trim(
				TFSourceUtil.removeComment(lines[i]));

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

			if ((entries == null) || !_setSortKeys(entries, list)) {
				continue;
			}

			_sort(lines, entries);
		}
	}

	private void _sortTopLevelBlocks(String[] lines) {
		List<Entry> entries = new ArrayList<>();

		int previousEndLineIndex = -1;

		for (int i = 0; i < lines.length; i++) {
			Matcher matcher = _topLevelBlockPattern.matcher(lines[i]);

			if (!matcher.matches()) {
				if (Validator.isNull(StringUtil.trim(lines[i])) ||
					_isCommentLine(lines[i])) {

					continue;
				}

				return;
			}

			String sortKey = _getKey(lines[i]);

			if (sortKey == null) {
				return;
			}

			int endLineIndex = TFSourceUtil.getBlockEndLineIndex(lines, i, 0);

			if (endLineIndex == -1) {
				return;
			}

			int startLineIndex = i;

			while (((startLineIndex - 1) > previousEndLineIndex) &&
				   _isCommentLine(lines[startLineIndex - 1])) {

				startLineIndex--;
			}

			Entry entry = new Entry(0, lines, startLineIndex, endLineIndex);

			entry.setSortKey(sortKey);

			entries.add(entry);

			i = endLineIndex;

			previousEndLineIndex = endLineIndex;
		}

		_sort(lines, entries);
	}

	private static final String[] _SORTABLE_LIST_KEYS = {"Action", "NotAction"};

	private static final Pattern _blockHeaderPattern = Pattern.compile(
		"([\\w-]+(\\s+\"[^\"]*\")*)\\s*\\{");
	private static final Pattern _keyPattern = Pattern.compile(
		"^(\"[^\"]+\"|[\\w.-]+)\\s*[:=](?!=)");
	private static final Pattern _stringValuePattern = Pattern.compile(
		"\"([^\"]*)\",?");
	private static final Pattern _topLevelBlockPattern = Pattern.compile(
		"\\w.*\\{");

	private class Entry {

		public Entry(
			int group, String[] lines, int startLineIndex, int endLineIndex) {

			_group = group;
			_startLineIndex = startLineIndex;

			for (int i = startLineIndex; i <= endLineIndex; i++) {
				_lines.add(lines[i]);
			}
		}

		public int getEndLineIndex() {
			return _startLineIndex + _lines.size() - 1;
		}

		public String getFirstLine() {
			return StringUtil.trim(_lines.get(0));
		}

		public int getGroup() {
			return _group;
		}

		public String getLastLine() {
			return _lines.get(_lines.size() - 1);
		}

		public List<String> getLines() {
			return _lines;
		}

		public String getSortKey() {
			return _sortKey;
		}

		public int getStartLineIndex() {
			return _startLineIndex;
		}

		public void setSortKey(String sortKey) {
			_sortKey = sortKey;
		}

		private final int _group;
		private final List<String> _lines = new ArrayList<>();
		private String _sortKey;
		private final int _startLineIndex;

	}

	private class EntryComparator implements Comparator<Entry> {

		@Override
		public int compare(Entry entry1, Entry entry2) {
			if (entry1.getGroup() != entry2.getGroup()) {
				return entry1.getGroup() - entry2.getGroup();
			}

			return _naturalOrderStringComparator.compare(
				entry1.getSortKey(), entry2.getSortKey());
		}

		private final NaturalOrderStringComparator
			_naturalOrderStringComparator = new NaturalOrderStringComparator();

	}

}