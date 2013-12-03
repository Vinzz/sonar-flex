/*
 * SonarQube Flex Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.flex.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.flex.FlexGrammar;
import org.sonar.flex.checks.utils.Variable;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.regex.Pattern;

@Rule(
  key = "S117",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class LocalVarAndParameterNameCheck extends SquidCheck<LexerlessGrammar> {


  private static final String DEFAULT = "^[_a-z][a-zA-Z0-9]*$";
  private static final String MESSAGE = "Rename this local variable \"{0}\" to match the regular expression {1}";
  private Pattern pattern = null;

  @RuleProperty(
    key = "format",
    defaultValue = DEFAULT)
  String format = DEFAULT;


  @Override
  public void init() {
    subscribeTo(FlexGrammar.FUNCTION_DEF);
  }

  public void visitFile(AstNode astNode) {
    if (pattern == null) {
      pattern = Pattern.compile(format);
    }
  }

  @Override
  public void visitNode(AstNode astNode) {
    checkFunctionParametersName(astNode.getFirstChild(FlexGrammar.FUNCTION_COMMON)
      .getFirstChild(FlexGrammar.FUNCTION_SIGNATURE)
      .getFirstChild(FlexGrammar.PARAMETERS));

    if (astNode.getFirstChild(FlexGrammar.FUNCTION_COMMON).getFirstChild(FlexGrammar.BLOCK) != null) {
      checkLocalVariableName(astNode.getFirstChild(FlexGrammar.FUNCTION_COMMON)
        .getFirstChild(FlexGrammar.BLOCK)
        .getFirstChild(FlexGrammar.DIRECTIVES)
        .getChildren(FlexGrammar.DIRECTIVE));
    }
  }

  private void checkLocalVariableName(List<AstNode> functionDirectives) {
    for (AstNode directive : functionDirectives) {

      if (Variable.isVariable(directive)) {
        AstNode variableDeclStatement = directive
          .getFirstChild(FlexGrammar.ANNOTABLE_DIRECTIVE)
          .getFirstChild(FlexGrammar.VARIABLE_DECLARATION_STATEMENT);

        for (AstNode identifier : Variable.getDeclaredIdentifiers(variableDeclStatement)) {
          String varName = identifier.getTokenValue();

          if (!pattern.matcher(varName).matches()) {
            getContext().createLineViolation(this, MESSAGE, identifier, varName, format);
          }
        }
      }
    }
  }

  private void checkFunctionParametersName(AstNode parametersNode) {
    if (parametersNode != null) {

      for (AstNode parameter : parametersNode.getChildren(FlexGrammar.PARAMETER)) {
        String variableName = parameter.getFirstChild(FlexGrammar.TYPED_IDENTIFIER).getFirstChild(FlexGrammar.IDENTIFIER).getTokenValue();

        if (!pattern.matcher(variableName).matches()) {
          getContext().createLineViolation(this, MESSAGE, parametersNode, variableName, format);
        }
      }

      String restParameterName = getRestParameterName(parametersNode.getFirstChild(FlexGrammar.REST_PARAMETERS));
      if (restParameterName != null && !pattern.matcher(restParameterName).matches()) {
        getContext().createLineViolation(this, MESSAGE, parametersNode, restParameterName, format);
      }
    }
  }

  private static String getRestParameterName(AstNode restParameterNode) {
    if (restParameterNode != null && restParameterNode.getFirstChild(FlexGrammar.TYPED_IDENTIFIER) != null) {
      return restParameterNode.getFirstChild(FlexGrammar.TYPED_IDENTIFIER)
        .getFirstChild(FlexGrammar.IDENTIFIER)
        .getTokenValue();
    }
    return null;
  }
}
