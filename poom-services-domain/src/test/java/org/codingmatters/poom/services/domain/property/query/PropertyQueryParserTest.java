package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class PropertyQueryParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenExpressionsWithProperties__whenLHSPropertyValidatorIsDefined__thenValidatorIsCalledOnlyForLHSProperties() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();
        List<String> props = Collections.synchronizedList(new LinkedList<>());

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(s -> props.add(s) || true)
                .build(FilterEvents.noop()).parse(query);

        assertThat(props, contains("l1", "l2", "l3"));
    }

    @Test
    public void givenExpressionsWithProperties__whenLHSPropertyValidatorIsDefined_andOnePropertyIsInalid__thenParserExceptionRaised() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();

        thrown.expect(InvalidPropertyException.class);
        thrown.expectMessage("invalid left hand side properties : l2");

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(s -> ! "l2".equals(s))
                .build(FilterEvents.noop()).parse(query);
    }

    @Test
    public void givenExpressionsWithProperties__whenRHSPropertyValidatorIsDefined__thenValidatorIsCalledOnlyForRHSProperties() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();
        List<String> props = Collections.synchronizedList(new LinkedList<>());

        PropertyQueryParser.builder()
                .rightHandSidePropertyValidator(s -> props.add(s) || true)
                .build(FilterEvents.noop()).parse(query);

        assertThat(props, contains("r1", "r2", "r3"));
    }

    @Test
    public void givenExpressionsHaveLHS_andRHSProperties__whenRHSPropertyValidatorIsDefined_andOnePropertyIsInvalid__thenParserExceptionRaised() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 < r1 || l2 <r2) && l3 > r3").build();

        thrown.expect(InvalidPropertyException.class);
        thrown.expectMessage("invalid right hand side properties : r2");

        PropertyQueryParser.builder()
                .rightHandSidePropertyValidator(s -> ! "r2".equals(s))
                .build(FilterEvents.noop()).parse(query);
    }

    @Test
    public void givenEventsImplemented__whenParsing__thenExpressionIsParsed() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 > 1 || l2 >2) && l3 > 3 && ! l4 > 4").build();

        StackedFilterEvents<String> events = new StackedFilterEvents<String>() {
            @Override
            public Void graterThan(String left, Object right) throws FilterEventError {
                this.push(left + " > " + right);
                return null;
            }

            @Override
            public Void not() throws FilterEventError {
                String result = "not(" + this.pop() + ")";
                this.push(result);
                return null;
            }

            @Override
            public Void and() throws FilterEventError {
                String result = "and(";
                for (String s : this.reversedPopAll()) {
                    result += s + ", ";
                }
                result += ")";
                this.push(result);
                return null;
            }

            @Override
            public Void or() throws FilterEventError {
                String result = "or(";
                for (String s : this.reversedPopAll()) {
                    result += s + ", ";
                }
                result += ")";
                this.push(result);
                return null;
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(events.result(), is("and(and(or(l1 > 1, l2 > 2, ), l3 > 3, ), not(l4 > 4), )"));
    }
}