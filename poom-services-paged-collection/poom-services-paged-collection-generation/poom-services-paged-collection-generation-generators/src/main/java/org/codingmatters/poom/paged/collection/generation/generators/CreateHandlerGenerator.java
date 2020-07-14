package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.*;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

public class CreateHandlerGenerator extends PagedCollectionHandlerGenerator {
    public CreateHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor);
    }

    @Override
    public TypeSpec handler() {
        String handlerClassSimpleName = this.collectionDescriptor.name() + "Create";
        return TypeSpec.classBuilder(handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.collectionDescriptor.create().requestValueObject()),
                        this.className(this.collectionDescriptor.create().responseValueObject())
                ))

                .addField(FieldSpec.builder(ClassName.get(CategorizedLogger.class), "log", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("$T.getLogger($L.class)", CategorizedLogger.class, handlerClassSimpleName)
                        .build())
                .addField(this.adapterProviderClass(), "adapterProvider", Modifier.PRIVATE, Modifier.FINAL)

                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                this.adapterProviderClass(),
                                "adapterProvider"
                        )
                        .addCode(this.constructorBody())
                        .build())
                .addMethod(MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
                        .addParameter(this.className(this.collectionDescriptor.create().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.create().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .build();
    }

    private CodeBlock applyBody() {
        return null;
    }

    private CodeBlock constructorBody() {
        return CodeBlock.builder()
                .addStatement("this.adapterProvider = adapterProvider")
                .build();
    }

}
