package org.codingmatters.poom.services.domain.property.query.events;

import org.codingmatters.poom.services.domain.property.query.SortEvents;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortParser;

import java.util.Stack;

public class SortEventsGenerator extends PropertySortBaseVisitor {
    private final SortEvents sortEvents;
    private final Stack<Object> stack = new Stack<>();

    public SortEventsGenerator(SortEvents sortEvents) {
        this.sortEvents = sortEvents;
    }

    @Override
    public Object visitPropertyExpression(PropertySortParser.PropertyExpressionContext ctx) {
        Object result = super.visitPropertyExpression(ctx);
        this.sortEvents.sorted(ctx.IDENTIFIER().getText(), this.stack.isEmpty() ? SortEvents.Direction.ASC : (SortEvents.Direction) this.stack.pop());
        return result;
    }

    @Override
    public Object visitSortDirection(PropertySortParser.SortDirectionContext ctx) {
        if(ctx.ASC() != null) {
            this.stack.push(SortEvents.Direction.ASC);
        } else if(ctx.DESC() != null) {
            this.stack.push(SortEvents.Direction.DESC);
        } else {
            this.stack.push(SortEvents.Direction.ASC);
        }
        return super.visitSortDirection(ctx);
    }
}
