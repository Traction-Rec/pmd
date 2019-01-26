package net.sourceforge.pmd.lang.apex.rule.security;

import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

public class ApexRecRestrictExtendingExceptionRule extends AbstractApexRule {
    @Override
    public Object visit(ASTUserClass node, Object data) {
        super.visit(node, data);
        // Only interested in Exception classes, and not interested in TrecException
        final String className = Helper.getClassName(node);
        if (!className.endsWith("Exception") || "TrecException".equals(className)) {
            return data;
        }

        // if class extends exception add an error
        final String superType = Helper.getSuperType(node);
        if ("Exception".equals(superType)) {
            addViolation(data, node);
        }

        return data;
    }
}
