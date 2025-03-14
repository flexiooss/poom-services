package org.codingmatters.poom.api.test.generation.mavel;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codingmatters.poom.api.test.generation.TestApiGenerator;
import org.codingmatters.rest.api.generator.exception.RamlSpecException;
import org.codingmatters.rest.api.generator.handlers.HandlersHelper;
import org.codingmatters.rest.maven.plugin.AbstractGenerateAPIMojo;
import org.codingmatters.value.objects.generation.Naming;
import org.raml.v2.api.RamlModelResult;

import java.io.File;
import java.io.IOException;

@Mojo(name = "test-api")
public class TestApiMojo extends AbstractGenerateAPIMojo {

    @Parameter(required = true, alias = "root-package")
    private String rootPackage;

    @Parameter(required = false, alias = "api-package")
    private String apiPackage;

    @Parameter(required = false, alias = "client-package")
    private String clientPackage;

    @Parameter(defaultValue = "${basedir}/target/generated-sources/", alias = "output-directory")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            RamlModelResult raml = this.resolveRamlModel();
            TestApiGenerator generator = new TestApiGenerator(
                    this.apiPackage != null ? this.apiPackage : this.rootPackage + ".api",
                    this.clientPackage != null ? this.clientPackage : this.rootPackage + ".client",
                    new Naming(),
                    this.outputDirectory
            );
            generator.generate(raml);
        } catch (IOException e) {
            throw new MojoExecutionException("error while generating paged collection handlers", e);
        }
    }
}
