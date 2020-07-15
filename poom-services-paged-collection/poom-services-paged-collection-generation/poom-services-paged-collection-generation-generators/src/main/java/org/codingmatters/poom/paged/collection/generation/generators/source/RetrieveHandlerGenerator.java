package org.codingmatters.poom.paged.collection.generation.generators.source;

import com.squareup.javapoet.*;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import javax.lang.model.element.Modifier;
import java.util.Optional;
import java.util.function.Function;

public class RetrieveHandlerGenerator extends PagedCollectionHandlerGenerator {
    public RetrieveHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor, collectionDescriptor.retrieve());
    }

    @Override
    public TypeSpec handler() {
        String handlerClassSimpleName = this.collectionDescriptor.name() + "Retrieve";
        return TypeSpec.classBuilder(handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.collectionDescriptor.retrieve().requestValueObject()),
                        this.className(this.collectionDescriptor.retrieve().responseValueObject())
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
                        .addParameter(this.className(this.collectionDescriptor.retrieve().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.retrieve().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethod(this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"))
                .addMethod(this.errorResponseMethod("badRequestError", "Status400", "BAD_REQUEST"))
//                .addMethod(this.errorResponseMethod("notFoundError", "Status404", "RESOURCE_NOT_FOUND"))
                .addMethod(this.entityNotFoundResponseMethod())
                .addMethod(this.castedErrorMethod())

                .build();
    }

    private CodeBlock constructorBody() {
        return CodeBlock.builder()
                .addStatement("this.adapterProvider = adapterProvider")
                .build();
    }

    private CodeBlock applyBody() {
        return CodeBlock.builder()
                //adapter
                .addStatement("$T adapter", this.adapterClass())
                .beginControlFlow("try")
                    .addStatement("adapter = this.adapterProvider.adapter()")
                .nextControlFlow("catch($T e)", Exception.class)
                    .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "failed getting adapter for ")
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //crud
                .beginControlFlow("if(adapter.crud() == null)")
                    .addStatement("$T token = log.tokenized().info($S, adapter.getClass(), request)",
                            String.class, "adapter {} implementation breaks contract, crud should not ne null for {}"
                    )
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //request validation
                .beginControlFlow("if(! request.opt().entityId().isPresent())")
                    .addStatement("$T token = log.tokenized().info($S, request)",
                            String.class, "no entity id provided to update entity : {}"
                    )
                    .addStatement("return this.badRequestError(token)")
                .endControlFlow()

                //retrieve
                .addStatement("$T<$T<$T>> entity", Optional.class, Entity.class, this.className(this.collectionDescriptor.types().entity()))
                .beginControlFlow("try")
                .addStatement("entity = adapter.crud().retrieveEntity(request.entityId())")
                .nextControlFlow("catch($T e)", BadRequestException.class)
                .addStatement("return $T.builder().status400($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status400", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .nextControlFlow("catch($T e)", ForbiddenException.class)
                .addStatement("return $T.builder().status403($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status403", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .nextControlFlow("catch($T e)", NotFoundException.class)
                .addStatement("return $T.builder().status404($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status404", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .nextControlFlow("catch($T e)", UnauthorizedException.class)
                .addStatement("return $T.builder().status401($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status401", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .nextControlFlow("catch($T e)", UnexpectedException.class)
                .addStatement("return $T.builder().status500($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status500", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .endControlFlow()

                //nominal
                .beginControlFlow("if(entity.isPresent())")
                    .addStatement("return $T.builder().status200($T.builder()" +
                                    ".xEntityId(entity.get().id())" +
                                    ".location(String.format($S, adapter.crud().entityRepositoryUrl(), entity.get().id()))" +
                                    ".payload(entity.get().value())" +
                                    ".build()).build()",
                            this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                            this.relatedClassName("Status200", this.collectionDescriptor.retrieve().responseValueObject()),
                            "%s/%s"
                    )
                .nextControlFlow("else")
                    .addStatement("$T token = log.tokenized().info($S, adapter.crud().entityRepositoryUrl(), request)",
                            String.class, "no entity found in repository {} for request {}"
                    )
                .addStatement("return this.entityNotFound(request.entityId(), token)")
                .endControlFlow()
                .build();
    }

}
