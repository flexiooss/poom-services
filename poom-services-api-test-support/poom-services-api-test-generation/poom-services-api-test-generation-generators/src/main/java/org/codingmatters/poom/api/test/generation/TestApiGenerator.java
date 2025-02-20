package org.codingmatters.poom.api.test.generation;

import com.squareup.javapoet.*;
import org.codingmatters.poom.handler.CumulatingTestHandler;
import org.codingmatters.value.objects.generation.Naming;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.codingmatters.value.objects.generation.GenerationUtils.packageDir;
import static org.codingmatters.value.objects.generation.GenerationUtils.writeJavaFile;

public class TestApiGenerator {
    private final String apiPackage;
    private final String clientPackage;
    private final String testPackage;
    private final Naming naming;
    private final File rootDirectory;

    public TestApiGenerator(String apiPackage, String clientPackage, Naming naming, File rootDirectory) {
        this.apiPackage = apiPackage;
        this.clientPackage = clientPackage;
        this.naming = naming;
        this.rootDirectory = rootDirectory;

        this.testPackage = this.apiPackage + ".test";
    }


    public void generate(RamlModelResult raml) throws IOException {
        File packageDestination = packageDir(this.rootDirectory, this.testPackage);
        writeJavaFile(packageDestination, this.testPackage, this.type(raml));
        System.out.println("generated test api for " + raml.getApiV10().title().value());
    }

    private TypeSpec type(RamlModelResult ramlModel) {
        String apiTitle = ramlModel.getApiV10().title().value();
        TypeSpec.Builder result = TypeSpec.classBuilder(this.naming.type(apiTitle, "TestApi"))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(BeforeEachCallback.class))
                .addSuperinterface(ClassName.get(AfterEachCallback.class))
                .addField(this.clientType(apiTitle), "client", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(this.constructor(apiTitle, ramlModel.getApiV10().resources()))
                .addMethod(this.clientMethod(apiTitle))
                .addMethods(this.jupiterExtensionMethods(ramlModel.getApiV10().resources()))
                ;

        this.addMethodsAndFields(ramlModel.getApiV10().resources(), result);

        return result.build();
    }

    private void addMethodsAndFields(List<Resource> resources, TypeSpec.Builder result) {
        for (Resource resource : resources) {
            result.addMethods(this.handlerResourceMethods(resource));
            result.addFields(this.handlerResourceFields(resource));
            this.addMethodsAndFields(resource.resources(), result);
        }
    }

    private List<MethodSpec> jupiterExtensionMethods(List<Resource> resources) {
        List<MethodSpec> results = new LinkedList<>();

        CodeBlock.Builder cleanup = CodeBlock.builder();
        this.appendCalls("cleanup", cleanup, resources);
        results.add(MethodSpec.methodBuilder("afterEach")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get(ExtensionContext.class), "context")
                        .addCode(cleanup.build())
                .build());


        CodeBlock.Builder initialize = CodeBlock.builder();
        this.appendCalls("initialize", initialize, resources);
        results.add(MethodSpec.methodBuilder("beforeEach")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get(ExtensionContext.class), "context")
                        .addCode(initialize.build())
                .build());
        return results;
    }

    private void appendCalls(String call, CodeBlock.Builder builder, List<Resource> resources) {
        for (Resource resource : resources) {
            for (Method method : resource.methods()) {
                String property = this.naming.property(resource.displayName().value(), method.displayName().value());
                builder.addStatement("this.$L.$L()", property, call);
            }
            this.appendCalls(call, builder, resource.resources());
        }
    }

    private List<FieldSpec> handlerResourceFields(Resource resource) {
        List<FieldSpec> result = new LinkedList<>();
        for (Method method : resource.methods()) {
            result.add(FieldSpec.builder(
                    this.handlerResourceType(resource, method),
                    this.naming.property(resource.displayName().value(), method.displayName().value())
            ).addModifiers(Modifier.PRIVATE, Modifier.FINAL).initializer(CodeBlock.builder()
                            .addStatement("new $T() {protected $T defaultResponse($T request) {return null;}}",
                                    this.handlerResourceType(resource, method),
                                    this.responseType(resource, method),
                                    this.requestType(resource, method)
                            )
                    .build()).build());
        }
        return result;
    }

    private List<MethodSpec> handlerResourceMethods(Resource resource) {
        List<MethodSpec> results = new LinkedList<>();
        for (Method method : resource.methods()) {
            String property = this.naming.property(resource.displayName().value(), method.displayName().value());
            results.add(MethodSpec.methodBuilder(property)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(this.handlerResourceType(resource, method))
                            .addStatement("return this.$L", property)
                    .build()
            );
        }
        return results;
    }

    private ParameterizedTypeName handlerResourceType(Resource resource, Method method) {
        return ParameterizedTypeName.get(
                ClassName.get(CumulatingTestHandler.class),
                this.requestType(resource, method), this.responseType(resource, method)
        );
    }

    private ClassName responseType(Resource resource, Method method) {
        return ClassName.get(this.apiPackage, this.naming.type(resource.displayName().value(), method.displayName().value(), "Response"));
    }

    private ClassName requestType(Resource resource, Method method) {
        return ClassName.get(this.apiPackage, this.naming.type(resource.displayName().value(), method.displayName().value(), "Request"));
    }

    private MethodSpec constructor(String apiTitle, List<Resource> resources) {
        ClassName handlersType = ClassName.get(this.apiPackage, this.naming.type(apiTitle + "Handlers"));
        ClassName handlersClientType = ClassName.get(this.clientPackage, this.naming.type(apiTitle + "HandlersClient"));

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$T.Builder handlersBuilder = new $T.Builder()", handlersType, handlersType);

        this.appendResourceHandlerInitStatements(builder, resources);

        return builder
                .addStatement("$T handlers = handlersBuilder.build()", handlersType)
                .addStatement("this.client = new $T(handlers)", handlersClientType)
                .build();
    }

    private void appendResourceHandlerInitStatements(MethodSpec.Builder builder, List<Resource> resources) {
        for (Resource resource : resources) {
            for (Method method : resource.methods()) {
                String property = this.naming.property(resource.displayName().value(), method.displayName().value());
                builder.addStatement("handlersBuilder.$LHandler(this.$L)", property, property);
            }
            this.appendResourceHandlerInitStatements(builder, resource.resources());
        }

    }

    private MethodSpec clientMethod(String apiTitle) {
        return MethodSpec.methodBuilder("client")
                .addModifiers(Modifier.PUBLIC)
                .returns(this.clientType(apiTitle))
                .addStatement("return this.client")
                .build();
    }

    private ClassName clientType(String apiTitle) {
        return ClassName.get(this.clientPackage, this.naming.type(apiTitle + "Client"));
    }
}
