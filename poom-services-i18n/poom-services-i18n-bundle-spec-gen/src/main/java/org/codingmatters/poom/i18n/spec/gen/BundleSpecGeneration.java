package org.codingmatters.poom.i18n.spec.gen;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang.text.StrBuilder;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.json.BundleSpecWriter;
import org.codingmatters.value.objects.generation.GenerationUtils;

import javax.lang.model.element.Modifier;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class BundleSpecGeneration {

    private final String packageName;
    private final BundleSpec spec;
    private final JsonFactory jsonFactory;

    public BundleSpecGeneration(String packageName, BundleSpec spec, JsonFactory jsonFactory) {
        this.packageName = packageName;
        this.spec = spec;
        this.jsonFactory = jsonFactory;
    }

    public void to(File sourceDirectory) throws IOException {
        File packageDestination = GenerationUtils.packageDir(sourceDirectory, this.packageName);
        String bundleSpecFileName = "bundle-" + UUID.randomUUID().toString() + ".json";
        String specResource = this.packageName.replaceAll("\\.", "/") + "/spec/" + bundleSpecFileName;

        File specFile = new File(sourceDirectory, specResource);

        specFile.getParentFile().mkdirs();
        specFile.createNewFile();

        this.serializeSpec(specFile);
        GenerationUtils.writeJavaFile(packageDestination, this.packageName, this.bundleType(specResource));
    }

    private TypeSpec bundleType(String specResource) {
        return TypeSpec.interfaceBuilder(this.bundleInterface())
                .addModifiers(Modifier.PUBLIC)
                .addMethods(this.keyMethods())
                .addMethod(this.defaultLocaleAccessor())
                .addMethod(this.bundleNameAccessor())
                .addMethod(this.specAccessor(specResource))
                .addMethod(this.versionAccessor())
                .build();
    }

    private void serializeSpec(File specFile) throws IOException {
        try(
                OutputStream out = new FileOutputStream(specFile);
                JsonGenerator generator = this.jsonFactory.createGenerator(out)
        ) {
            new BundleSpecWriter().write(generator, this.spec);
        }
    }

    private MethodSpec bundleNameAccessor() {
        return MethodSpec.methodBuilder("bundleName")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addStatement("return $S", this.spec.name())
                .build();
    }

    private MethodSpec specAccessor(String specResource) {
        return MethodSpec.methodBuilder("spec")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(InputStream.class)
                .addStatement("return $L.class.getClassLoader().getResourceAsStream($S)", this.bundleInterface().simpleName(), specResource)
                .build();
    }

    private MethodSpec defaultLocaleAccessor() {
        return MethodSpec.methodBuilder("defaultLocale")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(ClassName.get(String.class))
                .addStatement("return $S", this.spec.defaultLocale())
                .build();
    }

    private List<MethodSpec> keyMethods() {
        List<MethodSpec> result = new LinkedList<>();
        if(this.spec.opt().messages().isPresent()) {
            for (MessageSpec message : this.spec.messages()) {
                result.add(this.keyMethod(message));
            }
        }
        return result;
    }

    private MethodSpec keyMethod(MessageSpec from) {
        return MethodSpec.methodBuilder(this.uncapitalizedFirst(this.camelCased(from.key())))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(String.class))
                .addStatement("return $S", from.key())
                .build();
    }

    private MethodSpec versionAccessor() {
        return MethodSpec.methodBuilder("version")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addStatement("$T rawVersion = $L.class.getPackage().getImplementationVersion()", String.class, this.bundleInterface().simpleName())
                .addStatement("return rawVersion != null ? rawVersion : $S", "dev-" + System.currentTimeMillis())
                .build();
    }

    private ClassName bundleInterface() {
        return ClassName.get(this.packageName, this.bundleInterfaceName());
    }

    private String bundleInterfaceName() {
        return this.camelCased(this.spec.name() + "Bundle");
    }

    private String camelCased(String str) {
        StrBuilder result = new StrBuilder();
        String[] parts = str.split("[\\s.]+");
        for (String part : parts) {
            result.append(this.capitalizedFirst(part));
        }

        return result.toString();
    }

    private String capitalizedFirst(String str) {
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    private String uncapitalizedFirst(String str) {
        return str.substring(0,1).toLowerCase() + str.substring(1);
    }
}
