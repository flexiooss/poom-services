package org.codingmatters.poom.paged.collection.generation.generators.source;

import com.squareup.javapoet.*;
import org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse;
import org.codingmatters.poom.generic.resource.domain.BatchEntityCreator;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

public class BatchCreateHandlerGenerator extends PagedCollectionHandlerGenerator {

    public BatchCreateHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor, collectionDescriptor.batchCreate());
    }

    @Override
    public TypeSpec handler() throws IncoherentDescriptorException {
        if (!collectionDescriptor.opt().batchCreate().isPresent()) return null;
        if (!collectionDescriptor.opt().types().batchCreateResponse().isPresent())
            throw new IncoherentDescriptorException("cannot generate batch create handler without a BatchCreateResponse type");
        if (!collectionDescriptor.opt().types().error().isPresent())
            throw new IncoherentDescriptorException("cannot generate batch create handler without an error type");
        if (!collectionDescriptor.opt().types().message().isPresent())
            throw new IncoherentDescriptorException("cannot generate batch create handler without an message type");
        if (!collectionDescriptor.opt().batchCreate().requestValueObject().isPresent())
            throw new IncoherentDescriptorException("cannot generate batch create handler without request class");
        if (!collectionDescriptor.opt().batchCreate().responseValueObject().isPresent())
            throw new IncoherentDescriptorException("cannot generate batch create handler without response class");

        String handlerClassSimpleName = this.collectionDescriptor.name() + "BatchCreate";
        return TypeSpec.classBuilder(handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.collectionDescriptor.batchCreate().requestValueObject()),
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject())
                ))

                .addField(FieldSpec.builder(ClassName.get(CategorizedLogger.class), "log", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("$T.getLogger($L.class)", CategorizedLogger.class, handlerClassSimpleName)
                        .build())

                .addField(this.checkedEntityActionProviver(BatchEntityCreator.class, this.collectionDescriptor.types().create()), "provider", Modifier.PRIVATE, Modifier.FINAL)

                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(this.checkedEntityActionProviver(BatchEntityCreator.class, this.collectionDescriptor.types().create()), "provider")
                        .addStatement("this.provider = provider")
                        .build())


                .addMethod(MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
                        .addParameter(this.className(this.collectionDescriptor.batchCreate().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.batchCreate().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethod(this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"))
                .addMethod(this.errorResponseMethod("notAllowedError", "Status405", "ENTITY_CREATION_NOT_ALLOWED"))
                .addMethod(this.castedErrorMethod())

                .build();
    }

    private CodeBlock applyBody() {
//        ValueList<ObjectValue> list = ValueList.<ObjectValue>builder().build();
        return CodeBlock.builder()
                // action
                .addStatement("$T<$T> action",
                        BatchEntityCreator.class,
                        this.className(this.collectionDescriptor.types().create())
                )
                .beginControlFlow("try")
                .addStatement("action = this.provider.action(request)")
                .nextControlFlow("catch($T e)", Exception.class)
                .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "failed getting action for ")
                .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //creation
                .addStatement("$T<$T> values = request.opt().payload().orElseGet(() -> $T.<$T>builder().build())",
                        this.className(this.collectionDescriptor.types().valueList()),
                        this.className(this.collectionDescriptor.types().create()),
                        this.className(this.collectionDescriptor.types().valueList()),
                        this.className(this.collectionDescriptor.types().create())
                )
                .addStatement("$T response = null", this.className(this.collectionDescriptor.types().batchCreateResponse()))
                .beginControlFlow("try")
                .addStatement("$T rawResponse = action.createEntitiesFrom(values.toArray(new $T[0]))",
                        BatchCreateResponse.class,
                        this.className(this.collectionDescriptor.types().create())
                )
                .beginControlFlow("if(rawResponse != null)")
                .addStatement("response = $T.fromMap(rawResponse.toMap()).build()",
                        this.className(this.collectionDescriptor.types().batchCreateResponse())
                )
                .nextControlFlow("else")

                .endControlFlow()
                .nextControlFlow("catch($T e)", BadRequestException.class)
                .addStatement("return $T.builder().status400($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status400", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .nextControlFlow("catch($T e)", ForbiddenException.class)
                .addStatement("return $T.builder().status403($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status403", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .nextControlFlow("catch($T e)", NotFoundException.class)
                .addStatement("return $T.builder().status404($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status404", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .nextControlFlow("catch($T e)", UnauthorizedException.class)
                .addStatement("return $T.builder().status401($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status401", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .nextControlFlow("catch($T e)", UnexpectedException.class)
                .addStatement("return $T.builder().status500($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status500", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .nextControlFlow("catch($T e)", MethodNotAllowedException.class)
                .addStatement("return $T.builder().status405($T.builder().payload(this.casted(e.error())).build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status405", this.collectionDescriptor.batchCreate().responseValueObject())
                )
                .endControlFlow()

                //failure
                .beginControlFlow("if(response == null)")
                .addStatement("$T token = log.tokenized().error($S, this.provider.getClass(), request)",
                        String.class, "provider {} implementation breaks contract, created entity is null for {}"
                )
                .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                //nominal
                .beginControlFlow("if(response.opt().errors().isEmpty() || response.errors().isEmpty())")
                .addStatement("return $T.builder().status201($T.builder()" +
                                ".xEntityType(action.entityType())" +
                                ".location(String.format($S, action.entityRepositoryUrl()))" +
                                ".payload(response)" +
                                ".build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status201", this.collectionDescriptor.batchCreate().responseValueObject()),
                        "%s"
                )
                .nextControlFlow("else")
                .addStatement("return $T.builder().status207($T.builder()" +
                                ".xEntityType(action.entityType())" +
                                ".location(String.format($S, action.entityRepositoryUrl()))" +
                                ".payload(response)" +
                                ".build()).build()",
                        this.className(this.collectionDescriptor.batchCreate().responseValueObject()),
                        this.relatedClassName("Status207", this.collectionDescriptor.batchCreate().responseValueObject()),
                        "%s"
                )
                .endControlFlow()
                .build();
    }
}
