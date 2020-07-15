package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.*;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

public class ReplaceHandlerGenerator extends PagedCollectionHandlerGenerator {
    public ReplaceHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor, collectionDescriptor.replace());
    }

    @Override
    public TypeSpec handler() {
        String handlerClassSimpleName = this.collectionDescriptor.name() + "Replace";
        return TypeSpec.classBuilder(handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.collectionDescriptor.replace().requestValueObject()),
                        this.className(this.collectionDescriptor.replace().responseValueObject())
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
                        .addParameter(this.className(this.collectionDescriptor.replace().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.replace().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethod(this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"))
                .addMethod(this.errorResponseMethod("notAllowedError", "Status405", "ENTITY_REPLACEMENT_NOT_ALLOWED"))
                .addMethod(this.errorResponseMethod("badRequestError", "Status400", "BAD_REQUEST"))
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
                .addStatement("$T token = log.tokenized().error($S, adapter.getClass(), request)",
                        String.class, "adapter {} implementation breaks contract, crud should not ne null for {}"
                )
                .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //action validation
                .beginControlFlow("if(! adapter.crud().supportedActions().contains($T.DELETE))",
                        Action.class
                )
                .addStatement("$T token = log.tokenized().info($S, $T.DELETE, adapter.crud().supportedActions(), request)",
                        String.class, "{} action not supported, adapter only supports {}, request was {}", Action.class
                )
                .addStatement("return this.notAllowedError(token)")
                .endControlFlow()

                //request validation
                .beginControlFlow("if(! request.opt().entityId().isPresent())")
                .addStatement("$T token = log.tokenized().info($S, request)", String.class, "no entity id provided to update entity : {}")
                .addStatement("return this.badRequestError(token)")
                .endControlFlow()

                //default value
                .addStatement("$T value = request.opt().payload().orElseGet(() -> $T.builder().build())",
                        this.className(this.collectionDescriptor.types().replace()),
                        this.className(this.collectionDescriptor.types().replace())
                )

                //replacement
                .addStatement("$T<$T> entity", Entity.class, this.className(this.collectionDescriptor.types().entity()))
                .beginControlFlow("try")
                    .addStatement("entity = adapter.crud().replaceEntityWith(request.entityId(), value)")
                .nextControlFlow("catch($T e)", BadRequestException.class)
                    .addStatement("return $T.builder().status400($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.collectionDescriptor.replace().responseValueObject()),
                            this.relatedClassName("Status400", this.collectionDescriptor.replace().responseValueObject())
                    )
                .nextControlFlow("catch($T e)", ForbiddenException.class)
                    .addStatement("return $T.builder().status403($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.collectionDescriptor.replace().responseValueObject()),
                            this.relatedClassName("Status403", this.collectionDescriptor.replace().responseValueObject())
                    )
                .nextControlFlow("catch($T e)", NotFoundException.class)
                    .addStatement("return $T.builder().status404($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.collectionDescriptor.replace().responseValueObject()),
                            this.relatedClassName("Status404", this.collectionDescriptor.replace().responseValueObject())
                    )
                .nextControlFlow("catch($T e)", UnauthorizedException.class)
                    .addStatement("return $T.builder().status401($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.collectionDescriptor.replace().responseValueObject()),
                            this.relatedClassName("Status401", this.collectionDescriptor.replace().responseValueObject())
                    )
                .nextControlFlow("catch($T e)", UnexpectedException.class)
                    .addStatement("return $T.builder().status500($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.collectionDescriptor.replace().responseValueObject()),
                            this.relatedClassName("Status500", this.collectionDescriptor.replace().responseValueObject())
                    )
                .endControlFlow()

                //replacement error
                .beginControlFlow("if(entity == null)")
                    .addStatement("$T token = log.tokenized().error($S, adapter.getClass(), request)",
                            String.class, "adapter {} implementation breaks contract, created entity is null for {}"
                    )
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //nominal result
                .addStatement("return $T.builder().status200($T.builder()" +
                                ".xEntityId(entity.id())" +
                                ".location(String.format($S, adapter.crud().entityRepositoryUrl(), entity.id()))" +
                                ".payload(entity.value())" +
                                ".build()).build()",
                        this.className(this.collectionDescriptor.replace().responseValueObject()),
                        this.relatedClassName("Status200", this.collectionDescriptor.replace().responseValueObject()),
                        "%s/%s"
                )
                .build();
    }
}
