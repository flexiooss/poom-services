package org.codingmatters.poom.i18n.spec.gen;

import org.codingmatters.poom.i18n.bundle.spec.descriptors.ArgSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.ValueList;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class BundleSpecValidatorTest {

    public static final BundleSpec MINIMAL_SPEC = BundleSpec.builder()
            .name("test")
            .defaultLocale("fr-FR")
            .build();

    @Test
    public void givenValidatingBundle__whenNoName__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withName(null)),
                is(BundleSpecValidator.invalid("must have a name"))
        );
    }

    @Test
    public void givenValidatingBundle__whenNoDefaultLocale__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale(null)),
                is(BundleSpecValidator.invalid("must have a default locale"))
        );
    }

    @Test
    public void givenValidatingBundle__whenHasAName_andDefaultLocaleIsNotParsable__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("not quite a locale")),
                is(BundleSpecValidator.invalid("default locale is not parseable (must follow the <language>-<country>, i.e., fr or fr-FR"))
        );
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("fff")),
                is(BundleSpecValidator.invalid("default locale is not parseable (must follow the <language>-<country>, i.e., fr or fr-FR"))
        );
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("fr-fr")),
                is(BundleSpecValidator.invalid("default locale is not parseable (must follow the <language>-<country>, i.e., fr or fr-FR"))
        );
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("fr-FRR")),
                is(BundleSpecValidator.invalid("default locale is not parseable (must follow the <language>-<country>, i.e., fr or fr-FR"))
        );

    }

    @Test
    public void givenValidatingBundle__whenShortLocale__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("fr")),
                is(BundleSpecValidator.valid())
        );
    }

    @Test
    public void givenValidatingBundle__whenLongLocale__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withDefaultLocale("fr-FR")),
                is(BundleSpecValidator.valid())
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageOk__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").args(ArgSpec.builder().name("a").type(ArgSpec.Type.STRING).build()).message("hello {a:s}")
                                .build()
                ))),
                is(BundleSpecValidator.valid())
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageHasNoKey__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .args(ArgSpec.builder().name("a").type(ArgSpec.Type.STRING).build())
                                .message("{a:s}")
                                .build()
                ))),
                is(BundleSpecValidator.invalid("all messages must have a key"))
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageHasNoLocalization__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").args(ArgSpec.builder().name("a").type(ArgSpec.Type.STRING).build())
                                .build()
                ))),
                is(BundleSpecValidator.invalid("must provide a default message for key a.b.c"))
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageHasUnamedArgument__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").args(ArgSpec.builder().type(ArgSpec.Type.STRING).build()).message("hello")
                                .build()
                ))),
                is(BundleSpecValidator.invalid("arguments for key a.b.c must be named"))
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageHasUntypedArgument__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").args(ArgSpec.builder().name("a").build()).message("hello")
                                .build()
                ))),
                is(BundleSpecValidator.invalid("argument a for key a.b.c must be typed"))
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageHasNoArgs_andUsesUndeclaredArgument__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").message("hello {b:s}")
                                .build()
                ))),
                is(BundleSpecValidator.invalid("message for key a.b.c uses undeclared argument : b"))
        );
    }

    @Test
    public void givenValidatingBundleWithMessage__whenMessageUsesUndeclaredArgument__thenInvalid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC.withMessages(messages(
                        MessageSpec.builder()
                                .key("a.b.c").args(ArgSpec.builder().name("a").type(ArgSpec.Type.STRING).build()).message("hello {b:s}")
                                .build()
                ))),
                is(BundleSpecValidator.invalid("message for key a.b.c uses undeclared argument : b"))
        );
    }

    private ValueList<MessageSpec> messages(MessageSpec ... messages) {
        return new ValueList.Builder<MessageSpec>().with(messages).build();
    }

    @Test
    public void givenValidatingBundle__whenHasMinimalData__thenValid() throws Exception {
        assertThat(
                new BundleSpecValidator().validate(MINIMAL_SPEC),
                is(BundleSpecValidator.valid())
        );
    }
}