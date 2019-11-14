package org.codingmatters.poom.services.domain.property.query.events;

import org.codingmatters.poom.services.domain.property.query.FilterEvents;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;

import java.util.Stack;

public class EventsGenerator extends PropertyFilterBaseVisitor {
    private final FilterEvents events;
    private final Stack<Object> stack = new Stack<>();

    public void reset() {
        this.stack.clear();
    }

    public EventsGenerator(FilterEvents events) {
        this.events = events;
    }

    @Override
    public Object visitDecimalOperand(PropertyFilterParser.DecimalOperandContext ctx) {
        if(ctx.getText().contains(".")) {
            this.stack.push(Double.parseDouble(ctx.getText()));
        } else {
            this.stack.push(Long.parseLong(ctx.getText()));
        }
        return this.stack.peek();
    }

    @Override
    public Object visitStringOperand(PropertyFilterParser.StringOperandContext ctx) {
        this.stack.push(ctx.getText());
        return this.stack.peek();
    }

    @Override
    public Object visitTrueOperand(PropertyFilterParser.TrueOperandContext ctx) {
        this.stack.push(Boolean.TRUE);
        return this.stack.peek();
    }

    @Override
    public Object visitFalseOperand(PropertyFilterParser.FalseOperandContext ctx) {
        this.stack.push(Boolean.FALSE);
        return this.stack.peek();
    }

    @Override
    public Object visitNullOperand(PropertyFilterParser.NullOperandContext ctx) {
        this.stack.push(null);
        return this.stack.peek();
    }

    @Override
    public Object visitPropertyOperand(PropertyFilterParser.PropertyOperandContext ctx) {
        this.stack.push(new Property(ctx.IDENTIFIER().getText()));
        return this.stack.peek();
    }

    @Override
    public Object visitComparison(PropertyFilterParser.ComparisonContext ctx) {
        super.visitComparison(ctx);
        if(this.stack.peek() instanceof Property) {
            if (ctx.operator().GT() != null) {
                return this.events.graterThanProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().GTE() != null) {
                return this.events.graterThanOrEqualsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().LT() != null) {
                return this.events.lowerThanProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().LTE() != null) {
                return this.events.lowerThanOrEqualsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().EQ() != null) {
                return this.events.isEqualsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().STARTS_WITH() != null) {
                return this.events.startsWithProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().ENDS_WITH() != null) {
                return this.events.endsWithProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            } else if (ctx.operator().CONTAINS() != null) {
                return this.events.containsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
            }
        } else {
            if (ctx.operator().GT() != null) {
                return this.events.graterThan(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().GTE() != null) {
                return this.events.graterThanOrEquals(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().LT() != null) {
                return this.events.lowerThan(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().LTE() != null) {
                return this.events.lowerThanOrEquals(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().EQ() != null) {
                return this.events.isEquals(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().STARTS_WITH() != null) {
                return this.events.startsWith(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().ENDS_WITH() != null) {
                return this.events.endsWith(ctx.IDENTIFIER().getText(), this.stack.pop());
            } else if (ctx.operator().CONTAINS() != null) {
                return this.events.contains(ctx.IDENTIFIER().getText(), this.stack.pop());
            }
        }
        return null;
    }

    @Override
    public Object visitNegation(PropertyFilterParser.NegationContext ctx) {
        super.visitNegation(ctx);
        return this.events.not();
    }

    @Override
    public Object visitOr(PropertyFilterParser.OrContext ctx) {
        super.visitOr(ctx);
        return this.events.or();
    }

    @Override
    public Object visitAnd(PropertyFilterParser.AndContext ctx) {
        super.visitAnd(ctx);
        return this.events.and();
    }

    class Property {
        public final String name;

        Property(String name) {
            this.name = name;
        }
    }
}
