/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.io.unsync.UnsyncBufferedReader;
import com.liferay.petra.io.unsync.UnsyncStringReader;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.checkstyle.util.DetailASTUtil;
import com.liferay.source.formatter.util.FileUtil;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alan Huang
 */
public class JSONObjectDefinitionFileCheck extends BaseFileCheck {

	@Override
	public boolean isLiferaySourceCheck() {
		return true;
	}

	@Override
	protected String doProcess(
			String fileName, String absolutePath, String content)
		throws CheckstyleException, IOException, JSONException {

		if (!absolutePath.endsWith("-definition-object-definition.json")) {
			return content;
		}

		JSONObject jsonObject = new JSONObjectImpl(content);

		String className = jsonObject.getString("className");

		if (className.isBlank()) {
			return content;
		}

		String regex = _getClassNameRegex();

		if (className.matches(regex)) {
			return content;
		}

		addMessage(
			fileName,
			"\"className\" does not match the pattern specified by \"_class" +
				"NamePattern\" in \"ObjectDefinitionClassNameProcessorImpl\"");

		return content;
	}

	private boolean _appendString(DetailAST detailAST, StringBundler sb) {
		if (detailAST == null) {
			return false;
		}

		if (detailAST.getType() == TokenTypes.STRING_LITERAL) {
			String text = detailAST.getText();

			sb.append(text.substring(1, text.length() - 1));

			return true;
		}

		if (detailAST.getType() != TokenTypes.PLUS) {
			return false;
		}

		DetailAST firstChildDetailAST = detailAST.getFirstChild();

		DetailAST nextSiblingDetailAST = firstChildDetailAST.getNextSibling();

		if (_appendString(firstChildDetailAST, sb) &&
			_appendString(nextSiblingDetailAST, sb)) {

			return true;
		}

		return false;
	}

	private String _extractCombinedString(DetailAST detailAST) {
		DetailAST elistDetailAST = detailAST.findFirstToken(TokenTypes.ELIST);

		if ((elistDetailAST == null) || (elistDetailAST.getChildCount() == 0)) {
			return null;
		}

		DetailAST exprDetailAST = elistDetailAST.findFirstToken(
			TokenTypes.EXPR);

		if (exprDetailAST == null) {
			return null;
		}

		StringBundler sb = new StringBundler();

		if (_appendString(exprDetailAST.getFirstChild(), sb)) {
			return StringUtil.replace(sb.toString(), "\\\\", "\\");
		}

		return null;
	}

	private synchronized String _getClassNameRegex()
		throws CheckstyleException, IOException {

		if (_classNameRegex != null) {
			return _classNameRegex;
		}

		File portalDir = getPortalDir();

		if (portalDir == null) {
			return null;
		}

		File file = new File(
			portalDir,
			"/modules/apps/object/object-service/src/main/java/com/liferay" +
				"/object/internal/definition/processor" +
					"/ObjectDefinitionClassNameProcessorImpl.java");

		String content = FileUtil.read(file);

		List<String> lines = new ArrayList<>();

		try (UnsyncBufferedReader unsyncBufferedReader =
				new UnsyncBufferedReader(new UnsyncStringReader(content))) {

			String line = null;

			while ((line = unsyncBufferedReader.readLine()) != null) {
				lines.add(line);
			}
		}

		FileText fileText = new FileText(file, lines);

		FileContents fileContents = new FileContents(fileText);

		DetailAST rootDetailAST = JavaParser.parse(fileContents);

		DetailAST nextSiblingDetailAST = rootDetailAST.getNextSibling();

		while (true) {
			if (nextSiblingDetailAST.getType() != TokenTypes.CLASS_DEF) {
				nextSiblingDetailAST = nextSiblingDetailAST.getNextSibling();

				continue;
			}

			DetailAST objBlockDetailAST = nextSiblingDetailAST.findFirstToken(
				TokenTypes.OBJBLOCK);

			List<DetailAST> variableDefinitionDetailASTs =
				DetailASTUtil.getAllChildTokens(
					objBlockDetailAST, false, TokenTypes.VARIABLE_DEF);

			for (DetailAST variableDefinitionDetailAST :
					variableDefinitionDetailASTs) {

				DetailAST identDetailAST =
					variableDefinitionDetailAST.findFirstToken(
						TokenTypes.IDENT);

				if (identDetailAST == null) {
					continue;
				}

				String variableName = identDetailAST.getText();

				if (!variableName.equals("_classNamePattern")) {
					continue;
				}

				DetailAST assignDetailAST =
					variableDefinitionDetailAST.findFirstToken(
						TokenTypes.ASSIGN);

				if (assignDetailAST == null) {
					return null;
				}

				DetailAST firstChildDetailAST = assignDetailAST.getFirstChild();

				if ((firstChildDetailAST == null) ||
					(firstChildDetailAST.getType() != TokenTypes.EXPR)) {

					return null;
				}

				firstChildDetailAST = firstChildDetailAST.getFirstChild();

				if ((firstChildDetailAST == null) ||
					(firstChildDetailAST.getType() != TokenTypes.METHOD_CALL)) {

					return null;
				}

				FullIdent fullIdent = FullIdent.createFullIdentBelow(
					firstChildDetailAST);

				if (!StringUtil.equals(
						fullIdent.getText(), "Pattern.compile")) {

					return null;
				}

				_classNameRegex = _extractCombinedString(firstChildDetailAST);

				return _classNameRegex;
			}

			return null;
		}
	}

	private String _classNameRegex;

}