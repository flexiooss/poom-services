package org.codingmatters.poom.i18n.spec.gen;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.json.BundleSpecWriter;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.FileHelper;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.codingmatters.tests.reflect.ReflectMatchers.aPublic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class BundleSpecGenerationTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

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
                this.jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_())
        );
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasAMessage__thenAPublicStaticAccessorIsNamedFromKey() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("aKey").withoutParameters().returning(String.class)))
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
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(this.classes.get("org.generated.ATestBundle").call("aKey").get(), is("a.key"));
    }

    @Test
    public void givenGeneratingBundleSpecInterface__whenBundleHasDefaultLocale__thenAPublicStaticDefaultLocaleIsGenerated() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .messages(MessageSpec.builder().key("a.key").build())
                        .build(),
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("defaultLocale").withoutParameters().returning(String.class)))
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
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(this.classes.get("org.generated.ATestBundle").call("defaultLocale").get(), is("fr-FR"));
    }



    @Test
    public void givenGeneratingBundleSpecInterface__whenBundle__thenAPublicStaticSpecStreamAccessor() throws Exception {
        new BundleSpecGeneration(
                "org.generated",
                BundleSpec.builder()
                        .name("a test")
                        .build(),
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("spec").withoutParameters().returning(InputStream.class)))
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
                jsonFactory).to(this.dir.getRoot());

        this.print(this.dir.getRoot(), "");
        this.fileHelper.printFile(this.dir.getRoot(), "ATestBundle.java");

        this.compile();

        assertThat(
                this.readStream((InputStream) this.classes.get("org.generated.ATestBundle").call("spec").get()),
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
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat(
                this.classes.get("org.generated.ATestBundle").get(),
                is(aPublic().interface_().with(aPublic().static_().method().named("version").withoutParameters().returning(String.class)))
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
                jsonFactory).to(this.dir.getRoot());

        this.compile();

        assertThat((String) this.classes.get("org.generated.ATestBundle").call("version").get(), startsWith("dev-"));
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
        this.classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();
    }
}