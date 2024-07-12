package org.codingmatters.poom.paged.collection.generation.generators.source;

import com.squareup.javapoet.*;
import org.codingmatters.poom.generic.resource.domain.EntityRetriever;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;

import javax.lang.model.element.Modifier;
import java.util.Optional;
import java.util.function.Function;

public class RetrieveHandlerGenerator extends PagedCollectionHandlerGenerator {
    public RetrieveHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor, collectionDescriptor.retrieve());
    }

    @Override
    public TypeSpec handler() throws IncoherentDescriptorException {
        if(! collectionDescriptor.opt().retrieve().isPresent()) return null;
        if(! collectionDescriptor.opt().types().entity().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without an entity type");
        if(! collectionDescriptor.opt().types().error().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without an error type");
        if(! collectionDescriptor.opt().types().message().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without an message type");
//        if(! collectionDescriptor.opt().entityIdParam().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without an entityIdParam");
        if(! collectionDescriptor.opt().retrieve().requestValueObject().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without a request class");
        if(! collectionDescriptor.opt().retrieve().responseValueObject().isPresent()) throw new IncoherentDescriptorException("cannot generate retrieve handler without a response class");

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

                .addField(this.checkedEntityActionProviver(EntityRetriever.class, this.collectionDescriptor.types().entity()), "provider", Modifier.PRIVATE, Modifier.FINAL)

                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(this.checkedEntityActionProviver(EntityRetriever.class, this.collectionDescriptor.types().entity()), "provider")
                        .addStatement("this.provider = provider")
                        .build())

                .addMethod(MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
                        .addParameter(this.className(this.collectionDescriptor.retrieve().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.retrieve().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethod(this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"))
                .addMethod(this.errorResponseMethod("badRequestError", "Status400", "BAD_REQUEST"))
                .addMethod(this.entityNotFoundResponseMethod())
                .addMethod(this.castedErrorMethod())

                .build();
    }

    private CodeBlock applyBody() {
        CodeBlock.Builder result = CodeBlock.builder()
                //adapter
                .addStatement("$T<$T> action", EntityRetriever.class, this.className(this.collectionDescriptor.types().entity()))
                .beginControlFlow("try")
                .addStatement("action = this.provider.action(request)")
                .nextControlFlow("catch($T e)", Exception.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "failed getting action for ")
                .addStatement("return this.unexpectedError(token)")
                .endControlFlow();

        if(this.collectionDescriptor.entityIdParam() != null) {
            result
                    //request validation
                    .beginControlFlow("if(! request.opt().$L().isPresent())", this.entityProperty())
                    .addStatement("$T token = log.tokenized().info($S, request)",
                            String.class, "no entity id provided to update entity : {}"
                    )
                    .addStatement("return this.badRequestError(token)")
                    .endControlFlow()
            ;
        }
        result
                //retrieve
                .addStatement("$T<$T<$T>> entity", Optional.class, Entity.class, this.className(this.collectionDescriptor.types().entity()))
                .beginControlFlow("try")
                ;
        if(this.collectionDescriptor.entityIdParam() != null) {
            result.addStatement("entity = action.retrieveEntity(request.$L())", this.entityProperty());
        } else {
            result.addStatement("entity = action.retrieveEntity(null)");
        }
        result
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
                .nextControlFlow("catch($T e)", MethodNotAllowedException.class)
                .addStatement("return $T.builder().status405($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                        this.relatedClassName("Status405", this.collectionDescriptor.retrieve().responseValueObject())
                )
                .endControlFlow()

                //nominal
                .beginControlFlow("if(entity.isPresent())")
                    .addStatement("return $T.builder().status200($T.builder()" +
                                    ".xEntityId(entity.get().id())" +
                                    ".xEntityType(action.entityType())" +
                                    ".location(String.format($S, action.entityRepositoryUrl(), entity.get().id()))" +
                                    ".payload(entity.get().value())" +
                                    ".build()).build()",
                            this.className(this.collectionDescriptor.retrieve().responseValueObject()),
                            this.relatedClassName("Status200", this.collectionDescriptor.retrieve().responseValueObject()),
                            "%s/%s"
                    )
                .nextControlFlow("else")
                    .addStatement("$T token = log.tokenized().info($S, action.entityRepositoryUrl(), request)",
                            String.class, "no entity found in repository {} for request {}"
                    )
                ;
        if(this.collectionDescriptor.entityIdParam() != null) {
            result.addStatement("return this.entityNotFound(request.$L(), token)", this.entityProperty());
        } else {
            result.addStatement("return this.entityNotFound(null, token)");
        }
        result.endControlFlow();
        return result.build();
    }

}
