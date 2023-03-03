package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.PropertyQueryParser;
import org.codingmatters.test.Simple;
import org.codingmatters.test.WithObject;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class ReflectFilterEventsTest {

    @Test
    public void givenValuesAreNumbers__whenSimpleGratherThanExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a > 12").build());

        assertTrue(events.result().test(Simple.builder().a(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(2L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleGratherThanOrEqualsExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a >= 12").build());

        assertTrue(events.result().test(Simple.builder().a(42L).build()));
        assertTrue(events.result().test(Simple.builder().a(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(2L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleGratherThanPropertyExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a > b").build());

        assertTrue(events.result().test(Simple.builder().a(42L).b(40L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(2L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleGratherThanOrEqualsPropertyExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a >= b").build());

        assertTrue(events.result().test(Simple.builder().a(42L).b(40L).build()));
        assertTrue(events.result().test(Simple.builder().a(12L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(2L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleLowerThanExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a < 12").build());

        assertTrue(events.result().test(Simple.builder().a(8L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleLowerThanOrEqualsExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a <= 12").build());

        assertTrue(events.result().test(Simple.builder().a(8L).build()));
        assertTrue(events.result().test(Simple.builder().a(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleLowerThanPropertyExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a < b").build());

        assertTrue(events.result().test(Simple.builder().a(40L).b(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleLowerThanOrEqualsPropertyExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a <= b").build());

        assertTrue(events.result().test(Simple.builder().a(40L).b(42L).build()));
        assertTrue(events.result().test(Simple.builder().a(12L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleEqualsExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a == 12").build());

        assertTrue(events.result().test(Simple.builder().a(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(2L).build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreNumbers__whenSimpleEqualsPropertyExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a == b").build());

        assertTrue(events.result().test(Simple.builder().a(42L).b(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).b(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).b(12L).build()));
        assertFalse(events.result().test(Simple.builder().b(12L).build()));
        assertFalse(events.result().test(Simple.builder().a(42L).build()));
        assertTrue(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleStartsWith__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c starts with 'prefix'").build());

        assertTrue(events.result().test(Simple.builder().c("prefixsuffix").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").build()));
        assertFalse(events.result().test(Simple.builder().c("").build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleStartsPropertyWith__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c starts with d").build());

        assertTrue(events.result().test(Simple.builder().c("prefixsuffix").d("prefix").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").d("prefix").build()));
        assertFalse(events.result().test(Simple.builder().c("").d("prefix").build()));
        assertFalse(events.result().test(Simple.builder().d("prefix").build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleEndsWith__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c ends with 'suffix'").build());

        assertTrue(events.result().test(Simple.builder().c("prefixsuffix").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").build()));
        assertFalse(events.result().test(Simple.builder().c("").build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleEndsWithProperty__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c ends with d").build());

        assertTrue(events.result().test(Simple.builder().c("prefixsuffix").d("suffix").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").d("suffix").build()));
        assertFalse(events.result().test(Simple.builder().c("").d("suffix").build()));
        assertFalse(events.result().test(Simple.builder().d("suffix").build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleContains__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c contains 'sub'").build());

        assertTrue(events.result().test(Simple.builder().c("stringsithsubinit").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").build()));
        assertFalse(events.result().test(Simple.builder().c("").build()));
        assertFalse(events.result().test(Simple.builder().build()));
    }

    @Test
    public void givenValuesAreStrings__whenSimpleContainsProperty__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("c contains d").build());

        assertTrue(events.result().test(Simple.builder().c("stringsithsubinit").d("sub").build()));
        assertFalse(events.result().test(Simple.builder().c("notquite").d("sub").build()));
        assertFalse(events.result().test(Simple.builder().c("").d("sub").build()));
        assertFalse(events.result().test(Simple.builder().d("sub").build()));
    }

    @Test
    public void whenAndExpression__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a == 12 && b == 42").build());

        assertTrue(events.result().test(Simple.builder().a(12L).b(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(11L).b(42L).build()));
        assertFalse(events.result().test(Simple.builder().a(12L).b(41L).build()));
    }

    @Test
    public void whenOr__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("a == 12 || b == 42").build());

        assertTrue(events.result().test(Simple.builder().a(12L).b(42L).build()));
        assertTrue(events.result().test(Simple.builder().a(11L).b(42L).build()));
        assertTrue(events.result().test(Simple.builder().a(12L).b(41L).build()));
        assertFalse(events.result().test(Simple.builder().a(11L).b(41L).build()));
    }

    @Test
    public void whenNot__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("! a == 12").build());

        assertFalse(events.result().test(Simple.builder().a(12L).build()));
        assertTrue(events.result().test(Simple.builder().a(11L).build()));
    }

    @Test
    public void givenObjectNestedObject__whenNestedEquals__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<WithObject> events = new ReflectFilterEvents<>(WithObject.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("p.a == 'toto'").build());

        assertTrue(events.result().test(WithObject.builder().p(ObjectValue.builder().property("a", PropertyValue.builder().stringValue("toto")).build()).build()));
        assertFalse(events.result().test(WithObject.builder().p(ObjectValue.builder().property("a", PropertyValue.builder().stringValue("tutu")).build()).build()));
    }

    @Test
    public void givenSimpleObject__whenDatetimeEquals__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("aDate == 1970-01-02T03:04:05.678").build());

        assertTrue(events.result().test(Simple.builder().aDate(LocalDateTime.of(1970, 1, 2, 3, 4, 5, 678000000)).build()));
        assertFalse(events.result().test(Simple.builder().aDate(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 678000000)).build()));
    }

    @Test
    public void givenSimpleObject__whenDatetimeGreatherThan__thenPredicateIsOk() throws Exception {
        ReflectFilterEvents<Simple> events = new ReflectFilterEvents<>(Simple.class);
        PropertyQueryParser.builder().build(events).parse(PropertyQuery.builder().filter("aDate > 2001-01-02T03:04:05.678").build());

        assertFalse(events.result().test(Simple.builder().aDate(LocalDateTime.of(1970, 1, 2, 3, 4, 5, 678000000)).build()));
        assertTrue(events.result().test(Simple.builder().aDate(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 678000000)).build()));
    }
}