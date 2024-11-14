package org.codingmatters.poom.services.domain.property.query.events;

import org.codingmatters.poom.services.domain.property.query.FilterEvents;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class FilterEventsGenerator extends PropertyFilterBaseVisitor {
    public enum SpecialValues {
        NULL
    }

    private final FilterEvents events;
    private final Stack<Object> stack = new Stack<>();

    public void reset() {
        this.stack.clear();
    }

    public FilterEventsGenerator(FilterEvents events) {
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
        this.stack.push(ctx.getText().substring(1, ctx.getText().length() - 1));
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
        this.stack.push(SpecialValues.NULL);
        return this.stack.peek();
    }

    @Override
    public Object visitPropertyOperand(PropertyFilterParser.PropertyOperandContext ctx) {
        this.stack.push(new Property(ctx.IDENTIFIER().getText()));
        return this.stack.peek();
    }

    @Override
    public Object visitDateOperand(PropertyFilterParser.DateOperandContext ctx) {
        this.stack.push(LocalDate.parse(ctx.DATE_LITERAL().getText()));
        return this.stack.peek();
    }

    @Override
    public Object visitTimeOperand(PropertyFilterParser.TimeOperandContext ctx) {
        if(ctx.TIME_LITERAL() != null) {
            this.stack.push(LocalTime.parse(ctx.TIME_LITERAL().getText()));
        } else if(ctx.TIME_WITHOUT_SFRAC_LITERAL() != null) {
            this.stack.push(LocalTime.parse(ctx.TIME_WITHOUT_SFRAC_LITERAL().getText()));
        } else {
            throw new AssertionError("unimplemented time literal :" + ctx.getText());
        }

        return this.stack.peek();
    }

    @Override
    public Object visitDatetimeOperand(PropertyFilterParser.DatetimeOperandContext ctx) {
        if(ctx.DATETIME_LITERAL() != null) {
            this.stack.push(LocalDateTime.parse(ctx.DATETIME_LITERAL().getText()));
        } else if(ctx.DATETIME_WITHOUT_SFRAC_LITERAL() != null) {
            this.stack.push(LocalDateTime.parse(ctx.DATETIME_WITHOUT_SFRAC_LITERAL().getText()));
        } else {
            throw new AssertionError("unimplemented datetime literal :" + ctx.getText());
        }
        return this.stack.peek();
    }

    @Override
    public Object visitUtcDatetimeOperand(PropertyFilterParser.UtcDatetimeOperandContext ctx) {
        if(ctx.UTC_DATETIME_LITERAL() != null) {
            this.stack.push(ZonedDateTime.parse(ctx.UTC_DATETIME_LITERAL().getText()));
        } else if(ctx.UTC_DATETIME_WITHOUT_SFRAC_LITERAL() != null) {
            this.stack.push(ZonedDateTime.parse(ctx.UTC_DATETIME_WITHOUT_SFRAC_LITERAL().getText()));
        } else {
            throw new AssertionError("unimplemented utc datetime literal :" + ctx.getText());
        }
        return super.visitUtcDatetimeOperand(ctx);
    }

    @Override
    public Object visitZonedDatetimeOperand(PropertyFilterParser.ZonedDatetimeOperandContext ctx) {
        if(ctx.ZONED_DATETIME_LITERAL() != null) {
            this.stack.push(ZonedDateTime.parse(ctx.ZONED_DATETIME_LITERAL().getText()));
        } else if(ctx.ZONED_DATETIME_WITHOUT_SFRAC_LITERAL() != null) {
            this.stack.push(ZonedDateTime.parse(ctx.ZONED_DATETIME_WITHOUT_SFRAC_LITERAL().getText()));
        } else {
            throw new AssertionError("unimplemented zoned datetime literal :" + ctx.getText());
        }

        return super.visitZonedDatetimeOperand(ctx);
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
                return this.eqPropExpression(ctx);
            } else if (ctx.operator().NEQ() != null) {
                return this.neqPropExpression(ctx);
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
                return eqExpression(ctx);
            } else if (ctx.operator().NEQ() != null) {
                return neqExpression(ctx);
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

    private Object eqExpression(PropertyFilterParser.ComparisonContext ctx) {
        Object value = this.stack.pop();
        if(SpecialValues.NULL.equals(value)) {
            return this.events.isNull(ctx.IDENTIFIER().getText());
        } else {
            return this.events.isEquals(ctx.IDENTIFIER().getText(), value);
        }
    }

    private Object neqExpression(PropertyFilterParser.ComparisonContext ctx) {
        Object value = this.stack.pop();
        if(SpecialValues.NULL.equals(value)) {
            return this.events.isNotNull(ctx.IDENTIFIER().getText());
        } else {
            return this.events.isNotEquals(ctx.IDENTIFIER().getText(), value);
        }
    }

    private Object eqPropExpression(PropertyFilterParser.ComparisonContext ctx) {
        return this.events.isEqualsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
    }
    private Object neqPropExpression(PropertyFilterParser.ComparisonContext ctx) {
        return this.events.isNotEqualsProperty(ctx.IDENTIFIER().getText(), ((Property)this.stack.pop()).name);
    }

    @Override
    public Object visitNegation(PropertyFilterParser.NegationContext ctx) {
        super.visitNegation(ctx);
        return this.events.not();
    }

    @Override
    public Object visitIsEmpty(PropertyFilterParser.IsEmptyContext ctx) {
        super.visitIsEmpty(ctx);
        return this.events.isEmpty(ctx.IDENTIFIER().getText());
    }

    @Override
    public Object visitIsNotEmpty(PropertyFilterParser.IsNotEmptyContext ctx) {
        super.visitIsNotEmpty(ctx);
        return this.events.isNotEmpty(ctx.IDENTIFIER().getText());
    }

    @Override
    public Object visitIn(PropertyFilterParser.InContext ctx) {
        super.visitIn(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.in(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitInEmpty(PropertyFilterParser.InEmptyContext ctx) {
        return this.events.in(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitAnyIn(PropertyFilterParser.AnyInContext ctx) {
        super.visitAnyIn(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.anyIn(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitAnyInEmpty(PropertyFilterParser.AnyInEmptyContext ctx) {
        return this.events.anyIn(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitContainsAny(PropertyFilterParser.ContainsAnyContext ctx) {
        super.visitContainsAny(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.containsAny(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitContainsAnyEmpty(PropertyFilterParser.ContainsAnyEmptyContext ctx) {
        return this.events.containsAny(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitStartsWithAny(PropertyFilterParser.StartsWithAnyContext ctx) {
        super.visitStartsWithAny(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.startsWithAny(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitStartsWithAnyEmpty(PropertyFilterParser.StartsWithAnyEmptyContext ctx) {
        return this.events.startsWithAny(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitEndsWithAny(PropertyFilterParser.EndsWithAnyContext ctx) {
        super.visitEndsWithAny(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.endsWithAny(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitEndsWithAnyEmpty(PropertyFilterParser.EndsWithAnyEmptyContext ctx) {
        return this.events.endsWithAny(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitContainsAll(PropertyFilterParser.ContainsAllContext ctx) {
        super.visitContainsAll(ctx);
        List<Object> list = new LinkedList<>();
        for (Object o : this.stack) {
            list.add(o);
        }
        this.stack.clear();

        return this.events.containsAll(ctx.IDENTIFIER().getText(), list);
    }

    @Override
    public Object visitContainsAllEmpty(PropertyFilterParser.ContainsAllEmptyContext ctx) {
        return this.events.containsAll(ctx.IDENTIFIER().getText(), Collections.emptyList());
    }

    @Override
    public Object visitIsMatchingPattern(PropertyFilterParser.IsMatchingPatternContext ctx) {
        String pattern = ctx.PATTERN().getText();

        List<FilterEvents.PatternOption> options;
        if(pattern.matches("/.*/[iI]+")) {
            options = Collections.singletonList(FilterEvents.PatternOption.CASE_INSENSITIVE);
        } else {
            options = Collections.emptyList();
        }
        return this.events.isMatchingPattern(ctx.IDENTIFIER().getText(), this.cleanPattern(pattern), options);
    }

    private String cleanPattern(String pattern) {
        if(pattern.endsWith("/")) {
            return pattern.substring(1, pattern.length() - 1);
        } else {
            return pattern.substring(1, pattern.length() - 2);
        }
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
