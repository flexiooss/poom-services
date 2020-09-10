package org.codingmatters.poom.i18n.bundle.spec;

import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class BundleSpecsTest {

    public static final BundleSpec BUNDLE_SPEC_0 = BundleSpec.builder()
            .name("first bundle")
            .defaultLocale("en-GB")
            .messages(
                    MessageSpec.builder().key("greetings").message("Hello {user:s} !").build()
            )
            .build();
    public static final BundleSpec SECOND_BUNDLE_1 = BundleSpec.builder()
            .name("second bundle")
            .defaultLocale("fr-FR")
            .messages(
                    MessageSpec.builder().key("say.i.to.user").message("Bonjour {user:s} !").build()
            )
            .build();

    @Test
    public void whenReadingFromYaml__thenBundlesAreRead() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("i18n-bundles.yml")) {
            BundleSpec[] bundleSpec = BundleSpecs.fromYaml().read(in);

            assertThat(bundleSpec[0], is(BUNDLE_SPEC_0));
            assertThat(bundleSpec[1], is(SECOND_BUNDLE_1));
        }
    }

    @Test
    public void whenReadingFromJson__thenBundlesAreRead() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("i18n-bundles.json")) {
            BundleSpec[] bundleSpec = BundleSpecs.fromJson().read(in);

            assertThat(bundleSpec[0], is(BUNDLE_SPEC_0));
            assertThat(bundleSpec[1], is(SECOND_BUNDLE_1));
        }
    }
}