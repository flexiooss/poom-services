package org.codingmatters.poom.paged.collection.generation.generators;

import org.codingmatters.rest.maven.plugin.raml.RamlFileCollector;
import org.codingmatters.tests.compile.CompiledCode;
import org.codingmatters.tests.compile.helpers.ClassLoaderHelper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


public class PagedCollectionGeneratorTest {
    static RamlModelResult TEST_API_RAML = raml("test-api-spec.raml");


    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void whenGenerating__thenGenerated() throws Exception {
        new PagedCollectionGenerator(TEST_API_RAML, "org.generated.api", "org.generated.api.types").generate(this.dir.getRoot());
        ClassLoaderHelper classes = CompiledCode.builder().source(this.dir.getRoot()).compile().classLoader();

        assertThat(classes.get("org.generated.api.collection.handlers.NoParamsUpdate"), is(notNullValue()));
    }

    static private RamlModelResult raml(String resource) {
        RamlFileCollector.Builder builder = RamlFileCollector.spec(resource);
        RamlFileCollector collector;
        try {
            collector = builder.build();
        } catch (IOException e) {
            throw new RuntimeException("failed parsing " + resource, e);
        }
        return new RamlModelBuilder().buildApi(collector.specFile());
    }

}