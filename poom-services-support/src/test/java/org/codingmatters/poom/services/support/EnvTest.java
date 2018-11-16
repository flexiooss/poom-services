package org.codingmatters.poom.services.support;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

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

}