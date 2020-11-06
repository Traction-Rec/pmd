package net.sourceforge.pmd.lang.apex.rule.security;

import java.util.*;

import apex.jorje.data.ast.TypeRef;
import apex.jorje.semantic.symbol.type.CodeUnitDetails;
import net.sourceforge.pmd.lang.apex.ast.*;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

public class RecHelper extends Helper {
    private RecHelper() {
        super();
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
            if (getClassName(databaseAccessNode) == className) {
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
        ArrayList<AbstractApexNode<?>> databaseAccessNodes = new ArrayList<>();
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
        ArrayList<ASTMethodCallExpression> databaseCalls = new ArrayList<>();
        for (ASTMethodCallExpression call : calls) {
            if (isMethodName(call, "Database", Helper.ANY_METHOD)) {
                databaseCalls.add(call);
            }
        }
        return databaseCalls;
    }

    /**
     * @param node
     * @return all inline SOSL or SOQL operations in a given node descendant's path
     */
    static ArrayList<AbstractApexNode<?>> getAllInlineSoqlOrSosl(final ApexNode<?> node) {
        ArrayList<AbstractApexNode<?>> allDmlNodes = new ArrayList<>();
        allDmlNodes.addAll(node.findDescendantsOfType(ASTSoqlExpression.class));
        allDmlNodes.addAll(node.findDescendantsOfType(ASTSoslExpression.class));
        return allDmlNodes;
    }

    /**
     * @param node
     * @return all inline DML operations in a given node descendant's path
     */
    static ArrayList<AbstractApexNode<?>> getAllInlineDml(final ApexNode<?> node) {
        ArrayList<AbstractApexNode<?>> allDmlNodes = new ArrayList<>();
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
            lowerCaseStrings.add(iterator.next().toLowerCase());
        }
        return lowerCaseStrings;
    }
}
