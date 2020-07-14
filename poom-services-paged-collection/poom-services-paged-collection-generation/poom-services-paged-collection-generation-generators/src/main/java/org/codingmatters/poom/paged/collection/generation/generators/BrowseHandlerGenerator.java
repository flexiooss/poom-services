package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.*;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class BrowseHandlerGenerator extends PagedCollectionHandlerGenerator {

    public BrowseHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        super(collectionDescriptor);
    }

    @Override
    public TypeSpec handler() {
        String handlerClassSimpleName = this.collectionDescriptor.name() + "Browse";
        return TypeSpec.classBuilder(handlerClassSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        this.className(this.collectionDescriptor.browse().requestValueObject()),
                        this.className(this.collectionDescriptor.browse().responseValueObject())
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
                        .addParameter(this.className(this.collectionDescriptor.browse().requestValueObject()), "request")
                        .returns(this.className(this.collectionDescriptor.browse().responseValueObject()))
                        .addCode(this.applyBody())
                        .build())

                .addMethods(this.privateMethods())

                .build();
    }

    private CodeBlock constructorBody() {
        return CodeBlock.builder()
                .addStatement("this.adapterProvider = adapterProvider")
                .build();
    }

    private CodeBlock applyBody() {
        return CodeBlock.builder()

                .addStatement("$T adapter", this.adapterClass())
                .beginControlFlow("try")
                    .addStatement("adapter = this.adapterProvider.adapter()")
                .nextControlFlow("catch($T e)", Exception.class)
                    .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "failed getting adapter for ")
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                .addStatement("$T pager = adapter.pager()", this.pagerClass())
                .beginControlFlow("if(pager == null)")
                    .addStatement("$T token = log.tokenized().info($S, adapter.getClass(), request)",
                            String.class, "adapter {} has no pager, browsing method not allowed, request was : {}"
                    )
                    .addStatement("return this.browsingNotAllowed(token)")
                .endControlFlow()

                .beginControlFlow("if(pager.unit() == null)")
                    .addStatement("$T token = log.tokenized().error($S, adapter.getClass(), request)",
                            String.class, "adapter {} implementation breaks contract, pager unit cannot be null, request was : {}"
                    )
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                .beginControlFlow("if(pager.lister() == null)")
                    .addStatement("$T token = log.tokenized().error($S, adapter.getClass(), request)",
                            String.class, "adapter {} implementation breaks contract, pager lister cannot be null, request was : {}"
                    )
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                .beginControlFlow("if(pager.maxPageSize() <= 0)")
                    .addStatement("$T token = log.tokenized().error($S, adapter.getClass(), request)",
                            String.class, "adapter {} implementation breaks contract, pager max page size  cannot be lower or equal to 0, request was : {}"
                    )
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                .addStatement("$T page", this.pageClass())
                .beginControlFlow("try")
                    .addStatement("page = $T.forRequestedRange(request.range())" +
                                    ".unit(pager.unit())" +
                                    ".maxPageSize(pager.maxPageSize())" +
                                    ".pager(pager.lister())" +
                                    ".page(this.parseQuery(request))",
                            Rfc7233Pager.class
                    )
                .nextControlFlow("catch($T e)", RepositoryException.class)
                    .addStatement("$T token = log.tokenized().error($S + request, e)", String.class, "unexpected error listing entities ")
                    .addStatement("return this.unexpectedError(token)")
                .endControlFlow()

                // invalid page request
                .beginControlFlow("if(! page.isValid())")
                    .addStatement("$T token = log.tokenized().info($S, request)",
                            String.class, "illegal search for entities, request was {}"
                    )
                .addStatement("$T.builder().status416($T.builder()" +
                        ".acceptRange(page.acceptRange())" +
                        ".contentRange(page.contentRange())" +
                        ".payload($T.builder()" +
                            ".code($T.Code.ILLEGAL_RANGE_SPEC)" +
                            ".token(token)" +
                            ".messages(" +
                                "$T.builder().key($S).args(request.range(), request.filter(), request.orderBy()).build()," +
                                "$T.builder().key($S).args(token).build()" +
                            ")" +
                            ".build())" +
                        ".build()).build()",
                        this.className(this.collectionDescriptor.browse().responseValueObject()),
                        this.relatedClassName("Status416", this.collectionDescriptor.browse().responseValueObject()),
                        this.className(this.collectionDescriptor.types().error()),
                        this.className(this.collectionDescriptor.types().error()),
                        this.className(this.collectionDescriptor.types().message()), MessageKeys.ILLEGAL_SEARCH_QUERY,
                        this.className(this.collectionDescriptor.types().message()), MessageKeys.SEE_LOGS_WITH_TOKEN
                )
                .endControlFlow()

                // nominal
                .beginControlFlow("if(page.isPartial())")
                    .addStatement("return $T.builder().status206($T.builder()" +
                            ".acceptRange(page.acceptRange())" +
                            ".contentRange(page.contentRange())" +
                            ".payload(page.list().valueList())" +
                            ".build()).build()",
                            this.className(this.collectionDescriptor.browse().responseValueObject()),
                            this.relatedClassName("Status206", this.collectionDescriptor.browse().responseValueObject())
                            )
                .nextControlFlow("else")
                    .addStatement("return $T.builder().status200($T.builder()" +
                                ".acceptRange(page.acceptRange())" +
                                ".contentRange(page.contentRange())" +
                                ".payload(page.list().valueList())" +
                                ".build()).build()",
                        this.className(this.collectionDescriptor.browse().responseValueObject()),
                        this.relatedClassName("Status200", this.collectionDescriptor.browse().responseValueObject())
                )
                .endControlFlow()
                .build();
    }

    private Iterable<MethodSpec> privateMethods() {
        return Arrays.asList(
                MethodSpec.methodBuilder("parseQuery").addModifiers(Modifier.PRIVATE)
                        .addParameter(this.className(this.collectionDescriptor.browse().requestValueObject()), "request")
                        .returns(ParameterizedTypeName.get(Optional.class, PropertyQuery.class))
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("if(request.opt().filter().isPresent() || request.opt().orderBy().isPresent())")
                                .addStatement("return $T.of($T.builder().filter(request.filter()).sort(request.orderBy()).build())", Optional.class, PropertyQuery.class)
                                .nextControlFlow("else")
                                .addStatement("return $T.empty()", Optional.class)
                                .endControlFlow()
                                .build())
                        .build(),
                this.errorResponseMethod("unexpectedError", "Status500", "UNEXPECTED_ERROR"),
                this.errorResponseMethod("browsingNotAllowed", "Status405", "COLLECTION_BROWSING_NOT_ALLOWED")
        );
    }

    protected MethodSpec errorResponseMethod(String methodName, String status, String errorCode) {
        return MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "token")
                .returns(this.className(this.collectionDescriptor.browse().responseValueObject()))
                .addCode(CodeBlock.builder()
                        .addStatement(
                                "return $T.builder().$L($T.builder().payload($T.builder()" +
                                        ".code($T.Code.$L)" +
                                        ".token(token)" +
                                        ".messages($T.builder().key($S).args(token).build())" +
                                        ".build()).build()).build()",
                                this.className(this.collectionDescriptor.browse().responseValueObject()),
                                status.substring(0, 1).toLowerCase() + status.substring(1),
                                this.relatedClassName(status, this.collectionDescriptor.browse().responseValueObject()),
                                this.className(this.collectionDescriptor.types().error()),
                                this.className(this.collectionDescriptor.types().error()),
                                errorCode,
                                this.className(this.collectionDescriptor.types().message()),
                                MessageKeys.SEE_LOGS_WITH_TOKEN
                        )
                        .build())
                .build();
    }

    private ParameterizedTypeName pagerClass() {
        return ParameterizedTypeName.get(
                ClassName.get(PagedCollectionAdapter.Pager.class),
                this.className(this.collectionDescriptor.types().entity())
        );
    }

    private ParameterizedTypeName pageClass() {
        return ParameterizedTypeName.get(
                ClassName.get(Rfc7233Pager.Page.class),
                this.className(this.collectionDescriptor.types().entity())
        );
    }
}
