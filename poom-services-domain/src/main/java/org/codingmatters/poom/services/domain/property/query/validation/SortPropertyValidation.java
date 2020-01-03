package org.codingmatters.poom.services.domain.property.query.validation;

import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortParser;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SortPropertyValidation extends PropertySortBaseVisitor {
    private final Predicate<String> propertyValidator;
    private final List<String> invalid = new LinkedList<>();

    public SortPropertyValidation(Predicate<String> propertyValidator) {
        this.propertyValidator = propertyValidator;
    }

    @Override
    public Object visitPropertyExpression(PropertySortParser.PropertyExpressionContext ctx) {
        if(! this.propertyValidator.test(ctx.IDENTIFIER().getText())) {
            this.invalid.add(ctx.IDENTIFIER().getText());
        }
        return super.visitPropertyExpression(ctx);
    }

    public void isValid() throws InvalidPropertyException {
        StringBuilder message = new StringBuilder();
        if(! this.invalid.isEmpty()) {
            message.append("invalid left hand side properties : " + this.invalid.stream().collect(Collectors.joining(", ")));
        }

        if(! (this.invalid.isEmpty())) {
            throw new InvalidPropertyException(message.toString());
        }
    }
}
