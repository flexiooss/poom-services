package org.codingmatters.poom.paged.collection.generation.generators.source;

import com.squareup.javapoet.*;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

public class ReplaceOrUpdateHandlerGenerator extends PagedCollectionHandlerGenerator {

    public enum HandlerConfig {
        Replace("replaceEntityWith", "ENTITY_REPLACEMENT_NOT_ALLOWED") {
            @Override
            public org.codingmatters.poom.paged.collection.generation.spec.Action action(PagedCollectionDescriptor pagedCollectionDescriptor) {
                return pagedCollectionDescriptor.replace();
            }

            @Override
            public String type(PagedCollectionDescriptor pagedCollectionDescriptor) {
                return pagedCollectionDescriptor.opt().types().replace().orElse(null);
            }
        },
        Update("updateEntityWith", "ENTITY_UPDATE_NOT_ALLOWED") {
            @Override
            public org.codingmatters.poom.paged.collection.generation.spec.Action action(PagedCollectionDescriptor pagedCollectionDescriptor) {
                return pagedCollectionDescriptor.update();
            }

            @Override
            public String type(PagedCollectionDescriptor pagedCollectionDescriptor) {
                return pagedCollectionDescriptor.opt().types().update().orElse(null);
            }
        }
        ;

        public final String crudMethod;
        public final String notAllowedCode;

        HandlerConfig(String crudMethod, String notAllowedCode) {
            this.crudMethod = crudMethod;
            this.notAllowedCode = notAllowedCode;
        }

        public abstract org.codingmatters.poom.paged.collection.generation.spec.Action action(PagedCollectionDescriptor pagedCollectionDescriptor);
        public abstract String type(PagedCollectionDescriptor pagedCollectionDescriptor);
    }

    private final org.codingmatters.poom.paged.collection.generation.spec.Action action;
    private final String type;
    private final String crudMethod;
    private final String handlerClassSimpleName;
    private final String notAllowedCode;

    public ReplaceOrUpdateHandlerGenerator(PagedCollectionDescriptor collectionDescriptor, HandlerConfig handlerConfig) {
        super(collectionDescriptor, handlerConfig.action(collectionDescriptor));
        this.action = handlerConfig.action(collectionDescriptor);
        this.type = handlerConfig.type(collectionDescriptor);
        this.crudMethod = handlerConfig.crudMethod;
        this.handlerClassSimpleName = this.collectionDescriptor.name() + handlerConfig.name();
        this.notAllowedCode = handlerConfig.notAllowedCode;
    }

    @Override
    public TypeSpec handler() throws IncoherentDescriptorException {
        if(this.action == null) return null;
        if(! collectionDescriptor.opt().types().entity().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without an entity type");
        if(! collectionDescriptor.opt().types().error().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without an error type");
        if(this.type == null) throw new IncoherentDescriptorException("cannot generate replace/update handler without an replace/update type");
        if(! collectionDescriptor.opt().types().message().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without an message type");
        if(! collectionDescriptor.opt().entityIdParam().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without an entityIdParam");
        if(! this.action.opt().requestValueObject().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without a request class");
        if(! this.action.opt().responseValueObject().isPresent()) throw new IncoherentDescriptorException("cannot generate replace/update handler without a response class");

        return TypeSpec.classBuilder(this.handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.action.requestValueObject()),
                        this.className(this.action.responseValueObject())
                ))

                .addField(FieldSpec.builder(ClassName.get(CategorizedLogger.class), "log", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("$T.getLogger($L.class)", CategorizedLogger.class, this.handlerClassSimpleName)
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
                        .addParameter(this.className(this.action.requestValueObject()), "request")
                        .returns(this.className(this.action.responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethod(this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"))
                .addMethod(this.errorResponseMethod("notAllowedError", "Status405", this.notAllowedCode))
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
                        this.className(this.type),
                        this.className(this.type)
                )

                //replace or update
                .addStatement("$T<$T> entity", Entity.class, this.className(this.collectionDescriptor.types().entity()))
                .beginControlFlow("try")
                    .addStatement("entity = adapter.crud().$L(request.entityId(), value)", this.crudMethod)
                .nextControlFlow("catch($T e)", BadRequestException.class)
                    .addStatement("return $T.builder().status400($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.action.responseValueObject()),
                            this.relatedClassName("Status400", this.action.responseValueObject())
                    )
                .nextControlFlow("catch($T e)", ForbiddenException.class)
                    .addStatement("return $T.builder().status403($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.action.responseValueObject()),
                            this.relatedClassName("Status403", this.action.responseValueObject())
                    )
                .nextControlFlow("catch($T e)", NotFoundException.class)
                    .addStatement("return $T.builder().status404($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.action.responseValueObject()),
                            this.relatedClassName("Status404", this.action.responseValueObject())
                    )
                .nextControlFlow("catch($T e)", UnauthorizedException.class)
                    .addStatement("return $T.builder().status401($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.action.responseValueObject()),
                            this.relatedClassName("Status401", this.action.responseValueObject())
                    )
                .nextControlFlow("catch($T e)", UnexpectedException.class)
                    .addStatement("return $T.builder().status500($T.builder().payload(this.casted(e.error())).build()).build()",
                            this.className(this.action.responseValueObject()),
                            this.relatedClassName("Status500", this.action.responseValueObject())
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
                        this.className(this.action.responseValueObject()),
                        this.relatedClassName("Status200", this.action.responseValueObject()),
                        "%s/%s"
                )
                .build();
    }
}
