/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.rule.security;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.sourceforge.pmd.lang.apex.ast.ASTMethodCallExpression;
import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.ast.AbstractApexNode;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;

public class ApexRecRestrictDatabaseUsageRule extends AbstractApexRule {

    private static final PropertyDescriptor<String> CLASSES_ALLOWED_DATABASE_ACCESS_CSV_PROPERTY
            = PropertyFactory.stringProperty("classesAllowedDatabaseAccessCsv")
            .desc("CSV of names of classes allowed database access.")
            .defaultValue("")
            .build();
    private static final PropertyDescriptor<String> UNRESTRICTED_DATABASE_METHODS_CSV_PROPERTY
            = PropertyFactory.stringProperty("unrestrictedDatabaseMethodsCsv")
            .desc("Database methods that are not restricted.")
            .defaultValue("")
            .build();

    public ApexRecRestrictDatabaseUsageRule() {
        definePropertyDescriptor(CLASSES_ALLOWED_DATABASE_ACCESS_CSV_PROPERTY);
        definePropertyDescriptor(UNRESTRICTED_DATABASE_METHODS_CSV_PROPERTY);
    }

    @Override
    public Object visit(ASTUserClass node, Object data) {
        // Not interested in tests
        final String className = RecHelper.getClassName(node);
        final List<String> allowedClasses =
                Arrays.asList(getProperty(CLASSES_ALLOWED_DATABASE_ACCESS_CSV_PROPERTY).split("\\s*,\\s*"));
        if (RecHelper.isTestMethodOrClass(node) || allowedClasses.contains(className)) {
            return data;
        }

        // if class calls the database add an error
        List<AbstractApexNode<?>> databaseAccessNodes = RecHelper.getAllDatabaseAccessNodesNotInSubclasses(node);
        List<String> allowedMethodNames = RecHelper.convertListStringToLowerCase(
                Arrays.asList(getProperty(UNRESTRICTED_DATABASE_METHODS_CSV_PROPERTY).split("\\s*,\\s*"))
        );
        if (!databaseAccessNodes.isEmpty()) {
            for (AbstractApexNode<?> call : databaseAccessNodes) {
                // If calling Database.x (i.e. not inline SOQL or SOSL) check if call is allowed
                if (call instanceof ASTMethodCallExpression) {
                    ASTMethodCallExpression methodNode = (ASTMethodCallExpression) call;
                    if (!allowedMethodNames.contains(methodNode.getMethodName().toLowerCase(Locale.ROOT))) {
                        addViolation(data, call);
                    }
                } else {
                    addViolation(data, call);
                }
            }
        }

        return data;
    }
}
