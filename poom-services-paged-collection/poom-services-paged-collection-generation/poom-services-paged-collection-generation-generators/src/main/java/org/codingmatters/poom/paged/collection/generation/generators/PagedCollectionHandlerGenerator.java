package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;

public abstract class PagedCollectionHandlerGenerator {
    protected final PagedCollectionDescriptor collectionDescriptor;

    public PagedCollectionHandlerGenerator(PagedCollectionDescriptor collectionDescriptor) {
        this.collectionDescriptor = collectionDescriptor;
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
}
