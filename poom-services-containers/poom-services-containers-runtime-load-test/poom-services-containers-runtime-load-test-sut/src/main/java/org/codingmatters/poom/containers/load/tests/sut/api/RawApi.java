package org.codingmatters.poom.containers.load.tests.sut.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.containers.load.tests.sut.api.types.Struct;
import org.codingmatters.poom.containers.load.tests.sut.api.types.json.StructReader;
import org.codingmatters.poom.containers.load.tests.sut.api.types.json.StructWriter;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class RawApi implements Api {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RawApi.class);

    private final Processor processor;
    private final JsonFactory jsonFactory;

    public RawApi(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
        this.processor = this::process;
    }

    private void process(RequestDelegate request, ResponseDelegate response) {
        Struct struct = null;

        String qparam = null;
        List<String> qparams = request.queryParameters().getOrDefault("qparam", Collections.emptyList());
        if(! qparams.isEmpty()) {
            qparam = qparams.get(0);
        }

        String uparam = null;
        if(request.pathMatcher(".*/structured/[^/]+/?").matches()) {
            List<String> uparams = request.uriParameters(".*/structured/{uparam}/?").getOrDefault("uparam", Collections.emptyList());
            if(! uparams.isEmpty()) {
                uparam = uparams.get(0);
            }
        }

        if(request.method().equals(RequestDelegate.Method.POST)) {
            if(request.pathMatcher(".*/file.*").matches()) {
                try(InputStream in = request.payload()) {
                    byte [] buffer = new byte[1024];
                    int total = 0;
                    for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                        total += read;
                    }
                    struct = Struct.builder().prop("read %s bytes", total).build();
                } catch (IOException e) {
                    log.error("error reading body", e);
                }
            } else {
                try(InputStream in = request.payload(); JsonParser parser = this.jsonFactory.createParser(in)) {
                    struct = new StructReader().read(parser);
                } catch (JsonParseException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            struct = Struct.builder().prop("uparam=%s ; qparam=%s", uparam, qparam).build();
        }

        String responsePayload = "";
        try(OutputStream out = new ByteArrayOutputStream(); ) {
            try(JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
                new StructWriter().write(generator, struct);
            }
            responsePayload = out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        response.status(200);
        response.contenType("application/json");
        response.payload(responsePayload, "utf-8");
    }

    @Override
    public String name() {
        return "raw";
    }

    @Override
    public String version() {
        return Api.versionFrom(RawApi.class);
    }

    @Override
    public Processor processor() {
        return this.processor;
    }
}
