package org.codingmatters.poom.services.domain.property.query;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterLexer;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;
import org.junit.Test;

public class ExploreTest {
    @Test
    public void given__when__then() throws Exception {

        PropertyFilterBaseVisitor v = new PropertyFilterBaseVisitor() {
            @Override
            public Object visitComparison(PropertyFilterParser.ComparisonContext ctx) {
                System.out.print("BLABLA." + ctx.IDENTIFIER().getSymbol().getText() + " ");
                this.visit(ctx.operator());
                this.visit(ctx.operand());
                return null;
            }

            @Override
            public Object visitPropertyOperand(PropertyFilterParser.PropertyOperandContext ctx) {
                System.out.print("BLUBLU." + ctx.IDENTIFIER().getSymbol().getText() + " ");
                return null;
            }

            @Override
            public Object visitTerminal(TerminalNode node) {
                System.out.print(node.getSymbol().getText() + " ");
                return null;
            }
        };


        CodePointCharStream input = CharStreams.fromString("left==right&&blu>bla");
        CommonTokenStream tokens = new CommonTokenStream(new PropertyFilterLexer(input));
        PropertyFilterParser parser = new PropertyFilterParser(tokens);

        v.visit(parser.criterion());
    }

}
