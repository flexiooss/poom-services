package org.codingmatters.poom.paged.collection.generation.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codingmatters.poom.paged.collection.generation.generators.PagedCollectionGenerator;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.rest.api.generator.exception.RamlSpecException;
import org.codingmatters.rest.maven.plugin.AbstractGenerateAPIMojo;
import org.raml.v2.api.RamlModelResult;

import java.io.File;
import java.io.IOException;

@Mojo(name = "paged-collection-handlers")
public class GeneratePagedCollectionHandlersMojo extends AbstractGenerateAPIMojo {

    @Parameter(required = true, alias = "api-package")
    private String apiPackage;

    @Parameter(required = false, alias = "types-package")
    private String typesPackage;

    @Parameter(defaultValue = "${basedir}/target/generated-sources/", alias = "output-directory")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        RamlModelResult raml = this.resolveRamlModel();
        PagedCollectionGenerator generator = new PagedCollectionGenerator(
                raml,
                this.apiPackage,
                this.typesPackage != null ? this.typesPackage : this.apiPackage + ".types");
        try {
            generator.generate(this.outputDirectory);
        } catch (RamlSpecException | IncoherentDescriptorException | IOException e) {
            throw new MojoExecutionException("error while generating paged collection handlers", e);
        }
    }
}
