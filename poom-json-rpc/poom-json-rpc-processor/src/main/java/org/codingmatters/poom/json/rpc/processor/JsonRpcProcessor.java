package org.codingmatters.poom.json.rpc.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.json.rpc.descriptors.RpcEntryPointDescriptor;
import org.codingmatters.poom.json.rpc.descriptors.RpcMethodDescriptor;
import org.codingmatters.poom.json.rpc.types.RpcError;
import org.codingmatters.poom.json.rpc.types.RpcRequest;
import org.codingmatters.poom.json.rpc.types.RpcResponse;
import org.codingmatters.poom.json.rpc.types.json.RpcRequestReader;
import org.codingmatters.poom.json.rpc.types.json.RpcResponseWriter;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.rest.api.RequestDelegate;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.value.objects.values.ObjectValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class JsonRpcProcessor implements Processor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JsonRpcProcessor.class);

    public static final String JSON_RPC_VERSION = "2.0";

    static public final RpcResponse INVALID_REQUEST_RESPONSE = RpcResponse.builder()
            .jsonrpc(JSON_RPC_VERSION)
            .error(RpcError.builder()
                    .code(-32600)
                    .message("Invalid Request")
                    .build())
            .build();
    static public final RpcResponse METHOD_NOT_FOUND_RESPONSE = RpcResponse.builder()
            .jsonrpc(JSON_RPC_VERSION)
            .error(RpcError.builder()
                    .code(-32601)
                    .message("Method not found")
                    .build())
            .build();

    private static final byte[] PARSE_ERROR = """
                                {"jsonrpc":"%s","error":{"code": -32700,"message": "Parse error"},"id": null}""".formatted(JSON_RPC_VERSION)
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] INTERNAL_ERROR = """
                                {"jsonrpc":"%s","error":{"code": -32603,"message": "Internal error"},"id": null}""".formatted(JSON_RPC_VERSION)
            .getBytes(StandardCharsets.UTF_8);



    private final RpcEntryPointDescriptor descriptor;
    private final JsonFactory  jsonFactory;
    private final ExecutorService pool;

    public JsonRpcProcessor(RpcEntryPointDescriptor descriptor, JsonFactory jsonFactory, ExecutorService pool) {
        this.descriptor = descriptor;
        this.jsonFactory = jsonFactory;
        this.pool = pool;
    }

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        if(request.method() != RequestDelegate.Method.POST) {
            response.status(405);
            response.contenType("text/plain");
            response.payload("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        } else {
            if(! request.contentType().equals("application/json")) {
                response.status(415);
                response.contenType("text/plain");
                response.payload("Unsupported Media Type".getBytes(StandardCharsets.UTF_8));
            } else {
                this.processPayload(request, response);
            }
        }
    }

    private void processPayload(RequestDelegate request, ResponseDelegate response) {
        response.contenType("application/json");

        RpcRequest[] requests;
        try (JsonParser parser = this.jsonFactory.createParser(request.payload())) {
            requests = new RpcRequestReader().readArray(parser);;
        } catch (IOException e) {
            log.error("error reading rpc request", e);
            response.status(200);
            response.payload(PARSE_ERROR);
            return;
        }

        boolean synchronousCall = Arrays.stream(requests).filter(rpcRequest -> rpcRequest.opt().id().isPresent()).findAny().isPresent();
        if(synchronousCall) {
            response.status(200);
        } else {
            response.status(204);
        }

        Future<List<RpcResponse>> futures = null;
        try {
            futures = this.executeCalls(requests);
        } catch (BusyException e) {
            response.status(200);
            response.payload(INTERNAL_ERROR);
            return;
        }

        if(synchronousCall) {
            List<RpcResponse> rpcResponses;
            try {
                rpcResponses = futures.get();
            } catch (InterruptedException | ExecutionException e) {
                response.payload(INTERNAL_ERROR);
                return;
            }
            RpcResponseWriter writer = new RpcResponseWriter();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                try (JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
                    if (rpcResponses.size() > 1) {
                        writer.writeArray(generator, rpcResponses.toArray(new RpcResponse[0]));
                    } else {
                        writer.write(generator, rpcResponses.get(0));
                    }
                    generator.flush();
                    generator.close();
                } catch (IOException e) {
                    response.payload(INTERNAL_ERROR);
                    return;
                }
                response.payload(out.toByteArray());
            } catch (IOException e) {
                response.payload(INTERNAL_ERROR);
                return;
            }
        }
    }

    private Future<List<RpcResponse>> executeCalls(RpcRequest[] requests) throws BusyException {
        FutureTask<List<RpcResponse>> result = new FutureTask<List<RpcResponse>>(new Callable<List<RpcResponse>>() {
            @Override
            public List<RpcResponse> call() throws Exception {
                List<RpcResponse> rpcResponses = new ArrayList<>(requests.length);
                for (RpcRequest rpcRequest : requests) {
                    rpcResponses.add(processRpcRequest(rpcRequest));
                }

                return rpcResponses;
            }
        });
        try {
            this.pool.submit(result);
        } catch (RejectedExecutionException e) {
            log.error("error submitting RPC requests, call was rejected by pool", e);
            throw new BusyException("resource exhausted", e);
        }

        return result;
    }

    private RpcResponse processRpcRequest(RpcRequest rpcRequest) {
        if(rpcRequest.opt().jsonrpc().isEmpty()) return INVALID_REQUEST_RESPONSE;
        if(rpcRequest.opt().method().isEmpty()) return INVALID_REQUEST_RESPONSE;
        if(! JSON_RPC_VERSION.equals(rpcRequest.jsonrpc())) return INVALID_REQUEST_RESPONSE;


        Optional<RpcMethodDescriptor> methodDescriptor = this.descriptor.opt().methods().safe().stream()
                .filter(descriptor -> descriptor.method().equals(rpcRequest.method()))
                .findFirst();
        if(methodDescriptor.isPresent()) {
            Object param = null;
            try {
                param = this.processParam(methodDescriptor.get(), rpcRequest.params());
            } catch (ParamsProcessingException e) {
                log.error("error processing method params " + param + " with descriptor " + methodDescriptor.get(), e);
                return RpcResponse.builder().jsonrpc(JSON_RPC_VERSION).error(RpcError.builder()
                                .code(-32603)
                                .message("Internal Error")
                        .build()).build();
            }

            Object result = methodDescriptor.get().handler().apply(param);

            ObjectValue resultValue;
            try {
                resultValue = this.processResult(methodDescriptor.get(), result);
            } catch (ResultProcessingException e) {
                log.error("error processing method result " + result + " with descriptor " + methodDescriptor.get(), e);
                return RpcResponse.builder().jsonrpc(JSON_RPC_VERSION).error(RpcError.builder()
                        .code(-32603)
                        .message("Internal Error")
                        .build()).build();
            }
            return RpcResponse.builder()
                    .jsonrpc(JSON_RPC_VERSION)
                    .result(resultValue)
                    .id(rpcRequest.id())
                    .build();
        } else {
            return METHOD_NOT_FOUND_RESPONSE;
        }
    }

    private Object processParam(RpcMethodDescriptor rpcMethodDescriptor, ObjectValue params) throws ParamsProcessingException {
        try {
            Class paramsValue = rpcMethodDescriptor.paramsValue();

            Method fromMap = paramsValue.getMethod("fromMap", Map.class);
            fromMap.setAccessible(true);

            Object builder = fromMap.invoke(paramsValue, params.toMap());


            Method build = builder.getClass().getMethod("build");
            build.setAccessible(true);

            return build.invoke(builder);
        } catch (Throwable e) {
            throw new ParamsProcessingException("error processing params : " + params, e);
        }
    }

    private ObjectValue processResult(RpcMethodDescriptor rpcMethodDescriptor, Object result) throws ResultProcessingException {
        try {
            Class resultValue = rpcMethodDescriptor.resultValue();
            Method toMap = resultValue.getMethod("toMap");
            toMap.setAccessible(true);
            return ObjectValue.fromMap((Map) toMap.invoke(result)).build();
        } catch (Throwable e) {
            throw new ResultProcessingException("error processing results : " + result, e);
        }
    }

    private class ParamsProcessingException extends Exception {
        public ParamsProcessingException(String message) {
            super(message);
        }

        public ParamsProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private class ResultProcessingException extends Exception {
        public ResultProcessingException(String message) {
            super(message);
        }

        public ResultProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private class BusyException extends Exception {
        public BusyException(String message) {
            super(message);
        }

        public BusyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
