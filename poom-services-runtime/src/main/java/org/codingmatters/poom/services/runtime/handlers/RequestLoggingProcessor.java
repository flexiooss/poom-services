package org.codingmatters.poom.services.runtime.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.runtime.handlers.delegate.LoggingResponseDelegate;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequestLoggingProcessor implements Processor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RequestLoggingProcessor.class);

    static public boolean isPayloadLogged(String contentType) {
        return contentType.startsWith("application/json")
                || contentType.startsWith("text/");
    }

    private final RequestLoggingStateProvider state;
    private final Processor decorated;
    private final ObjectMapper mapper;

    public RequestLoggingProcessor(Processor decorated, RequestLoggingStateProvider state, JsonFactory jsonFactory) {
        this.decorated = decorated;
        this.state = state;
        this.mapper = new ObjectMapper(jsonFactory);
    }

    private boolean shouldLog(RequestDelegate requestDelegate) {
        return this.state.shouldLog(requestDelegate);
    }

    @Override
    public void process(RequestDelegate requestDelegate, ResponseDelegate responseDelegate) throws IOException {
        if(this.shouldLog(requestDelegate)) {
            LoggingResponseDelegate loggingResponseDelegate = new LoggingResponseDelegate(responseDelegate);
            this.decorated.process(requestDelegate, loggingResponseDelegate);
            this.safelyLog(requestDelegate, loggingResponseDelegate);
        } else {
            this.decorated.process(requestDelegate, responseDelegate);
        }
    }

    private void safelyLog(RequestDelegate requestDelegate, LoggingResponseDelegate responseDelegate) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("request", this.requestData(requestDelegate));
        data.put("response", responseDelegate.data());
        log.request().info("{}", this.asString(data));
    }

    private Map<String, Object> requestData(RequestDelegate requestDelegate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", requestDelegate.method().name());
        result.put("content-type", requestDelegate.contentType());
        result.put("path", requestDelegate.path());
        result.put("headers", this.filtered(requestDelegate.headers()));
        result.put("query-parameters", requestDelegate.queryParameters());
        result.put("payload", this.requestPayload(requestDelegate));

        return result;
    }

    private Map<String, List<String>> filtered(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String name : headers.keySet()) {
            if(name.equalsIgnoreCase("authorization")) {
                result.put(name, Collections.singletonList("undisclosed"));
            } else {
                result.put(name, headers.get(name));
            }
        }
        return result;
    }

    private Object requestPayload(RequestDelegate requestDelegate) {
        if(requestDelegate.contentType() != null) {
            if(isPayloadLogged(requestDelegate.contentType())) {
                try(InputStream in = requestDelegate.payload()) {
                    return this.readTextPayload(in);
                } catch (IOException e) {
                    log.error("error reading text payload", e);
                    return "error reading text payload";
                }
            } else {
                return "payload not logged";
            }
        }
        return null;
    }

    private String readTextPayload(InputStream payload) throws IOException {
        StringBuilder result = new StringBuilder();
        try(Reader reader = new InputStreamReader(payload)) {
            char[] buffer = new char[1024];
            for(int read = reader.read(buffer) ; read != -1 ; read = reader.read(buffer)) {
                result.append(buffer, 0, read);
            }
        }
        return result.toString();
    }


    private String asString(Map<String, Object> request) {
        try {
            return this.mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("error formatting request " + request.toString(), e);
            return "error formatting request";
        }
    }
}
