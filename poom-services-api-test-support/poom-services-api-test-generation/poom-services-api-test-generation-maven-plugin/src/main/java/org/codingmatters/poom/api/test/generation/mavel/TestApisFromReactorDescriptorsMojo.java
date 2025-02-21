package org.codingmatters.poom.api.test.generation.mavel;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codingmatters.poom.api.test.generation.TestApiGenerator;
import org.codingmatters.poom.api.test.generation.mavel.descriptors.TestApisDescriptor;
import org.codingmatters.poom.api.test.generation.mavel.descriptors.json.TestApisDescriptorReader;
import org.codingmatters.rest.maven.plugin.AbstractGenerateAPIMojo;
import org.codingmatters.value.objects.generation.Naming;
import org.raml.v2.api.RamlModelResult;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

@Mojo(name = "test-api-from-reactor-descriptors")
public class TestApisFromReactorDescriptorsMojo extends AbstractGenerateAPIMojo {
    @Parameter(required = true, alias = "descriptor-pattern", defaultValue = "target/client-descriptors/[^/]*\\.json")
    private String descriptorPattern;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @org.apache.maven.plugins.annotations.Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Parameter(defaultValue = "${basedir}/target/generated-sources/", alias = "output-directory")
    private File outputDirectory;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        JsonFactory jsonFactory = new JsonFactory();

        MavenProject reactorProject = this.project;
        while(reactorProject.hasParent()) {
            reactorProject = reactorProject.getParent();
        }

        System.out.println("session projects :");
        for (MavenProject sessionProject : this.session.getProjects()) {
            System.out.println("\t" + sessionProject.getArtifact().getArtifactId() + " " + sessionProject.getBasedir().getAbsolutePath());
            List<File> descriptors = this.findDescriptors(
                    sessionProject.getBasedir().listFiles(),
                    sessionProject.getBasedir().getAbsolutePath()
            );
            for (File descriptorFile : descriptors) {
                System.out.println("\t\t" + descriptorFile.getAbsolutePath());
                try (JsonParser parser = jsonFactory.createParser(descriptorFile)) {
                    TestApisDescriptor descriptor = new TestApisDescriptorReader().read(parser);
                    RamlModelResult raml;
                    try {
                        raml = this.parseFile(descriptor.apiSpecResource());
                    } catch (MojoFailureException e) {
                        throw new MojoExecutionException("missing raml descriptor " + descriptor.apiSpecResource() + ", is it in your classpath ? " + descriptor, e);
                    }

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
        }

    }

    private List<File> findDescriptors(File[] files, String root) {
        List<File> results = new LinkedList<>();

        for (File file : files) {
            if(file.isDirectory()) {
                results.addAll(this.findDescriptors(file.listFiles(), root));
            } else {
                String relativePath = file.getAbsolutePath().substring(root.length() + 1);
                if(relativePath.matches(this.descriptorPattern)) {
                    results.add(file);
                }
            }
        }

        return results;
    }
}
