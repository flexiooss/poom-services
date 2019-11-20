package org.codingmatters.poom.services.domain.property.query;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.codingmatters.poom.services.domain.property.query.events.EventsGenerator;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventException;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterBaseListener;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterLexer;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;
import org.codingmatters.poom.services.domain.property.query.validation.FilterPropertyValidation;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;

import java.util.function.Predicate;

public class PropertyQueryParser {

    static public Builder builder() {
        return new Builder();
    }

    static public class Builder {

        private Predicate<String> leftHandSidePropertyValidator;
        private Predicate<String> rightHandSidePropertyValidator;

        public Builder leftHandSidePropertyValidator(Predicate<String> propertyValidator) {
            this.leftHandSidePropertyValidator = propertyValidator;
            return this;
        }

        public Builder rightHandSidePropertyValidator(Predicate<String> propertyValidator) {
            this.rightHandSidePropertyValidator = propertyValidator;
            return this;
        }

        public PropertyQueryParser build(FilterEvents events) {
            return new PropertyQueryParser(
                    events,
                    this.leftHandSidePropertyValidator,
                    this.rightHandSidePropertyValidator);
        }
    }

    private final FilterEvents events;
    private final Predicate<String> leftHandSidePropertyValidator;
    private final Predicate<String> rightHandSidePropertyValidator;

    private PropertyQueryParser(FilterEvents events, Predicate<String> leftHandSidePropertyValidator, Predicate<String> rightHandSidePropertyValidator) {
        this.events = events;
        this.leftHandSidePropertyValidator = leftHandSidePropertyValidator != null ? leftHandSidePropertyValidator : s -> true;
        this.rightHandSidePropertyValidator = rightHandSidePropertyValidator != null ? rightHandSidePropertyValidator : s -> true;
    }

    public void parse(PropertyQuery query) throws InvalidPropertyException, FilterEventException {
        CodePointCharStream input = CharStreams.fromString(query.filter());
        CommonTokenStream tokens = new CommonTokenStream(new PropertyFilterLexer(input));
        PropertyFilterParser parser = new PropertyFilterParser(tokens);

        FilterPropertyValidation filterPropertyValidation = new FilterPropertyValidation(this.leftHandSidePropertyValidator, this.rightHandSidePropertyValidator);
        filterPropertyValidation.visit(parser.criterion());
        filterPropertyValidation.isValid();

        try {
            tokens.seek(0);
            new EventsGenerator(this.events).visit(parser.criterion());
        } catch (FilterEventError e) {
            throw new FilterEventException(e);
        }
    }
}
