/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.security;

import java.util.*;

import apex.jorje.semantic.symbol.type.CodeUnitDetails;
import net.sourceforge.pmd.lang.apex.ast.*;

import apex.jorje.data.Identifier;
import apex.jorje.data.ast.TypeRef;
import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.VariableExpression;
import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.ast.statement.FieldDeclaration;
import apex.jorje.semantic.ast.statement.VariableDeclaration;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

/**
 * Helper methods
 * 
 * @author sergey.gorbaty
 *
 */
public final class Helper {
    static final String ANY_METHOD = "*";

    private Helper() {
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
        final List<ASTModifierNode> modifierNode = node.findChildrenOfType(ASTModifierNode.class);
        for (final ASTModifierNode m : modifierNode) {
            if (m.getNode().getModifiers().isTest()) {
                return true;
            }
        }

        final String className = node.getNode().getDefiningType().getApexName();
        return className.endsWith("Test") || className.endsWith("_t") || className.endsWith("Test_serial");
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

    static boolean foundAnySOQLorSOSL(final ApexNode<?> node) {
        return !getAllInlineSoqlOrSosl(node).isEmpty();
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

    /**
     * Finds DML operations in a given node descendants' path
     * 
     * @param node
     * 
     * @return true if found DML operations in node descendants
     */
    static boolean foundAnyDML(final ApexNode<?> node) {
        return !getAllInlineDml(node).isEmpty();
    }

    static boolean isMethodName(final ASTMethodCallExpression methodNode, final String className,
            final String methodName) {
        final ASTReferenceExpression reference = methodNode.getFirstChildOfType(ASTReferenceExpression.class);

        return reference != null && reference.getNode().getNames().size() == 1
                && reference.getNode().getNames().get(0).getValue().equalsIgnoreCase(className)
                && (methodName.equals(ANY_METHOD) || isMethodName(methodNode, methodName));
    }

    static boolean isMethodName(final ASTMethodCallExpression m, final String methodName) {
        return isMethodName(m.getNode(), methodName);
    }

    static boolean isMethodName(final MethodCallExpression m, final String methodName) {
        return m.getMethodName().equalsIgnoreCase(methodName);
    }

    static boolean isMethodCallChain(final ASTMethodCallExpression methodNode, final String... methodNames) {
        String methodName = methodNames[methodNames.length - 1];
        if (Helper.isMethodName(methodNode, methodName)) {
            final ASTReferenceExpression reference = methodNode.getFirstChildOfType(ASTReferenceExpression.class);
            if (reference != null) {
                final ASTMethodCallExpression nestedMethod = reference
                        .getFirstChildOfType(ASTMethodCallExpression.class);
                if (nestedMethod != null) {
                    String[] newMethodNames = Arrays.copyOf(methodNames, methodNames.length - 1);
                    return isMethodCallChain(nestedMethod, newMethodNames);
                } else {
                    String[] newClassName = Arrays.copyOf(methodNames, methodNames.length - 1);
                    if (newClassName.length == 1) {
                        return Helper.isMethodName(methodNode, newClassName[0], methodName);
                    }
                }
            }
        }

        return false;
    }

    static String getFQVariableName(final ASTVariableExpression variable) {
        final ASTReferenceExpression ref = variable.getFirstChildOfType(ASTReferenceExpression.class);
        String objectName = "";
        if (ref != null) {
            if (ref.getNode().getNames().size() == 1) {
                objectName = ref.getNode().getNames().get(0).getValue() + ".";
            }
        }

        VariableExpression n = variable.getNode();
        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":").append(objectName)
                .append(n.getIdentifier().getValue());
        return sb.toString();
    }

    static String getFQVariableName(final ASTVariableDeclaration variable) {
        VariableDeclaration n = variable.getNode();
        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":")
                .append(n.getLocalInfo().getName());
        return sb.toString();
    }

    static String getFQVariableName(final ASTField variable) {
        Field n = variable.getNode();
        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":")
                .append(n.getFieldInfo().getName());
        return sb.toString();
    }

    static String getVariableType(final ASTField variable) {
        Field n = variable.getNode();
        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":")
                .append(n.getFieldInfo().getName());
        return sb.toString();
    }
    
    static String getFQVariableName(final ASTFieldDeclaration variable) {
        FieldDeclaration n = variable.getNode();
        String name = "";

        try {
            java.lang.reflect.Field f = n.getClass().getDeclaredField("name");
            f.setAccessible(true);
            Identifier nameField = (Identifier) f.get(n);
            name = nameField.getValue();

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":").append(name);
        return sb.toString();
    }

    static String getFQVariableName(final ASTNewKeyValueObjectExpression variable) {
        NewKeyValueObjectExpression n = variable.getNode();
        TypeRef typeRef = n.getTypeRef();
        String objType = typeRef.getNames().get(0).getValue();

        StringBuilder sb = new StringBuilder().append(n.getDefiningType().getApexName()).append(":").append(objType);
        return sb.toString();
    }

    static boolean isSystemLevelClass(ASTUserClass node) {
        List<TypeRef> interfaces = node.getNode().getDefiningType().getCodeUnitDetails().getInterfaceTypeRefs();

        for (TypeRef intObject : interfaces) {
            if (isWhitelisted(intObject.getNames())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWhitelisted(List<Identifier> ids) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i).getValue());

            if (i != ids.size() - 1) {
                sb.append(".");
            }
        }

        switch (sb.toString().toLowerCase(Locale.ROOT)) {
        case "queueable":
        case "database.batchable":
        case "installhandler":
            return true;
        default:
            break;
        }
        return false;
    }

    public static String getFQVariableName(Parameter p) {
        StringBuffer sb = new StringBuffer();
        sb.append(p.getDefiningType()).append(":").append(p.getName().getValue());
        return sb.toString();
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
