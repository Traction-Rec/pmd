/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.vf;

import java.io.Writer;

import net.sourceforge.pmd.lang.AbstractLanguageVersionHandler;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.VisitorStarter;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.RuleViolationFactory;
import net.sourceforge.pmd.lang.vf.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.vf.ast.DumpFacade;
import net.sourceforge.pmd.lang.vf.ast.VfNode;
import net.sourceforge.pmd.lang.vf.rule.VfRuleViolationFactory;

public class VfHandler extends AbstractLanguageVersionHandler {

    @Override
    public RuleViolationFactory getRuleViolationFactory() {
        return VfRuleViolationFactory.INSTANCE;
    }

    @Override
    public Parser getParser(ParserOptions parserOptions) {
        return new VfParser(parserOptions);
    }

    @Override
    public VisitorStarter getTypeResolutionFacade(ClassLoader classLoader) {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                ASTCompilationUnit astCompilationUnit = (ASTCompilationUnit) rootNode;
                VfExpressionTypeVisitor visitor = new VfExpressionTypeVisitor(astCompilationUnit.getPropertySource());
                visitor.visit((ASTCompilationUnit) rootNode, null);
            }
        };
    }

    @Override
    public ParserOptions getDefaultParserOptions() {
        return new VfParserOptions();
    }

    @Deprecated
    @Override
    public VisitorStarter getDumpFacade(final Writer writer, final String prefix, final boolean recurse) {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new DumpFacade().initializeWith(writer, prefix, recurse, (VfNode) rootNode);
            }
        };
    }
}
