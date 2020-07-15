package org.codingmatters.poom.paged.collection.generation.generators.source;

import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.raml.v2.api.RamlModelResult;

public class PagedCollectionDescriptorFromRamlParser {
    private final RamlModelResult ramlModel;

    public PagedCollectionDescriptorFromRamlParser(RamlModelResult ramlModel) {
        this.ramlModel = ramlModel;
    }

    public PagedCollectionDescriptor parse() {
        return null;
    }
}
