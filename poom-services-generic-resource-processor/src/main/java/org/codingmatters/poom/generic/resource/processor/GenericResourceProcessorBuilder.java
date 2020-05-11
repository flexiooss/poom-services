package org.codingmatters.poom.generic.resource.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.api.generic.resource.processor.GenericResourceProcessor;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.handlers.GenericResourceHandlersBuilder;
import org.codingmatters.poom.generic.resource.processor.internal.InterceptedResourcesProcessor;
import org.codingmatters.rest.api.Processor;

import java.util.HashMap;
import java.util.Map;

public class GenericResourceProcessorBuilder {

    private final Map<String, Processor> resourceProcessors = new HashMap<>();
    private final Map<String, Processor> resourcePreProcessors = new HashMap<>();
    private final JsonFactory jsonFactory;
    private final String apiPath;

    public GenericResourceProcessorBuilder(String apiPath, JsonFactory jsonFactory) {
        this.apiPath = apiPath;
        this.jsonFactory = jsonFactory;
    }

    public Processor build(Processor fallbackProcessor) {
        return new InterceptedResourcesProcessor(this.apiPath, new HashMap<>(this.resourceProcessors), new HashMap<>(this.resourcePreProcessors), fallbackProcessor);
    }
    public Processor build() {
        return this.build(null);
    }

    public GenericResourceProcessorBuilder preprocessedResourceAt(String basePattern, Processor preProcessor, GenericResourceAdapter.Provider adapterProvider) {
        if(basePattern.isEmpty() || basePattern.equals("/")) {
            basePattern = "";
        } else if(! basePattern.startsWith("/")) {
            basePattern = "/" + basePattern;
        }

        String pathPattern = basePattern.replaceAll("\\{\\w+\\}", "[^/]*");

        Processor resourceProcessor = this.buildResourceProcessor(adapterProvider, pathPattern);

        this.resourceProcessors.put(
                basePattern,
                resourceProcessor
        );
        if(preProcessor != null) {
            this.resourcePreProcessors.put(basePattern, preProcessor);
        }

        return this;
    }

    public GenericResourceProcessorBuilder resourceAt(String basePattern, GenericResourceAdapter.Provider adapterProvider) {
        return this.preprocessedResourceAt(basePattern, null, adapterProvider);
    }

    protected Processor buildResourceProcessor(GenericResourceAdapter.Provider adapterProvider, String pathPattern) {
        return new GenericResourceProcessor(this.apiPath + pathPattern, this.jsonFactory, new GenericResourceHandlersBuilder(adapterProvider).build());
    }
}
