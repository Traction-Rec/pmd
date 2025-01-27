/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.documentation;

import static apex.jorje.semantic.symbol.type.ModifierTypeInfos.GLOBAL;
import static apex.jorje.semantic.symbol.type.ModifierTypeInfos.OVERRIDE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sourceforge.pmd.lang.apex.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.apex.ast.ASTFormalComment;
import net.sourceforge.pmd.lang.apex.ast.ASTMethod;
import net.sourceforge.pmd.lang.apex.ast.ASTModifierNode;
import net.sourceforge.pmd.lang.apex.ast.ASTProperty;
import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.ast.ASTUserInterface;
import net.sourceforge.pmd.lang.apex.ast.ApexNode;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

import apex.jorje.data.Locations;
import apex.jorje.semantic.ast.modifier.ModifierGroup;

/**
 * Differs from ApexDocRule in that it does not require an @description field in the comment
 */
public class ApexRecDocRule extends AbstractApexRule {
    private static final Pattern RETURN_PATTERN = Pattern.compile("@return\\s");
    private static final Pattern PARAM_PATTERN = Pattern.compile("@param\\s+(\\w+)\\s");

    private static final String MISSING_COMMENT_MESSAGE = "Missing ApexDoc comment";
    private static final String MISSING_RETURN_MESSAGE = "Missing ApexDoc @return";
    private static final String UNEXPECTED_RETURN_MESSAGE = "Unexpected ApexDoc @return";
    private static final String MISMATCHED_PARAM_MESSAGE = "Missing or mismatched ApexDoc @param";

    public ApexRecDocRule() {
        addRuleChainVisit(ASTUserClass.class);
        addRuleChainVisit(ASTUserInterface.class);
        addRuleChainVisit(ASTMethod.class);
        addRuleChainVisit(ASTProperty.class);
    }

    @Override
    public Object visit(ASTUserClass node, Object data) {
        handleClassOrInterface(node, data);
        return data;
    }

    @Override
    public Object visit(ASTUserInterface node, Object data) {
        handleClassOrInterface(node, data);
        return data;
    }

    @Override
    public Object visit(ASTMethod node, Object data) {
        if (node.jjtGetParent() instanceof ASTProperty) {
            // Skip property methods, doc is required on the property itself
            return data;
        }

        ApexDocComment comment = getApexDocComment(node);
        if (comment == null) {
            if (shouldHaveApexDocs(node)) {
                addViolationWithMessage(data, node, MISSING_COMMENT_MESSAGE);
            }
        } else {
            String returnType = node.getNode().getReturnTypeRef().toString();
            boolean shouldHaveReturn = !(returnType.isEmpty() || "void".equalsIgnoreCase(returnType));
            if (comment.hasReturn != shouldHaveReturn) {
                if (shouldHaveReturn) {
                    addViolationWithMessage(data, node, MISSING_RETURN_MESSAGE);
                } else {
                    addViolationWithMessage(data, node, UNEXPECTED_RETURN_MESSAGE);
                }
            }

            // Collect parameter names in order
            final List<String> params = node.getNode().getMethodInfo().getParameters()
                    .stream().map(p -> p.getName().getValue()).collect(Collectors.toList());

            if (!comment.params.equals(params)) {
                addViolationWithMessage(data, node, MISMATCHED_PARAM_MESSAGE);
            }
        }

        return data;
    }

    @Override
    public Object visit(ASTProperty node, Object data) {
        ApexDocComment comment = getApexDocComment(node);
        if (comment == null) {
            if (shouldHaveApexDocs(node)) {
                addViolationWithMessage(data, node, MISSING_COMMENT_MESSAGE);
            }
        }

        return data;
    }

    private void handleClassOrInterface(ApexNode<?> node, Object data) {
        ApexDocComment comment = getApexDocComment(node);
        if (comment == null) {
            if (shouldHaveApexDocs(node)) {
                addViolationWithMessage(data, node, MISSING_COMMENT_MESSAGE);
            }
        }
    }

    private boolean shouldHaveApexDocs(ApexNode<?> node) {
        if (node.getNode().getLoc() == Locations.NONE) {
            return false;
        }

        // is this a test?
        for (final ASTAnnotation annotation : node.findDescendantsOfType(ASTAnnotation.class)) {
            if ("IsTest".equals(annotation.getImage())) {
                return false;
            }
        }

        ASTModifierNode modifier = node.getFirstChildOfType(ASTModifierNode.class);
        if (modifier != null) {
            boolean isPublic = modifier.isPublic();
            ModifierGroup modifierGroup = modifier.getNode().getModifiers();
            boolean isGlobal = modifierGroup.has(GLOBAL);
            boolean isOverride = modifierGroup.has(OVERRIDE);
            return (isPublic || isGlobal) && !isOverride;
        }
        return false;
    }

    private ApexDocComment getApexDocComment(ApexNode<?> node) {
        ASTFormalComment comment = node.getFirstChildOfType(ASTFormalComment.class);
        if (comment != null) {
            String token = comment.getToken();

            boolean hasReturn = RETURN_PATTERN.matcher(token).find();

            ArrayList<String> params = new ArrayList<>();
            Matcher paramMatcher = PARAM_PATTERN.matcher(token);
            while (paramMatcher.find()) {
                params.add(paramMatcher.group(1));
            }

            return new ApexDocComment(hasReturn, params);
        }
        return null;
    }

    private static class ApexDocComment {
        boolean hasReturn;
        List<String> params;

        ApexDocComment(boolean hasReturn, List<String> params) {
            this.hasReturn = hasReturn;
            this.params = params;
        }
    }
}
