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

		_sortTopLevelBlocks(lines);
		_sortEntries(lines);

		StringBundler sb = new StringBundler(lines.length * 2);

		for (String line : lines) {
			sb.append(line);
			sb.append(CharPool.NEW_LINE);
		}

		sb.setIndex(sb.index() - 1);

		return sb.toString();
	}

	private int _addHeredocLines(Entry entry, String[] lines, int lineIndex) {
		String heredocMarker = _getHeredocMarker(
			StringUtil.trim(lines[lineIndex]));

		if (heredocMarker == null) {
			return lineIndex;
		}

		int heredocEndLineIndex = _getHeredocEndLineIndex(
			lines, lineIndex, heredocMarker);

		if (heredocEndLineIndex == -1) {
			return -1;
		}

		for (int i = lineIndex + 1; i <= heredocEndLineIndex; i++) {
			entry.addLine(lines[i]);
		}

		return heredocEndLineIndex;
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
				entry = new Entry(group, i);

				entries.add(entry);
			}
			else if ((entry == null) ||
					 (indent.length() < childIndent.length())) {

				return null;
			}

			entry.addLine(lines[i]);

			i = _addHeredocLines(entry, lines, i);

			if (i == -1) {
				return null;
			}
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

	private boolean _isCloseLine(String trimmedLine) {
		char c = trimmedLine.charAt(0);

		if ((c == CharPool.CLOSE_BRACKET) ||
			(c == CharPool.CLOSE_CURLY_BRACE) ||
			(c == CharPool.CLOSE_PARENTHESIS)) {

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

	private void _sortEntries(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			String trimmedLine = StringUtil.trim(lines[i]);

			String heredocMarker = _getHeredocMarker(trimmedLine);

			if (heredocMarker != null) {
				int heredocEndLineIndex = _getHeredocEndLineIndex(
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

			_sortEntries(lines, entries);
		}
	}

	private void _sortEntries(String[] lines, List<Entry> entries) {
		List<Entry> sortedEntries = new ArrayList<>(entries);

		Collections.sort(sortedEntries, new EntryComparator());

		if (sortedEntries.equals(entries)) {
			return;
		}

		List<String> newLines = new ArrayList<>();

		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);

			if (i > 0) {
				Entry previousEntry = entries.get(i - 1);

				for (int j = previousEntry.getEndLineIndex() + 1;
					 j < entry.getStartLineIndex(); j++) {

					newLines.add(lines[j]);
				}
			}

			Entry sortedEntry = sortedEntries.get(i);

			List<String> entryLines = new ArrayList<>(sortedEntry.getLines());

			int lastIndex = entryLines.size() - 1;

			String lastLine = entryLines.get(lastIndex);

			if (lastLine.endsWith(StringPool.COMMA)) {
				lastLine = lastLine.substring(0, lastLine.length() - 1);
			}

			String originalLastLine = entry.getLastLine();

			if (originalLastLine.endsWith(StringPool.COMMA)) {
				lastLine += StringPool.COMMA;
			}

			entryLines.set(lastIndex, lastLine);

			newLines.addAll(entryLines);
		}

		Entry firstEntry = entries.get(0);

		for (int i = 0; i < newLines.size(); i++) {
			lines[firstEntry.getStartLineIndex() + i] = newLines.get(i);
		}
	}

	private void _sortTopLevelBlocks(String[] lines) {
		List<Entry> blocks = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			Matcher matcher = _topLevelBlockPattern.matcher(lines[i]);

			if (!matcher.matches()) {
				continue;
			}

			Entry block = new Entry(0, i);

			block.addLine(lines[i]);

			for (i = i + 1; i < lines.length; i++) {
				block.addLine(lines[i]);

				if (lines[i].equals(StringPool.CLOSE_CURLY_BRACE)) {
					break;
				}

				i = _addHeredocLines(block, lines, i);

				if (i == -1) {
					return;
				}
			}

			if (i == lines.length) {
				return;
			}

			block.setSortKey(_getKey(block.getFirstLine()));

			blocks.add(block);
		}

		_sortEntries(lines, blocks);
	}

	private static final String[] _SORTABLE_LIST_KEYS = {"Action", "NotAction"};

	private static final Pattern _heredocPattern = Pattern.compile(
		"<<-?(\\w+)$");
	private static final Pattern _indentPattern = Pattern.compile("^\\s*");
	private static final Pattern _keyPattern = Pattern.compile(
		"^(\"[^\"]+\"|[\\w.-]+)\\s*[:=](?!=)");
	private static final Pattern _nestedBlockPattern = Pattern.compile(
		"([\\w-]+(\\s+\"[^\"]*\")*)\\s*\\{");
	private static final Pattern _stringValuePattern = Pattern.compile(
		"\"([^\"]*)\",?");
	private static final Pattern _topLevelBlockPattern = Pattern.compile(
		"\\w.*\\{");

	private class Entry {

		public Entry(int group, int startLineIndex) {
			_group = group;
			_startLineIndex = startLineIndex;
		}

		public void addLine(String line) {
			_lines.add(line);
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