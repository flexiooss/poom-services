package org.codingmatters.poom.i18n.spec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codingmatters.poom.i18n.bundle.spec.BundleSpecs;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.spec.gen.BundleSpecGeneration;
import org.codingmatters.poom.i18n.spec.gen.BundleSpecValidator;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

@Mojo(name = "bundles")
public class BundleMojo extends AbstractMojo {

    @Parameter(alias = "bundle",defaultValue = "${basedir}/src/main/i18n/bundles.yml")
    private File bundleFile;

    @Parameter(alias = "bundle-package", defaultValue = "${project.groupId}.i18n.bundles")
    private String bundlePackage;


    @Parameter(defaultValue = "${basedir}/target/generated-sources/", alias = "sources-directory")
    private File sourcesDirectory;

//    @Parameter(defaultValue = "${basedir}/target/generated-resources/", alias = "resources-directory")
//    private File resourcesDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(! this.bundleFile.exists()) {
            this.getLog().info(String.format("no i18n bundle to process, %s does not exist.", this.bundleFile.getAbsolutePath()));
            return;
        }
        BundleSpec[] bundleSpecs;
        try(InputStream in = new FileInputStream(this.bundleFile)) {
            bundleSpecs = new BundleSpecs(new YAMLFactory()).read(in);
            if (bundleSpecs != null && bundleSpecs.length > 0) {
                this.getLog().info(String.format("will generate i18n bundles : %s", Arrays.stream(bundleSpecs).map(bundleSpec -> bundleSpec.name()).collect(Collectors.joining(", "))));
            } else {
                this.getLog().info(String.format("no i18n bundle found in %s", this.bundleFile.getAbsolutePath()));
            }
        } catch (IOException e) {
            throw new MojoFailureException("failed reading i18n bundle specs", e);
        }

        BundleSpecValidator validator = new BundleSpecValidator();
        for (BundleSpec bundleSpec : bundleSpecs) {
            BundleSpecValidator.BundleValidity validation = validator.validate(bundleSpec);
            if(! validation.isValid()) {
                throw new MojoExecutionException("error generating i18n bundles : " + validation.message());
            }
        }

        JsonFactory factory = new JsonFactory();
        int i = 1;
        for (BundleSpec bundleSpec : bundleSpecs) {
            try {
                new BundleSpecGeneration(this.bundlePackage, bundleSpec, factory).to(this.sourcesDirectory, this.sourcesDirectory);
                this.getLog().info(String.format("generated i18n bundle : %s", bundleSpec.name()));
            } catch (IOException e) {
                throw new MojoFailureException("error generating " + i + "th i18n bundle : " + bundleSpec.name() , e);
            }
            i ++;
        }
    }
}
