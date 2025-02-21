package org.codingmatters.poom.api.test.generation.mavel;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codingmatters.poom.api.test.generation.TestApiGenerator;
import org.codingmatters.poom.api.test.generation.mavel.descriptors.TestApisDescriptor;
import org.codingmatters.poom.api.test.generation.mavel.descriptors.json.TestApisDescriptorReader;
import org.codingmatters.rest.maven.plugin.AbstractGenerateAPIMojo;
import org.codingmatters.value.objects.generation.Naming;
import org.raml.v2.api.RamlModelResult;

import java.io.File;
import java.io.IOException;

@Mojo(name = "test-api-from-descriptor")
public class TestApisFromDescriptorMojo extends AbstractGenerateAPIMojo {
    @Parameter(required = true, alias = "descriptors")
    private File descriptorDir;

    @Parameter(defaultValue = "${basedir}/target/generated-sources/", alias = "output-directory")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        JsonFactory jsonFactory = new JsonFactory();
        if(this.descriptorDir.exists()) {
            if (this.descriptorDir.isDirectory()) {
                for (File descriptorFile : this.descriptorDir.listFiles()) {
                    try (JsonParser parser = jsonFactory.createParser(descriptorFile)) {
                        TestApisDescriptor descriptor = new TestApisDescriptorReader().read(parser);
                        RamlModelResult raml = this.parseFile(descriptor.apiSpecResource());
                        TestApiGenerator generator = new TestApiGenerator(
                                descriptor.apiPackage() != null ? descriptor.apiPackage() : descriptor.rootPackage() + ".api",
                                descriptor.clientPackage() != null ? descriptor.clientPackage() : descriptor.rootPackage() + ".client",
                                new Naming(),
                                this.outputDirectory
                        );
                        generator.generate(raml);
                    } catch (IOException e) {
                        throw new MojoExecutionException("failed reading descriptor file : " + descriptorFile, e);
                    }
                }
            } else {
                try (JsonParser parser = jsonFactory.createParser(this.descriptorDir)) {
                    TestApisDescriptor descriptor = new TestApisDescriptorReader().read(parser);
                    RamlModelResult raml = this.parseFile(descriptor.apiSpecResource());
                    TestApiGenerator generator = new TestApiGenerator(
                            descriptor.apiPackage() != null ? descriptor.apiPackage() : descriptor.rootPackage() + ".api",
                            descriptor.clientPackage() != null ? descriptor.clientPackage() : descriptor.rootPackage() + ".client",
                            new Naming(),
                            this.outputDirectory
                    );
                    generator.generate(raml);
                } catch (IOException e) {
                    throw new MojoExecutionException("failed reading descriptor file : " + this.descriptorDir, e);
                }
            }
        } else {
            throw new MojoExecutionException("no descriptor file : " + this.descriptorDir);
        }
    }
}
