package org.codingmatters.poom.paged.collection.generation.generators.source;

import com.squareup.javapoet.*;
import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.spec.Action;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;

import javax.lang.model.element.Modifier;

public abstract class PagedCollectionHandlerGenerator {
    protected final PagedCollectionDescriptor collectionDescriptor;
    private final Action handlerAction;

    public PagedCollectionHandlerGenerator(PagedCollectionDescriptor collectionDescriptor, Action handlerAction) {
        this.collectionDescriptor = collectionDescriptor;
        this.handlerAction = handlerAction;
    }

    public abstract TypeSpec handler() throws IncoherentDescriptorException;

    protected ParameterizedTypeName adapterProviderClass() {
        return ParameterizedTypeName.get(
                ClassName.get(PagedCollectionAdapter.Provider.class),
                this.orVoid(this.className(this.collectionDescriptor.types().entity())),
                this.orVoid(this.className(this.collectionDescriptor.types().create())),
                this.orVoid(this.className(this.collectionDescriptor.types().replace())),
                this.orVoid(this.className(this.collectionDescriptor.types().update()))
        );
    }

    private TypeName orVoid(ClassName className) {
        return className != null ? className : ClassName.get(Void.class);
    }

    protected ParameterizedTypeName adapterClass() {
        return ParameterizedTypeName.get(
                ClassName.get(PagedCollectionAdapter.class),
                this.orVoid(this.className(this.collectionDescriptor.types().entity())),
                this.orVoid(this.className(this.collectionDescriptor.types().create())),
                this.orVoid(this.className(this.collectionDescriptor.types().replace())),
                this.orVoid(this.className(this.collectionDescriptor.types().update()))
        );
    }

    protected ClassName className(String fqClassName) {
        if(fqClassName == null) return null;
        return ClassName.get(this.packageOf(fqClassName), this.simpleNameOf(fqClassName));
    }

    protected ClassName relatedClassName(String simpleName, String relatedToFq) {
        return ClassName.get(
                this.packageOf(relatedToFq) + "." + this.simpleNameOf(relatedToFq).toLowerCase(),
                simpleName
        );
    }

    protected String packageOf(String fqClassName) {
        if(fqClassName == null) return null;
        return fqClassName.substring(0, fqClassName.lastIndexOf('.'));
    }

    protected String simpleNameOf(String fqClassName) {
        if(fqClassName == null) return null;
        return fqClassName.substring(fqClassName.lastIndexOf('.') + 1);
    }
    protected MethodSpec errorResponseMethod(String methodName, String status, String errorCode) {
        return MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "token")
                .returns(this.className(this.handlerAction.responseValueObject()))
                .addCode(CodeBlock.builder()
                        .addStatement(
                                "return $T.builder().$L($T.builder().payload($T.builder()" +
                                        ".code($T.Code.$L)" +
                                        ".token(token)" +
                                        ".messages($T.builder().key($S).args(token).build())" +
                                        ".build()).build()).build()",
                                this.className(this.handlerAction.responseValueObject()),
                                status.substring(0, 1).toLowerCase() + status.substring(1),
                                this.relatedClassName(status, this.handlerAction.responseValueObject()),
                                this.className(this.collectionDescriptor.types().error()),
                                this.className(this.collectionDescriptor.types().error()),
                                errorCode,
                                this.className(this.collectionDescriptor.types().message()),
                                MessageKeys.SEE_LOGS_WITH_TOKEN
                        )
                        .build())
                .build();
    }

    protected MethodSpec castedErrorMethod() {
        return MethodSpec.methodBuilder("casted").addModifiers(Modifier.PRIVATE)
                .addParameter(Error.class, "error").returns(this.className(this.collectionDescriptor.types().error()))
                .addStatement("return $T.fromMap(error.toMap()).build()", this.className(this.collectionDescriptor.types().error()))
                .build();
    }

    protected MethodSpec entityNotFoundResponseMethod() {
        return MethodSpec.methodBuilder("entityNotFound").addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "entityId")
                .addParameter(String.class, "token")
                .returns(this.className(this.handlerAction.responseValueObject()))
                .addCode(CodeBlock.builder()
                        .addStatement(
                                "return $T.builder().status404($T.builder().payload($T.builder()" +
                                        ".code($T.Code.RESOURCE_NOT_FOUND)" +
                                        ".token(token)" +
                                        ".messages(" +
                                            "$T.builder().key($S).args(entityId).build()," +
                                            "$T.builder().key($S).args(token).build()" +
                                        ")" +
                                        ".build()).build()).build()",
                                this.className(this.handlerAction.responseValueObject()),
                                this.relatedClassName("Status404", this.handlerAction.responseValueObject()),
                                this.className(this.collectionDescriptor.types().error()),
                                this.className(this.collectionDescriptor.types().error()),
                                this.className(this.collectionDescriptor.types().message()),
                                MessageKeys.ENTITY_NOT_FOUND,
                                this.className(this.collectionDescriptor.types().message()),
                                MessageKeys.SEE_LOGS_WITH_TOKEN
                        )
                        .build())
                .build();
    }

}
