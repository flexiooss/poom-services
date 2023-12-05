package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventException;
import org.codingmatters.poom.services.domain.property.query.events.SortEventError;
import org.codingmatters.poom.services.domain.property.query.events.SortEventException;
import org.codingmatters.poom.services.domain.property.query.validation.InvalidPropertyException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.rules.ExpectedException;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;


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
    public void givenFilterEventsImplemented__whenParsing__thenExpressionIsParsed() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("(l1 > 1 || l2 >2) && l3 > 3 && ! l4 > 4").build();

        StackedFilterEvents<String> events = new StackedFilterEvents<String>("") {
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

    @Test
    public void given__when__thenIsNull() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 == null").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNull(String property) throws FilterEventError {
                parsed.add(property + " is null");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is null"));
    }

    @Test
    public void given__when__thenIsNotNull() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != null").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotNull(String property) throws FilterEventError {
                parsed.add(property + " is not null");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not null"));
    }

    @Test
    public void given__when__thenIsEmpty() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 is empty").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isEmpty(String property) throws FilterEventError {
                parsed.add(property + " is empty property");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is empty property"));
    }

    @Test
    public void given__when__thenIsNotEmpty() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 is not empty").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotEmpty(String property) throws FilterEventError {
                parsed.add(property + " is not empty property");
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not empty property"));
    }

    @Test
    public void given__when__thenIsNotEqualTo() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != 12").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotEquals(String left, Object right) throws FilterEventError {
                parsed.add(left + " is not equal to " + right);
                return parsed.get(parsed.size() - 1);
            }

        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not equal to 12"));
    }

    @Test
    public void given__when__thenIsNotEqualToProperty() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 != l2").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String isNotEqualsProperty(String left, String right) throws FilterEventError {
                parsed.add(left + " is not equal to property " + right);
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 is not equal to property l2"));
    }

    @Test
    public void given__whenInWithInts__thenIN() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 in (1, 2, 3)").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String in(String left, List<Object> right) throws FilterEventError {
                List classes = new LinkedList();
                for (Object o : right) {
                    classes.add(o.getClass().getSimpleName());
                }

                parsed.add(String.format("%s in %s (%s)", left, right, classes));
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        System.out.println(parsed);
        assertThat(parsed, contains("l1 in [1, 2, 3] ([Long, Long, Long])"));
    }

    @Test
    public void given__whenInWithStrings__thenIN() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 in ('1', '2', '3')").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String in(String left, List<Object> right) throws FilterEventError {
                List classes = new LinkedList();
                for (Object o : right) {
                    classes.add(o.getClass().getSimpleName());
                }

                parsed.add(String.format("%s in %s (%s)", left, right, classes));
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 in [1, 2, 3] ([String, String, String])"));
    }

    @Test
    public void given__whenContainsAnyWithStrings__thenCONTAINS() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 contains any ('1', '2', '3')").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String containsAny(String left, List<Object> right) throws FilterEventError {
                List classes = new LinkedList();
                for (Object o : right) {
                    classes.add(o.getClass().getSimpleName());
                }

                parsed.add(String.format("%s contains any %s (%s)", left, right, classes));
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 contains any [1, 2, 3] ([String, String, String])"));
    }

    @Test
    public void whenStartsWithAnyWithStrings() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 starts with any ('1', '2', '3')").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String startsWithAny(String left, List<Object> right) throws FilterEventError {
                List classes = new LinkedList();
                for (Object o : right) {
                    classes.add(o.getClass().getSimpleName());
                }

                parsed.add(String.format("%s starts with any %s (%s)", left, right, classes));
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 starts with any [1, 2, 3] ([String, String, String])"));
    }

    @Test
    public void whenEndsWithAnyWithStrings() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 ends with any ('1', '2', '3')").build();

        List<String> parsed = new LinkedList<>();

        FilterEvents<String> events = new FilterEvents<String>() {
            @Override
            public String endsWithAny(String left, List<Object> right) throws FilterEventError {
                List classes = new LinkedList();
                for (Object o : right) {
                    classes.add(o.getClass().getSimpleName());
                }

                parsed.add(String.format("%s ends with any %s (%s)", left, right, classes));
                return parsed.get(parsed.size() - 1);
            }
        };

        PropertyQueryParser.builder()
                .build(events)
                .parse(query);

        assertThat(parsed, contains("l1 ends with any [1, 2, 3] ([String, String, String])"));
    }

    @Test
    public void given__whenInWithEmptyList__thenFilterEventException() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 in ()").build();

        FilterEvents<String> events = new FilterEvents<String>() {
        };

        thrown.expect(FilterEventException.class);
        PropertyQueryParser.builder()
                .build(events)
                .parse(query);
    }

    @Test
    public void given__whenContainsAnyWithEmptyList__thenFilterEventException() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 contains any ()").build();

        FilterEvents<String> events = new FilterEvents<String>() {
        };

        thrown.expect(FilterEventException.class);
        PropertyQueryParser.builder()
                .build(events)
                .parse(query);
    }


    @Test
    public void givenExpressionsWithProperties__whenAPropertyStartsWithADigit__thenParsesOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("1prop == 12").build();
        List<String> props = Collections.synchronizedList(new LinkedList<>());

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(s -> props.add(s) || true)
                .build(FilterEvents.noop()).parse(query);

        assertThat(props, contains("1prop"));
    }

    @Test
    public void givenSortEventImplements__whenPropertyAreValid__thenEventsAreGenerated() throws Exception {
        PropertyQuery query = PropertyQuery.builder()
                .sort("p1 asc, p2, p3 desc")
                .build();

        StringBuilder result = new StringBuilder("|");
        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new SortEvents() {
                    @Override
                    public Object sorted(String property, Direction direction) throws SortEventError {
                        result.append(property).append(" ").append(direction.name()).append("|");
                        return null;
                    }
                }).parse(query);

        assertThat(result.toString(), is("|p1 ASC|p2 ASC|p3 DESC|"));
    }

    @Test
    public void givenSortEventImplements__whenPropertyAreInvalid__thenInvalidPropertyException() throws Exception {
        PropertyQuery query = PropertyQuery.builder()
                .sort("p1 asc, p2, p3 desc")
                .build();

        thrown.expect(InvalidPropertyException.class);

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> false)
                .build().parse(query);
    }

    @Test
    public void whenDatetimeLiteral__thenParsedToLocalDatetime() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00.123").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(LocalDateTime.class));
    }

    @Test
    public void whenDatetimeLiteral_andNoSecondFraction__thenParsedToLocalDatetime() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(LocalDateTime.class));
    }

    @Test
    public void whenUtcDatetimeLitteral__thenParsedZonedDatetimeWithUTCOffset() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00.123Z").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(ZonedDateTime.class));
        assertThat(((ZonedDateTime)parsed.get()).getOffset(), is(ZoneOffset.UTC));
    }

    @Test
    public void whenUtcDatetimeLitteral_andNoSecondFraction__thenParsedZonedDatetimeWithUTCOffset() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00Z").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(ZonedDateTime.class));
        assertThat(((ZonedDateTime)parsed.get()).getOffset(), is(ZoneOffset.UTC));
    }

    @Test
    public void whenZonedDatetimeLitteral__thenParsedToZonedDatetimeWithGivenOffset() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00.123+03:30").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(ZonedDateTime.class));
        assertThat(((ZonedDateTime)parsed.get()).getOffset(), is(ZoneOffset.ofHoursMinutes(3,30)));
    }

    @Test
    public void whenZonedDatetimeLitteral_andNoSecongFraction__thenParsedToZonedDatetimeWithGivenOffset() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00+03:30").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(ZonedDateTime.class));
        assertThat(((ZonedDateTime)parsed.get()).getOffset(), is(ZoneOffset.ofHoursMinutes(3,30)));
    }

    @Test
    public void whenZonedDatetimeLitteralWithFullNanos__thenParsedToZonedDatetimeWithGivenOffset() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06T12:06:00.123456789+03:30").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(ZonedDateTime.class));
        assertThat(((ZonedDateTime)parsed.get()).getOffset(), is(ZoneOffset.ofHoursMinutes(3,30)));
    }

    @Test
    public void date() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 2019-05-06").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(LocalDate.class));
    }

    @Test
    public void time() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 12:06:00.123").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(LocalTime.class));
    }

    @Test
    public void time_andNoSecondFraction() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("a == 12:06:00").build();
        StringBuilder result = new StringBuilder("|");

        AtomicReference<Object> parsed = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object isEquals(String left, Object right) throws FilterEventError {
                        parsed.set(right);
                        return null;
                    }
                }).parse(query);


        assertThat(parsed.get(), is(notNullValue()));
        assertThat(parsed.get(), isA(LocalTime.class));
    }

    @Test
    public void whenSortSyntaxError__thenThrowsWithReport() throws Exception {
        PropertyQuery query = PropertyQuery.builder().sort("&é'(-è_ç").build();

        thrown.expect(SortEventException.class);
        thrown.expectMessage("4 syntax errors found while parsing sort \"&é'(-è_ç\" : [line 1:0 token recognition error at: '&', line 1:1 token recognition error at: 'é', line 1:2 token recognition error at: ''(-è_ç', line 1:8 mismatched input '<EOF>' expecting IDENTIFIER]");
        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {}).parse(query);
    }


    @Test
    public void whenLexerErrorInFilter__thenThrowsWithReport() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("l1 !== 12").build();

        thrown.expect(FilterEventException.class);
        thrown.expectMessage("1 syntax error found while parsing filter \"l1 !== 12\" : [line 1:5 token recognition error at: '= ']");
        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {}).parse(query);
    }

    @Test
    public void whenFilterWithSyntaxError__thenThrowsWithReport() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("expiresAt < 2020-02-25T06:27:38.132456789123456789").build();

        thrown.expect(FilterEventException.class);
        thrown.expectMessage("1 syntax error found while parsing filter \"expiresAt < 2020-02-25T06:27:38.132456789123456789\" : [line 1:41 extraneous input '123456789' expecting <EOF>]");
        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {}).parse(query);
    }

    @Test
    public void givenDateTimeLiteral__whenNoMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalDateTime.class));
    }

    @Test
    public void givenDateTimeLiteral__whenOneDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.5").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalDateTime.class));
    }

    @Test
    public void givenDateTimeLiteral__whenTwoDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.52").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalDateTime.class));
    }

    @Test
    public void givenDateTimeLiteral__whenThreeDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.521").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalDateTime.class));
    }

    @Test
    public void givenUTCDateTimeLiteral__whenNoMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03Z").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenUTCDateTimeLiteral__whenOneDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.5Z").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenUTCDateTimeLiteral__whenTwoDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.52Z").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenUTCDateTimeLiteral__whenThreeDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.521Z").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenZonedDateTimeLiteral__whenNoMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03+03:30").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenZonedDateTimeLiteral__whenOneDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.5+03:30").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenZonedDateTimeLiteral__whenTwoDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.52+03:30").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }

    @Test
    public void givenZonedDateTimeLiteral__whenThreeDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 2021-11-24T10:00:03.521+03:30").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(ZonedDateTime.class));
    }



    @Test
    public void givenTimeLiteral__whenNoMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 10:00:03").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalTime.class));
    }

    @Test
    public void givenTimeLiteral__whenOneDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 10:00:03.5").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalTime.class));
    }

    @Test
    public void givenTimeLiteral__whenTwoDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 10:00:03.52").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalTime.class));
    }

    @Test
    public void givenTimeLiteral__whenThreeDigitMillis__thenParsedOk() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("618c033a6a63b9190f5419b9 >= 10:00:03.521").build();
        AtomicReference<Object> value = new AtomicReference<>();

        PropertyQueryParser.builder()
                .leftHandSidePropertyValidator(property -> true)
                .build(new FilterEvents() {
                    @Override
                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
                        value.set(right);
                        return null;
                    }
                }).parse(query);

        assertThat(value.get(), isA(LocalTime.class));
    }

    @Test
    public void givenChatelainQuery__when__then() throws Exception {
        PropertyQuery query = PropertyQuery.builder().filter("key in('5')&&((65686bcb538f3814740eb7ac contains any ('6560a1bca7df7a051a690433')))").build();
        AtomicReference<Object> value = new AtomicReference<>();

//        PropertyQueryParser.builder()
//                .leftHandSidePropertyValidator(property -> true)
//                .build(new FilterEvents() {
//                    @Override
//                    public Object graterThanOrEquals(String left, Object right) throws FilterEventError {
//                        value.set(right);
//                        return null;
//                    }
//                }).parse(query);
//
//        assertThat(value.get(), isA(LocalTime.class));
    }

}
