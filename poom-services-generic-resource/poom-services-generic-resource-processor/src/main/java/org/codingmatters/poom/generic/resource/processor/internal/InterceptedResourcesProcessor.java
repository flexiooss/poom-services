package org.codingmatters.poom.generic.resource.processor.internal;

import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;

public class InterceptedResourcesProcessor implements Processor {
    private static final Processor NOOP = (requestDelegate, responseDelegate) -> {};
    private final String apiPath;
    private final HashMap<String, Processor> processorForPattern;
    private final HashMap<String, Processor> preProcessorForPattern;
    private final Optional<Processor> fallbackProcessor;

    public InterceptedResourcesProcessor(String apiPath, HashMap<String, Processor> processorForPattern, HashMap<String, Processor> preProcessorForPattern, Processor fallbackProcessor) {
        this.apiPath = apiPath;
        this.processorForPattern = processorForPattern;
        this.preProcessorForPattern = preProcessorForPattern;
        this.fallbackProcessor = Optional.ofNullable(fallbackProcessor);
    }

    @Override
    public void process(RequestDelegate requestDelegate, ResponseDelegate responseDelegate) throws IOException {
        for (String pattern : this.processorForPattern.keySet()) {
            String matcherPattern = pattern.replaceAll("\\{[^\\}]+\\}", "[^/]+");
            Matcher matcher = requestDelegate.pathMatcher(this.apiPath + matcherPattern + "(/[^/]*/?|/?)");
            if (matcher.matches()) {
                this.preProcessorForPattern.getOrDefault(pattern, NOOP).process(requestDelegate, responseDelegate);
                this.processorForPattern.get(pattern).process(requestDelegate, responseDelegate);
                return;
            }
        }
        if (this.fallbackProcessor.isPresent()) {
            this.fallbackProcessor.get().process(requestDelegate, responseDelegate);
        }
    }
}