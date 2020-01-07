package org.codingmatters.poom.services.domain.property.query;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.codingmatters.poom.services.domain.property.query.events.*;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterLexer;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterParser;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortLexer;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortParser;
import org.codingmatters.poom.services.domain.property.query.validation.FilterPropertyValidation;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.codingmatters.poom.services.domain.property.query.validation.SortPropertyValidation;

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
            return this.build(events, SortEvents.noop());
        }

        public PropertyQueryParser build(SortEvents events) {
            return this.build(FilterEvents.noop(), events);
        }
        public PropertyQueryParser build(FilterEvents filterEvents, SortEvents sortEvents) {
            return new PropertyQueryParser(
                    filterEvents,
                    sortEvents,
                    this.leftHandSidePropertyValidator,
                    this.rightHandSidePropertyValidator);
        }

        public PropertyQueryParser build() {
            return this.build(FilterEvents.noop(), SortEvents.noop());
        }
    }

    private final FilterEvents filterEvents;
    private final SortEvents sortEvents;
    private final Predicate<String> leftHandSidePropertyValidator;
    private final Predicate<String> rightHandSidePropertyValidator;

    private PropertyQueryParser(FilterEvents filterEvents, SortEvents sortEvents, Predicate<String> leftHandSidePropertyValidator, Predicate<String> rightHandSidePropertyValidator) {
        this.filterEvents = filterEvents;
        this.sortEvents = sortEvents;
        this.leftHandSidePropertyValidator = leftHandSidePropertyValidator != null ? leftHandSidePropertyValidator : s -> true;
        this.rightHandSidePropertyValidator = rightHandSidePropertyValidator != null ? rightHandSidePropertyValidator : s -> true;
    }

    public void parse(PropertyQuery query) throws InvalidPropertyException, FilterEventException, SortEventException {
        if(query.filter() != null) {
            this.parseFilter(query.filter());
        }
        if(query.sort() != null) {
            this.parseSort(query.sort());
        }
    }

    public void parseFilter(String filter) throws InvalidPropertyException, FilterEventException {
        CodePointCharStream input = CharStreams.fromString(filter);
        CommonTokenStream tokens = new CommonTokenStream(new PropertyFilterLexer(input));
        PropertyFilterParser parser = new PropertyFilterParser(tokens);

        FilterPropertyValidation propertyValidation = new FilterPropertyValidation(this.leftHandSidePropertyValidator, this.rightHandSidePropertyValidator);
        propertyValidation.visit(parser.criterion());
        propertyValidation.isValid();
        try {
            tokens.seek(0);
            new FilterEventsGenerator(this.filterEvents).visit(parser.criterion());
        } catch (FilterEventError e) {
            throw new FilterEventException(e);
        }
    }

    private void parseSort(String sort) throws SortEventException, InvalidPropertyException {
        CodePointCharStream input = CharStreams.fromString(sort);
        CommonTokenStream tokens = new CommonTokenStream(new PropertySortLexer(input));
        PropertySortParser parser = new PropertySortParser(tokens);

        SortPropertyValidation propertyValidation = new SortPropertyValidation(this.leftHandSidePropertyValidator);
        propertyValidation.visit(parser.sortExpression());
        propertyValidation.isValid();
        try {
            tokens.seek(0);
            new SortEventsGenerator(this.sortEvents).visit(parser.sortExpression());
        } catch (SortEventError e) {
            throw new SortEventException(e);
        }
    }

}
