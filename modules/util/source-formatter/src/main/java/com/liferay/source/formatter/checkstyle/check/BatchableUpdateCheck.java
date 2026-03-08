/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.checkstyle.check;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.List;

/**
 * @author Alan Huang
 */
public class BatchableUpdateCheck extends BaseCheck {

	@Override
	public int[] getDefaultTokens() {
		return new int[] {TokenTypes.METHOD_DEF};
	}

	@Override
	protected void doVisitToken(DetailAST detailAST) {
		List<DetailAST> childDetailASTs = getAllChildTokens(
			detailAST, true, TokenTypes.LITERAL_DO, TokenTypes.LITERAL_FOR,
			TokenTypes.LITERAL_WHILE);

		for (DetailAST childDetailAST : childDetailASTs) {
			DetailAST slistDetailAST = childDetailAST.findFirstToken(
				TokenTypes.SLIST);

			if (slistDetailAST == null) {
				return;
			}

			List<DetailAST> methodCallDetailASTs = getMethodCalls(
				detailAST, null, "executeUpdate");

			DetailAST methodCallDetailAST = methodCallDetailASTs.get(0);

			String variableName = getName(
				methodCallDetailAST.findFirstToken(TokenTypes.DOT));

			DetailAST variableDefinitionDetailAST =
				getVariableDefinitionDetailAST(
					methodCallDetailAST, variableName, false);

			if ((variableDefinitionDetailAST == null) ||
				(variableDefinitionDetailAST.getLineNo() >=
					childDetailAST.getLineNo())) {

				continue;
			}

			String variableTypeName = getVariableTypeName(
				variableDefinitionDetailAST, variableName, false);

			if (variableTypeName.equals("PreparedStatement")) {
				log(methodCallDetailAST, _MSG_USE_ADD_BATCH);

				break;
			}
		}
	}

	private static final String _MSG_USE_ADD_BATCH = "add.batch.use";

}