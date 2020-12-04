/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.security;

import net.sourceforge.pmd.lang.apex.ast.ASTMethodCallExpression;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

public class ApexRecSystemDebugRestrictionRule extends AbstractApexRule {

    @Override
    public Object visit(ASTMethodCallExpression node, Object data) {
        final String className = node.getNode().getDefiningType().getApexName();
        if (RecHelper.isTestMethodOrClass(node)
                || "Log".equals(className)
                || "LogData".equals(className)) {
            return data; // stops all the rules
        }
        if ("System.debug".equals(node.getFullMethodName())) {
            addViolation(data, node);
        }

        return data;
    }
}
