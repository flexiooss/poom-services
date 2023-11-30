package org.codingmatters.poom.containers.load.tests.sut.api;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.containers.load.tests.sut.api.processor.SutProcessor;
import org.codingmatters.poom.containers.load.tests.sut.api.structuredcontentgetresponse.Status200;
import org.codingmatters.poom.containers.load.tests.sut.api.types.Struct;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class SutApi implements Api {
    private final Processor processor;

    public SutApi(JsonFactory jsonFactory) {
        this.processor = new SutProcessor(this.path(), jsonFactory, new SutHandlers.Builder()
                .structuredContentGetHandler(request -> StructuredContentGetResponse.builder()
                        .status200(Status200.builder().payload(Struct.builder()
                                        .prop("uparam=%s ; qparam=%s", request.uparam(), request.qparam())
                                .build()).build())
                        .build())
                .structuredContentPostHandler(request -> StructuredContentPostResponse.builder()
                        .status200(org.codingmatters.poom.containers.load.tests.sut.api.structuredcontentpostresponse.Status200.builder()
                                .payload(request.payload())
                                .build())
                        .build())
                .fileContentPostHandler(request -> {
                    int total = 0;
                    try(InputStream in = request.payload().content().asStream()) {
                        byte[] buffer = new byte[1024];
                        for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                            total += read;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return FileContentPostResponse.builder()
                            .status200(org.codingmatters.poom.containers.load.tests.sut.api.filecontentpostresponse.Status200.builder()
                                    .payload(Struct.builder()
                                            .prop("read %s bytes", total)
                                            .build())
                                    .build())
                            .build();
                })
                .build());
    }

    @Override
    public String name() {
        return SutDescriptor.NAME;
    }

    @Override
    public String version() {
        return Api.versionFrom(SutApi.class);
    }

    @Override
    public Processor processor() {
        return this.processor;
    }
}
