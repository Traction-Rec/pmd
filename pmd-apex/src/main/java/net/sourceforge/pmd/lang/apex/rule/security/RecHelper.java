/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.security;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Optional;

import net.sourceforge.pmd.lang.apex.ast.ASTDmlDeleteStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlInsertStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlMergeStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUndeleteStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUpdateStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTDmlUpsertStatement;
import net.sourceforge.pmd.lang.apex.ast.ASTMethodCallExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTSoqlExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTSoslExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.ast.AbstractApexNode;
import net.sourceforge.pmd.lang.apex.ast.ApexNode;

import apex.jorje.data.ast.TypeRef;
import apex.jorje.semantic.symbol.type.CodeUnitDetails;

public final class RecHelper {
    private RecHelper() {
        throw new AssertionError("Can't instantiate helper classes");
    }

    static String getClassName(final ApexNode<?> node) {
        return node.getNode().getDefiningType().getApexName();
    }

    static String getSuperType(final ApexNode<?> node) {
        CodeUnitDetails details = node.getNode().getDefiningType().getCodeUnitDetails();
        Optional<TypeRef> optionalTypeRef = details.getSuperTypeRef();
        if (!optionalTypeRef.isPresent()) {
            return "";
        } else {
            return optionalTypeRef.get().toString();
        }
    }

    static boolean isTestMethodOrClass(final ApexNode<?> node) {
        ASTUserClass parentClass = node.getFirstParentOfType(ASTUserClass.class);
        if (parentClass != null && isTestMethodOrClass(parentClass)) {
            return true;
        }

        final String className = node.getNode().getDefiningType().getApexName();
        return Helper.isTestMethodOrClass(node) || className.endsWith("_t") || className.endsWith("Test_serial");
    }

    /**
     * @param node The user class
     * @return All children database access nodes in this class that are not in a subclass
     */
    static List<AbstractApexNode<?>> getAllDatabaseAccessNodesNotInSubclasses(final ASTUserClass node) {
        List<AbstractApexNode<?>> databaseAccessNodes = getAllDatabaseAccessNodes(node);
        List<AbstractApexNode<?>> nonSubclassDatabaseAccessNodes = new ArrayList<>();
        final String className = getClassName(node);
        for (AbstractApexNode<?> databaseAccessNode : databaseAccessNodes) {
            if (getClassName(databaseAccessNode).equals(className)) {
                nonSubclassDatabaseAccessNodes.add(databaseAccessNode);
            }
        }
        return nonSubclassDatabaseAccessNodes;
    }

    /**
     * @param node The node
     * @return All children database access nodes
     */
    static List<AbstractApexNode<?>> getAllDatabaseAccessNodes(final ApexNode<?> node) {
        List<AbstractApexNode<?>> databaseAccessNodes = new ArrayList<>();
        databaseAccessNodes.addAll(getAllDatabaseMethodCalls(node));
        databaseAccessNodes.addAll(getAllInlineSoqlOrSosl(node));
        databaseAccessNodes.addAll(getAllInlineDml(node));
        return databaseAccessNodes;
    }

    /**
     * Check if class contains any Database.query / Database.insert [ Database.*
     * ] methods
     *
     * @param node The apex user class node
     * @return The all database method call expression nodes
     */
    static List<ASTMethodCallExpression> getAllDatabaseMethodCalls(final ApexNode<?> node) {
        List<ASTMethodCallExpression> calls = node.findDescendantsOfType(ASTMethodCallExpression.class);
        List<ASTMethodCallExpression> databaseCalls = new ArrayList<>();
        for (ASTMethodCallExpression call : calls) {
            if (Helper.isMethodName(call, "Database", Helper.ANY_METHOD)) {
                databaseCalls.add(call);
            }
        }
        return databaseCalls;
    }

    /**
     * @param node
     * @return all inline SOSL or SOQL operations in a given node descendant's path
     */
    static List<AbstractApexNode<?>> getAllInlineSoqlOrSosl(final ApexNode<?> node) {
        List<AbstractApexNode<?>> allDmlNodes = new ArrayList<>();
        allDmlNodes.addAll(node.findDescendantsOfType(ASTSoqlExpression.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTSoslExpression.class));
        return allDmlNodes;
    }

    /**
     * @param node
     * @return all inline DML operations in a given node descendant's path
     */
    static List<AbstractApexNode<?>> getAllInlineDml(final ApexNode<?> node) {
        List<AbstractApexNode<?>> allDmlNodes = new ArrayList<>();
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlUpsertStatement.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlUpdateStatement.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlUndeleteStatement.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlMergeStatement.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlInsertStatement.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTDmlDeleteStatement.class));
        return allDmlNodes;
    }

    public static List<String> convertListStringToLowerCase(List<String> strings) {
        List<String> lowerCaseStrings = new ArrayList<>();
        ListIterator<String> iterator = strings.listIterator();
        while (iterator.hasNext()) {
            lowerCaseStrings.add(iterator.next().toLowerCase(Locale.ROOT));
        }
        return lowerCaseStrings;
    }
}
