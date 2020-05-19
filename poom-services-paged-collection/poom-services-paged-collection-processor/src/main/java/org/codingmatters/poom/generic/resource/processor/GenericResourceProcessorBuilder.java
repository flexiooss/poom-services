package org.codingmatters.poom.generic.resource.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.api.paged.collection.processor.GenericResourceProcessor;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.handlers.GenericResourceHandlersBuilder;
import org.codingmatters.poom.generic.resource.processor.internal.InterceptedResourcesProcessor;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.HashMap;
import java.util.Map;

public class GenericResourceProcessorBuilder {

    private final Map<String, Processor> resourceProcessors = new HashMap<>();
    private final Map<String, Processor> resourcePreProcessors = new HashMap<>();
    private final JsonFactory jsonFactory;
    private final String apiPath;

    public GenericResourceProcessorBuilder(String apiPath, JsonFactory jsonFactory) {
        this.apiPath = this.normalized(apiPath);
        this.jsonFactory = jsonFactory;
    }

    public Processor build(Processor fallbackProcessor) {
        return new InterceptedResourcesProcessor(this.apiPath, new HashMap<>(this.resourceProcessors), new HashMap<>(this.resourcePreProcessors), fallbackProcessor);
    }

    public Processor build() {
        return this.build(null);
    }

    public GenericResourceProcessorBuilder resourceAt(
            String basePattern,
            Processor preProcessor,
            GenericResourceAdapter.Provider<ObjectValue, ObjectValue, ObjectValue, ObjectValue> adapterProvider
    ) {
        if(basePattern.isEmpty() || basePattern.equals("/")) {
            basePattern = "";
        } else if(! basePattern.startsWith("/")) {
            basePattern = "/" + basePattern;
        }

        this.resourceProcessors.put(
                basePattern,
                this.buildResourceProcessor(adapterProvider, basePattern)
        );
        if(preProcessor != null) {
            this.resourcePreProcessors.put(basePattern, preProcessor);
        }

        return this;
    }

    public GenericResourceProcessorBuilder resourceAt(
            String basePattern,
            GenericResourceAdapter.Provider<ObjectValue, ObjectValue, ObjectValue, ObjectValue> adapterProvider
    ) {
        return this.resourceAt(basePattern, null, adapterProvider);
    }

    private String normalized(String apiPath) {
        if(apiPath == null || apiPath.isEmpty()) return "";
        while (apiPath.startsWith("/")) {
            apiPath = apiPath.substring(1);
        }
        while (apiPath.endsWith("/")) {
            apiPath = apiPath.substring(apiPath.length() - 1);
        }
        return apiPath.isEmpty() ? "" : "/" + apiPath;
    }

    protected Processor buildResourceProcessor(GenericResourceAdapter.Provider adapterProvider, String pathPattern) {
        return new GenericResourceProcessor(
                (this.apiPath + pathPattern).replaceAll("\\{[^\\}]+\\}", "[^/]+"),
                this.jsonFactory,
                new GenericResourceHandlersBuilder(adapterProvider).build());
    }
}
