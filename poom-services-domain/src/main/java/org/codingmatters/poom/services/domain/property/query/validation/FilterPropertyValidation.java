package org.codingmatters.poom.services.domain.property.query.validation;


import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterBaseVisitor;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterPropertyValidation extends PropertyFilterBaseVisitor {
    private final Predicate<String> leftHandSidePropertyValidator;
    private final Predicate<String> rightHandSidePropertyValidator;

    private final List<String> invalidLhs = new LinkedList<>();
    private final List<String> invalidRhs = new LinkedList<>();

    public FilterPropertyValidation(Predicate<String> leftHandSidePropertyValidator, Predicate<String> rightHandSidePropertyValidator) {
        this.leftHandSidePropertyValidator = leftHandSidePropertyValidator;
        this.rightHandSidePropertyValidator = rightHandSidePropertyValidator;
    }


    @Override
    public Object visitComparison(PropertyFilterParser.ComparisonContext ctx) {
        if(! this.leftHandSidePropertyValidator.test(ctx.IDENTIFIER().getText())) {
            this.invalidLhs.add(ctx.IDENTIFIER().getText());
        }
        return super.visitComparison(ctx);
    }

    @Override
    public Object visitPropertyOperand(PropertyFilterParser.PropertyOperandContext ctx) {
        if(! this.rightHandSidePropertyValidator.test(ctx.IDENTIFIER().getText())) {
            this.invalidRhs.add(ctx.IDENTIFIER().getText());
        }
        return super.visitPropertyOperand(ctx);
    }

    public void isValid() throws InvalidPropertyException {
        StringBuilder message = new StringBuilder();
        if(! this.invalidLhs.isEmpty()) {
            message.append("invalid left hand side properties : " + this.invalidLhs.stream().collect(Collectors.joining(", ")));
            if(! this.invalidRhs.isEmpty()) {
                message.append(" ; ");
            }
        }
        if(! this.invalidRhs.isEmpty()) {
            message.append("invalid right hand side properties : " + this.invalidRhs.stream().collect(Collectors.joining(", ")));
        }

        if(! (this.invalidLhs.isEmpty() && this.invalidRhs.isEmpty())) {
            throw new InvalidPropertyException(message.toString());
        }
    }
}
