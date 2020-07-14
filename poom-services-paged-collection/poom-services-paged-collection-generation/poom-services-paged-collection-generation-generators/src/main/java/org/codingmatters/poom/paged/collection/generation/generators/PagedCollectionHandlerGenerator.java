package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.*;
import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
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

    public abstract TypeSpec handler();

    protected ParameterizedTypeName adapterProviderClass() {
        return ParameterizedTypeName.get(
                ClassName.get(PagedCollectionAdapter.Provider.class),
                this.className(this.collectionDescriptor.types().entity()),
                this.className(this.collectionDescriptor.types().create()),
                this.className(this.collectionDescriptor.types().replace()),
                this.className(this.collectionDescriptor.types().update())
        );
    }

    protected ParameterizedTypeName adapterClass() {
        return ParameterizedTypeName.get(
                ClassName.get(PagedCollectionAdapter.class),
                this.className(this.collectionDescriptor.types().entity()),
                this.className(this.collectionDescriptor.types().create()),
                this.className(this.collectionDescriptor.types().replace()),
                this.className(this.collectionDescriptor.types().update())
        );
    }

    protected ClassName className(String fqClassName) {
        return ClassName.get(this.packageOf(fqClassName), this.simpleNameOf(fqClassName));
    }

    protected ClassName relatedClassName(String simpleName, String relatedToFq) {
        return ClassName.get(
                this.packageOf(relatedToFq) + "." + this.simpleNameOf(relatedToFq).toLowerCase(),
                simpleName
        );
    }

    protected String packageOf(String fqClassName) {
        return fqClassName.substring(0, fqClassName.lastIndexOf('.'));
    }

    protected String simpleNameOf(String fqClassName) {
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
}
