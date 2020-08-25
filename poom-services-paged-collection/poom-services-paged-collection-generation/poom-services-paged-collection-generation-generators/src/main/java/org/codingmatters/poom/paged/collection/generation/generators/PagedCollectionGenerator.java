package org.codingmatters.poom.paged.collection.generation.generators;

import com.squareup.javapoet.TypeSpec;
import org.codingmatters.poom.paged.collection.generation.generators.raml.PagedCollectionDescriptorFromRamlParser;
import org.codingmatters.poom.paged.collection.generation.generators.source.*;
import org.codingmatters.poom.paged.collection.generation.generators.source.exception.IncoherentDescriptorException;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.rest.api.generator.exception.RamlSpecException;
import org.codingmatters.value.objects.generation.GenerationUtils;
import org.raml.v2.api.RamlModelResult;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PagedCollectionGenerator {
    private final RamlModelResult ramlModel;
    private final String apiPackage;
    private final String typesPackage;

    public PagedCollectionGenerator(RamlModelResult ramlModel, String apiPackage, String typesPackage) {
        this.ramlModel = ramlModel;
        this.apiPackage = apiPackage;
        this.typesPackage = typesPackage;
    }

    public void generate(File to) throws RamlSpecException, IncoherentDescriptorException, IOException {
        List<TypeSpec> handlerSpecs = new LinkedList<>();

        for (PagedCollectionDescriptor collectionDescriptor : new PagedCollectionDescriptorFromRamlParser(this.ramlModel, this.apiPackage, this.typesPackage).parse()) {
            handlerSpecs.addAll(this.generateSpecs(collectionDescriptor));
        }

        for (TypeSpec handlerSpec : handlerSpecs) {
            GenerationUtils.writeJavaFile(
                to,
                this.apiPackage + ".collection.handlers",
                handlerSpec
            );
        }


    }

    private List<TypeSpec> generateSpecs(PagedCollectionDescriptor collectionDescriptor) throws IncoherentDescriptorException {
        List<TypeSpec> handlerSpecs = new LinkedList<>();
        TypeSpec handlerSpec = new BrowseHandlerGenerator(collectionDescriptor).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        handlerSpec = new CreateHandlerGenerator(collectionDescriptor).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        handlerSpec = new RetrieveHandlerGenerator(collectionDescriptor).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        handlerSpec = new ReplaceOrUpdateHandlerGenerator(collectionDescriptor, ReplaceOrUpdateHandlerGenerator.HandlerConfig.Replace).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        handlerSpec = new ReplaceOrUpdateHandlerGenerator(collectionDescriptor, ReplaceOrUpdateHandlerGenerator.HandlerConfig.Update).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        handlerSpec = new DeleteHandlerGenerator(collectionDescriptor).handler();
        if(handlerSpec != null) {
            handlerSpecs.add(handlerSpec);
        }
        return handlerSpecs;
    }
}
