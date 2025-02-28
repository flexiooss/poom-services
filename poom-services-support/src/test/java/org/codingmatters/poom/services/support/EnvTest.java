package org.codingmatters.poom.services.support;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class EnvTest {

    @Test
    public void givenParsingList__whenNoSeparator__thenListAsOneElement() {
        assertThat(new Env.Var("abc").asList(","), contains("abc"));
    }

    @Test
    public void givenParsingList__whenCleanList__thenAllElementsReturned() {
        assertThat(new Env.Var("a,b,c").asList(","), contains("a", "b", "c"));
    }

    @Test
    public void givenParsingList__whenEndsWithSeparator__thenNoEmptyElement() {
        assertThat(new Env.Var("a,b,").asList(","), contains("a", "b"));
    }

    @Test
    public void givenParsingList__whenStartsWithSeparator__thenNoEmptyElement() {
        assertThat(new Env.Var(",b,c").asList(","), contains("b", "c"));
    }

    @Test
    public void givenParsingList__whenSeparatorDuplicated__thenNoEmptyElement() {
        assertThat(new Env.Var("a,,c").asList(","), contains("a", "c"));
    }

    @Test
    public void givenParsingList__whenSeparatorDuplicatedMultipleTimes__thenNoEmptyElement() {
        assertThat(new Env.Var("a,,,,,,,,,c").asList(","), contains("a", "c"));
    }

    @Test
    public void testBoolean() {
        assertThat(new Env.Var("true").asBoolean(), is(true));
        assertThat(new Env.Var("TRUE").asBoolean(), is(true));
        assertThat(new Env.Var("True").asBoolean(), is(true));
        assertThat(new Env.Var("TrUe").asBoolean(), is(true));
        assertThat(new Env.Var("false").asBoolean(), is(false));
        assertThat(new Env.Var(null).asBoolean(), is(false));
        assertThat(new Env.Var("").asBoolean(), is(false));
        assertThat(new Env.Var("plok").asBoolean(), is(false));
    }
}