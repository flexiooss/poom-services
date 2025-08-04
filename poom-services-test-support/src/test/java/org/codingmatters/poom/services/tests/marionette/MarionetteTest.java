package org.codingmatters.poom.services.tests.marionette;

import org.codingmatters.poom.services.tests.marionette.internals.Call;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

class MarionetteTest {
    @Test
    void whenNoCallExpected__thenAssertionErrorOnCall() throws Exception {
        AssertionError e = assertThrows(AssertionError.class, () ->
                Marionette.of(TestInterface.class).component().unary(42)
        );
        assertThat(e.getMessage(), is("unexpected call Call{TestInterface#unary([42])}"));
    }

    @Test
    void givenAnyArgumentResultExpected__whenUnaryMethod__thenNextCallReturnsValueReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("421").whenAnyArgs().unary(null);

        assertThat(marionette.component().unary(421), is("421"));
    }

    @Test
    void givenAnyArgumentResultExpected__whenNAryMethod__thenNextCallReturnsValueReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        Object result = new Object();
        marionette.nextCallReturns(result).whenAnyArgs().nary(null, null);

        assertThat(marionette.component().nary("421", 421L), is(result));
    }

    @Test
    void givenAnyArgumentResultExpected__when0AryMethod__thenNextCallReturnsValueReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        Object result = new Object();
        marionette.nextCallReturns(result).whenAnyArgs().oary();

        assertThat(marionette.component().oary(), is(result));
    }

    @Test
    void givenAnyArgumentResultExpected__whenProcMethod__thenNextCallReturnsValueReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        Object result = new Object();
        marionette.nextCallReturns(result).whenAnyArgs().proc(null, null, null);

        marionette.component().proc("str", 12, new Object());
    }

    @Test
    void givenCheckedArgumentResultExpected__whenCalledWithArgument__thenNextCallReturnsValueReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("421").whenCheckedArgs().unary(421);

        assertThat(marionette.component().unary(421), is("421"));
    }

    @Test
    void whenThrowExpected__thenThrownThrowedOnCall() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallThrows(() -> new IOException("blabla")).whenAnyArgs().throwing();

        Throwable e = assertThrows(Throwable.class, () ->
                marionette.component().throwing()
        );
        assertThat(e.getMessage(), is("blabla"));
    }

    @Test
    void givenCheckedArgumentResultExpected__whenCalledWithArgument__thenAssertionErrorOnCall() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("421").whenCheckedArgs().unary(421);

        AssertionError e = assertThrows(AssertionError.class, () ->
                marionette.component().unary(422)
        );
        assertThat(e.getMessage(), is("argument expected : [421] but was : [422]"));
    }

    @Test
    void givenNextCallReturnsSet__whenUnary_andAssertLastCall_withCalledArguments__thenOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().unary(null);

        marionette.component().unary(18);

        marionette.assertLastCall().was().unary(18);
    }

    @Test
    void givenNextCallReturnsSet__whenNnary_andAssertLastCall_withCalledArguments__thenOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().nary(null, null);

        marionette.component().nary("str", 42L);

        marionette.assertLastCall().was().nary("str", 42L);
    }

    @Test
    void givenNextCallReturnsSet__when0nary_andAssertLastCall_withCalledArguments__thenOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().oary();

        marionette.component().oary();

        marionette.assertLastCall().was().oary();
    }

    @Test
    void givenNextCallReturnsSet__whenProc_andAssertLastCall_withCalledArguments__thenOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().proc(null, null, null);

        Object o = new Object();
        marionette.component().proc("str", 12, o);

        marionette.assertLastCall().was().proc("str", 12, o);
    }

    @Test
    void givenNextCallReturnsSet__whenUnary_andAssertLastCall_withOtherArguments__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().unary(null);

        marionette.component().unary(42);

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().unary(12));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#unary([12])}>\n     but: was <Call{TestInterface#unary([42])}>"));
    }

    @Test
    void givenNextCallReturnsSet__whenUnary_andAssertLastCall_noCall__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().unary(null);

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().unary(12));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#unary([12])}>\n     but: was null"));
    }

    @Test
    void givenNextCallReturnsSet__whenNnary_andAssertLastCall_withOtherArguments__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().nary(null, null);

        marionette.component().nary("other", 12L);

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().nary("str", 42L));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#nary([str, 42])}>\n     but: was <Call{TestInterface#nary([other, 12])}>"));
    }

    @Test
    void givenNextCallReturnsSet__whenNnary_andAssertLastCall_andNoCall__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().nary(null, null);

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().nary("str", 42L));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#nary([str, 42])}>\n     but: was null"));
    }

    @Test
    void givenNextCallReturnsSet__when0ary_andAssertLastCall_andNoCall__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns("yop").whenAnyArgs().oary();

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().oary());
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#oary([])}>\n     but: was null"));
    }

    @Test
    void givenNextCallReturnsSet__whenProc_andAssertLastCall_withOtherArguments__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns(null).whenAnyArgs().proc(null, null, null);

        marionette.component().proc("other", 12, "p");

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().proc("str", 42, "o"));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#proc([str, 42, o])}>\n     but: was <Call{TestInterface#proc([other, 12, p])}>"));
    }

    @Test
    void givenNextCallReturnsSet__whenProc_andAssertLastCall_withNoCall__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns(null).whenAnyArgs().proc(null, null, null);

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertLastCall().was().proc("str", 12, "o"));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#proc([str, 12, o])}>\n     but: was null"));
    }

    @Test
    void givenManyResultSet__whenManyCalls__thenExpectedResultReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        for (int i = 0; i < 100; i++) {
            marionette.nextCallReturns("res-" + i).whenAnyArgs().unary(null);
        }

        for (int i = 0; i < 100; i++) {
            assertThat(marionette.component().unary(i), is("res-" + i));
        }
    }

    @Test
    void givenManyResultSet__whenManyCalls__thenLastCallAssertionOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        for (int i = 0; i < 100; i++) {
            marionette.nextCallReturns("res-" + i).whenAnyArgs().unary(null);
        }

        for (int i = 0; i < 100; i++) {
            marionette.component().unary(i);
            marionette.assertLastCall().was().unary(i);
        }
    }

    @Test
    void givenManyResultSet_andManyCalls__whenNthCall__thenAssertionOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        for (int i = 0; i < 100; i++) {
            marionette.nextCallReturns("res-" + i).whenAnyArgs().unary(null);
        }

        for (int i = 0; i < 100; i++) {
            marionette.component().unary(i);
        }

        for (int i = 0; i < 100; i++) {
            marionette.assertNthCall(i).was().unary(i);
        }
    }

    @Test
    void givenManyResultSet_andManyCalls__whenNthCallAssertionAfterCount__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        for (int i = 0; i < 100; i++) {
            marionette.nextCallReturns("res-" + i).whenAnyArgs().unary(null);
        }
        for (int i = 0; i < 100; i++) {
            marionette.component().unary(i);
        }

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertNthCall(100).was().unary(12));
        assertThat(e.getMessage(), is("\nExpected: is <Call{TestInterface#unary([12])}>\n     but: was null"));
    }

    @Test
    void whenCallsForDifferentMethods__thenEachMethodHasOwnLastCall() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().nary(null, null);

        marionette.component().unary(12);
        marionette.component().nary("plop", 42L);

        marionette.assertLastCall().was().unary(12);
        marionette.assertLastCall().was().nary("plop", 42L);
    }

    @Test
    void whenCallsForDifferentMethods__thenEachMethodHasOwnNthCall() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().nary(null, null);
        marionette.nextCallReturns("plop").whenAnyArgs().nary(null, null);
        marionette.nextCallReturns("plop").whenAnyArgs().nary(null, null);

        marionette.component().unary(12);
        marionette.component().nary("plop", 42L);
        marionette.component().unary(12);
        marionette.component().nary("plop", 42L);
        marionette.component().unary(12);
        marionette.component().nary("plop", 42L);

        for (int i = 0; i < 3; i++) {
            marionette.assertNthCall(i).was().unary(12);
            marionette.assertNthCall(i).was().nary("plop", 42L);
        }

    }

    @Test
    void giveDefaultCallDefined__whenMultipleCalls__thenOk() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.defaultCallReturns("plop").whenAnyArgs().unary(null);

        for (int i = 0; i < 100;  i++) {
            marionette.component().unary(12);
        }
    }

    @Test
    void givenManyOneCallDefined__whenMultipleCalls__thenManyPlusOneFails() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);

        for (int i = 0; i < 3;  i++) {
            marionette.component().unary(12);
        }

        assertThrows(AssertionError.class, () -> marionette.component().unary(12));
    }

    @Test
    void givenManyOneCallDefined__whenMultipleCalls_andDefaultCallDefined__thenManyPlusSucceeds() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);
        marionette.nextCallReturns("plop").whenAnyArgs().unary(null);

        marionette.defaultCallReturns("plop").whenAnyArgs().unary(null);

        for (int i = 0; i < 3;  i++) {
            marionette.component().unary(12);
        }
        marionette.component().unary(12);
    }

    @Test
    void givenNoReturnValue__whenNotCalled_andAssertingNeverCalled__thenOK() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);

        marionette.assertNeverCalled().unary(12);
    }

    @Test
    void givenDefaultRetunValue__whenNotCalled_andAssertingNeverCalled__thenOK() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.defaultCallReturns("plop").whenAnyArgs().oary();

        marionette.assertNeverCalled().oary();
    }

    @Test
    void givenDefaultRetunValue__whenCalled_andAssertingNeverCalled__thenAssertionError() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.defaultCallReturns("plop").whenAnyArgs().oary();

        marionette.component().oary();

        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertNeverCalled().oary());
        assertThat(e.getMessage(), is("was not expecting to be called : Call{TestInterface#oary([])}"));
    }

    @Test
    void givenDefaultRetunValue__whenCalledNTimes_andAssertingOnCalledTimes__thenSucceedsOnSameCount_andFailsOnDifferentCount() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.defaultCallReturns("plop").whenAnyArgs().oary();

        for (int i = 0; i < 124; i++) {
            marionette.component().oary();
        }

        marionette.assertCalledTimes(124).oary();
        AssertionError e = assertThrows(AssertionError.class, () -> marionette.assertCalledTimes(12).oary());
        assertThat(e.getMessage(), is("expected 12 calls of Call{TestInterface#oary([])} but was 124"));
    }

    @Test
    void whenPrimitiveTypeArg__then() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns(null).whenAnyArgs().primitiveTypeArg(false);
    }

    @Test
    void whenPrimitiveTypeResult__then() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns(Boolean.TRUE).whenAnyArgs().primitiveBooleanTypeResult();
    }

    @Test
    void givenNextCallReturnsSet__whenProc_andGetLastCall__thenCallReturned() throws Exception {
        Marionette<TestInterface> marionette = Marionette.of(TestInterface.class);
        marionette.nextCallReturns(null).whenAnyArgs().proc(null, null, null);

        marionette.component().proc("other", 12, "p");

        Call actual = marionette.assertLastCall().getFor("proc", String.class, Integer.class, String.class);
        assertThat(actual.arguments(), is(arrayContaining("other", 12, "p")));
    }
}