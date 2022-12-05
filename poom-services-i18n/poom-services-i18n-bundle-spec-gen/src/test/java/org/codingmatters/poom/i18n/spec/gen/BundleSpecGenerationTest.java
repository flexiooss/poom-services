package org.codingmatters.poom.i18n.spec.gen;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.ArgSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.json.BundleSpecWriter;
import org.codingmatters.poom.l10n.client.L10N;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.time.LocalDateTime;

import static org.codingmatters.tests.reflect.ReflectMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class BundleSpecGenerationTest {

    @Rule
    public TemporaryFolder sourcesDir = new TemporaryFolder();
    @Rule
    public TemporaryFolder resourcesDir = new TemporaryFolder();

    @Rule
    public FileHelper fileHelper = new FileHelper();
    private ClassLoaderHelper classes;
    private JsonFactory jsonFactory = new JsonFactory();

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleAsName__thenInterfaceIsCalledAfterThisName() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                this.jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_())
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleAsName__thenStaticClassBundle() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                this.jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Bundle").get(),
                is(aStatic().public_().class_())
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleAsName__thenStaticClassKeys() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                this.jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Keys").get(),
                is(aStatic().public_().class_())
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleAsName__thenABPublicStaticAccessorForBundleName() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                this.jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Bundle").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("name").returning(String.class)))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenCallingAccessorForBundleName__thenReturnsBundleName() throws Exception {

        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(this.classes.get("org.generated.ATestBundle$Bundle").call("name").get(), is("a test"));
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessage__thenAPublicStaticAccessorIsNamedFromKey() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();
        assertThat(
                this.classes.get("org.generated.ATestBundle$Keys").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("aKey").withoutParameters().returning(String.class)))
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessage__thenAPublicStaticMessageBuilderIsNamedFromKey() throws Exception {
        /*
          static L10N.Message greetings(L10N l10N) {
            return l10N.message(bundleName(), greetings());
          }
         */
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Messages").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("aKey").withParameters(
                        L10N.class
                ).returning(L10N.Message.class)))
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessage__thenAPublicStaticMessageBuilderWithDefaultL10NIsNamedFromKey() throws Exception {
        /*
          static L10N.Message greetings(L10N l10N) {
            return l10N.message(bundleName(), greetings());
          }
         */
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Messages").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("aKey").withoutParameters().returning(L10N.Message.class)))
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessageWithArgs__thenAPublicStaticMessageFormatterWithL10NNamedFromKey() throws Exception {
        /*

          static String greetings(L10N l10n, String user) {
            return greetings(l10n).m(user);
          }
         */
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").args(
                                a -> a.name("a1").type(ArgSpec.Type.STRING),
                                a -> a.name("a2").type(ArgSpec.Type.NUMBER),
                                a -> a.name("a3").type(ArgSpec.Type.DATE)
                        ).build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("aKey").withParameters(
                        L10N.class,
                        String.class,
                        Number.class,
                        LocalDateTime.class
                ).returning(String.class)))
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessageWithArgs__thenAPublicStaticMessageFormatterNamedFromKey() throws Exception {
        /*
          static String greetings(String user) {
            return greetings(L10N.l10n()).m(user);
          }
         */
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").args(
                                a -> a.name("a1").type(ArgSpec.Type.STRING),
                                a -> a.name("a2").type(ArgSpec.Type.NUMBER),
                                a -> a.name("a3").type(ArgSpec.Type.DATE)
                        ).build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.print(this.sourcesDir.getRoot(), "");
        this.fileHelper.printFile(this.sourcesDir.getRoot(), "ATestBundle.java");

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("aKey").withParameters(
                        String.class,
                        Number.class,
                        LocalDateTime.class
                ).returning(String.class)))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenCallingKeyAccessor__thenReturnsKey() throws Exception {

        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(this.classes.get("org.generated.ATestBundle$Keys").call("aKey").get(), is("a.key"));
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasDefaultLocale__thenAPublicStaticDefaultLocaleIsGenerated() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Bundle").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("defaultLocale").withoutParameters().returning(String.class)))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenCallingDefaultLocaleAccessor__thenReturnsDefaultLocale() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .defaultLocale("fr-FR")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(this.classes.get("org.generated.ATestBundle$Bundle").call("defaultLocale").get(), is("fr-FR"));
    }



    @Test
    public void givenGeneratingBundleSpecInterface__whenBundle__thenAPublicStaticSpecStreamAccessor() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Bundle").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("spec").withoutParameters().returning(InputStream.class)))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenDistinctResourceDir__thenSpecFileInResourceDirPath() throws Exception {
        BundleSpec spec = BundleSpec.builder()
                .name("a test")
                .build();
        new BundleSpecGeneration(
                "org.generated",
                spec,
                jsonFactory).to(this.sourcesDir.getRoot(), this.resourcesDir.getRoot());

        File specDir = new File(this.resourcesDir.getRoot(), "org/generated/spec");
        assertThat(specDir.exists(), is(true));
        assertThat(specDir.isDirectory(), is(true));

        File specFile = specDir.listFiles(file -> file.getName().startsWith("bundle-") && file.getName().endsWith(".json"))[0];

        assertThat(
                this.readStream(new FileInputStream(specFile)),
                is(this.jsonString(spec))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenBundle__thenSpecStreamAccessorReturnsJsonSpec() throws Exception {
        BundleSpec spec = BundleSpec.builder()
                .name("a test")
                .build();
        new BundleSpecGeneration(
                "org.generated",
                spec,
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.readStream((InputStream) this.classes.get("org.generated.ATestBundle$Bundle").call("spec").get()),
                is(this.jsonString(spec))
        );
    }



    @Test
    public void givenGeneratingBundleSpecInterface__thenAPublicStaticVersionAccessorIsGenerated() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle$Bundle").get(),
                is(aStatic().public_().class_().with(aPublic().static_().method().named("version").withoutParameters().returning(String.class)))
        );
    }

    @Test
    public void givenBundleSpecInterfaceGenerated__whenCallingVersionAccessor__thenReturnsDefaultLocale() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .defaultLocale("fr-FR")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.sourcesDir.getRoot(), this.sourcesDir.getRoot());

        this.compile();

        assertThat((String) this.classes.get("org.generated.ATestBundle$Bundle").call("version").get(), startsWith("dev-"));
    }




    private void print(File file, String prefix) {
        if(file.isDirectory()) {
            System.out.println(prefix + "+ " + file.getName());
            for (File child : file.listFiles()) {
                this.print(child, prefix + "  ");
            }
        } else {
            System.out.println(prefix + "- " + file.getName());
        }
    }

    private String jsonString(BundleSpec spec) throws IOException {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = new JsonFactory().createGenerator(out)) {
            new BundleSpecWriter().write(generator, spec);
            generator.close();
            return out.toString();
        }
    }

    private String readStream(InputStream in) throws IOException {
        try(in; Reader reader = new InputStreamReader(in)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }


    private void compile() throws Exception {
        this.classes = CompiledCode.builder()
                .source(this.sourcesDir.getRoot())
                .compile().classLoader();
    }
}