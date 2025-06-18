package org.codingmatters.poom.services.domain.property.query.rewrite;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.parsers.*;

public class PropertyPrefixer {


    private final String prefix;

    public PropertyPrefixer(String prefix) {
        this.prefix = prefix;
    }

    public PropertyQuery rewrite(PropertyQuery query) {
        PropertyQuery.Builder result = PropertyQuery.builder();

        query.opt().filter().ifPresent(filter -> this.rewriteFilter(query, result));
        query.opt().sort().ifPresent(sort -> this.rewriteSort(query, result));

        return result.build();
    }

    private void rewriteFilter(PropertyQuery query, PropertyQuery.Builder result) {
        CodePointCharStream input = CharStreams.fromString(query.filter());
        CommonTokenStream tokens = new CommonTokenStream(new PropertyFilterLexer(input));
        PropertyFilterParser parser = new PropertyFilterParser(tokens);

        FilterVisitor visitor = new FilterVisitor(this.prefix);
        visitor.visit(parser.criterion());
        result.filter(visitor.result());
    }

    private void rewriteSort(PropertyQuery query, PropertyQuery.Builder result) {
        CodePointCharStream input = CharStreams.fromString(query.sort());
        CommonTokenStream tokens = new CommonTokenStream(new PropertySortLexer(input));
        PropertySortParser parser = new PropertySortParser(tokens);

        SortVisitor visitor = new SortVisitor(this.prefix);
        visitor.visit(parser.sortExpression());
        result.sort(visitor.result());
    }

    class FilterVisitor extends PropertyFilterBaseVisitor {
        private final String prefix;
        private final StringBuilder result = new StringBuilder();

        public FilterVisitor(String prefix) {
            this.prefix = prefix;
        }

        public String result() {
            return this.result.toString();
        }

        @Override
        public Object visitComparison(PropertyFilterParser.ComparisonContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.operator());
            this.visit(ctx.operand());
            return null;
        }

        @Override
        public Object visitIn(PropertyFilterParser.InContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.IN());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitEndsWithAny(PropertyFilterParser.EndsWithAnyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.ENDS_WITH_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitContainsAll(PropertyFilterParser.ContainsAllContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.CONTAINS_ALL());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitIsEmpty(PropertyFilterParser.IsEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.IS_EMPTY());
            return null;
        }

        @Override
        public Object visitContainsAny(PropertyFilterParser.ContainsAnyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.CONTAINS_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitInEmpty(PropertyFilterParser.InEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.IN());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;

        }

        @Override
        public Object visitStartsWithAny(PropertyFilterParser.StartsWithAnyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.STARTS_WITH_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitIsMatchingPattern(PropertyFilterParser.IsMatchingPatternContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.REQ());
            this.visit(ctx.PATTERN());
            return null;
        }

        @Override
        public Object visitContainsAllEmpty(PropertyFilterParser.ContainsAllEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.CONTAINS_ALL());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitStartsWithAnyEmpty(PropertyFilterParser.StartsWithAnyEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.STARTS_WITH_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitContainsAnyEmpty(PropertyFilterParser.ContainsAnyEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.CONTAINS_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitEndsWithAnyEmpty(PropertyFilterParser.EndsWithAnyEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.ENDS_WITH_ANY());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;

        }

        @Override
        public Object visitAnyInEmpty(PropertyFilterParser.AnyInEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.ANY());
            this.visit(ctx.IN());
            this.visit(ctx.LPAR());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitIsNotEmpty(PropertyFilterParser.IsNotEmptyContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.IS_NOT_EMPTY());
            return null;
        }

        @Override
        public Object visitAnyIn(PropertyFilterParser.AnyInContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            this.visit(ctx.ANY());
            this.visit(ctx.IN());
            this.visit(ctx.LPAR());
            this.visit(ctx.operand_list());
            this.visit(ctx.RPAR());
            return null;
        }

        @Override
        public Object visitPropertyOperand(PropertyFilterParser.PropertyOperandContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            return null;
        }

        @Override
        public Object visitTerminal(TerminalNode node) {
            if(! node.getSymbol().getText().equals("<EOF>")) {
                this.result.append(node.getSymbol().getText()).append(" ");
            }
            return null;
        }
    }

    class SortVisitor extends PropertySortBaseVisitor {
        private final String prefix;
        private final StringBuilder result = new StringBuilder();

        public SortVisitor(String prefix) {
            this.prefix = prefix;
        }

        public String result() {
            return this.result.toString();
        }

        @Override
        public Object visitPropertyExpression(PropertySortParser.PropertyExpressionContext ctx) {
            this.result.append(this.prefix).append(ctx.IDENTIFIER().getSymbol().getText()).append(" ");
            if(ctx.sortDirection() != null) {
                this.visit(ctx.sortDirection());
            }
            return null;
        }

        @Override
        public Object visitTerminal(TerminalNode node) {
            if(! node.getSymbol().getText().equals("<EOF>")) {
                this.result.append(node.getSymbol().getText()).append(" ");
            }
            return null;
        }
    }
}
