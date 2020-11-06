package net.sourceforge.pmd.lang.apex.rule.security;

import apex.jorje.semantic.ast.modifier.OldModifiers;
import apex.jorje.semantic.symbol.type.ModifierOrAnnotationTypeInfo;
import net.sourceforge.pmd.lang.apex.ast.ASTModifierNode;
import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.ast.ApexNode;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

import java.util.WeakHashMap;

public class ApexRecSharingViolationsRule extends AbstractApexRule {

    private WeakHashMap<ApexNode<?>, Object> localCacheOfReportedNodes = new WeakHashMap<>();

    @Override
    public Object visit(ASTUserClass node, Object data) {
        if (RecHelper.isTestMethodOrClass(node)) {
            return data; // stops all the rules
        }

        if (!isSharingPresent(node)) {
            reportViolation(node, data);
        }
        localCacheOfReportedNodes.clear();

        // Visit sub-nodes
        super.visit(node, data);
        return data;
    }

    private void reportViolation(ApexNode<?> node, Object data) {
        ASTModifierNode modifier = node.getFirstChildOfType(ASTModifierNode.class);
        if (modifier != null) {
            if (localCacheOfReportedNodes.put(modifier, data) == null) {
                addViolation(data, modifier);
            }
        } else {
            if (localCacheOfReportedNodes.put(node, data) == null) {
                addViolation(data, node);
            }
        }
    }

    /**
     * Does class have sharing keyword declared?
     *
     * @param node
     * @return
     */
    private boolean isSharingPresent(ApexNode<?> node) {
        boolean sharingFound = false;

        for (ModifierOrAnnotationTypeInfo type : node.getNode().getDefiningType().getModifiers().all()) {
            if (type.getBytecodeName().equalsIgnoreCase(OldModifiers.ModifierType.WithoutSharing.toString())) {
                sharingFound = true;
                break;
            }
            if (type.getBytecodeName().equalsIgnoreCase(OldModifiers.ModifierType.WithSharing.toString())) {
                sharingFound = true;
                break;
            }
            if (type.getBytecodeName().equalsIgnoreCase("InheritedSharing")) {
                sharingFound = true;
                break;
            }

        }
        return sharingFound;
    }

}
