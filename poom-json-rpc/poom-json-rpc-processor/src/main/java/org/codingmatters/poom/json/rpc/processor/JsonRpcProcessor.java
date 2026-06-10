package org.codingmatters.poom.json.rpc.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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

    private static final byte[] PARSE_ERROR = """
                                {"jsonrpc":"%s","error":{"code": -32700,"message": "Parse error"},"id": null}""".formatted(JSON_RPC_VERSION)
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] INTERNAL_ERROR = """
                                {"jsonrpc":"%s","error":{"code": -32603,"message": "Internal error"},"id": null}""".formatted(JSON_RPC_VERSION)
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] EMPTY_BATCH_ERROR =
            ("{\"jsonrpc\":\"" + JSON_RPC_VERSION + "\",\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"},\"id\":null}")
                    .getBytes(StandardCharsets.UTF_8);

    private final RpcEntryPointDescriptor descriptor;
    private final JsonFactory jsonFactory;
    private final ExecutorService pool;

    public JsonRpcProcessor(RpcEntryPointDescriptor descriptor, JsonFactory jsonFactory, ExecutorService pool) {
        this.descriptor = descriptor;
        this.jsonFactory = jsonFactory;
        this.pool = pool;
    }

    @Override
    public void process(RequestDelegate request, ResponseDelegate response) throws IOException {
        if (request.method() != RequestDelegate.Method.POST) {
            response.status(405);
            response.contenType("text/plain");
            response.payload("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        } else {
            String ct = request.contentType();
            if (ct == null || !ct.startsWith("application/json")) {
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

        byte[] payloadBytes;
        try {
            payloadBytes = request.payload().readAllBytes();
        } catch (IOException e) {
            log.error("error reading rpc request payload", e);
            response.status(200);
            response.payload(PARSE_ERROR);
            return;
        }

        boolean isBatch;
        try (JsonParser peekParser = this.jsonFactory.createParser(payloadBytes)) {
            isBatch = peekParser.nextToken() == JsonToken.START_ARRAY;
        } catch (IOException e) {
            log.error("error reading rpc request", e);
            response.status(200);
            response.payload(PARSE_ERROR);
            return;
        }

        RpcRequest[] requests;
        try (JsonParser parser = this.jsonFactory.createParser(payloadBytes)) {
            requests = new RpcRequestReader().readArray(parser);
        } catch (IOException e) {
            log.error("error reading rpc request", e);
            response.status(200);
            response.payload(PARSE_ERROR);
            return;
        }

        if (isBatch && requests.length == 0) {
            response.status(200);
            response.payload(EMPTY_BATCH_ERROR);
            return;
        }

        boolean synchronousCall = Arrays.stream(requests)
                .anyMatch(rpcRequest -> rpcRequest.opt().id().isPresent());
        response.status(synchronousCall ? 200 : 204);

        Future<List<RpcResponse>> future;
        try {
            future = this.executeCalls(requests);
        } catch (BusyException e) {
            response.status(200);
            response.payload(INTERNAL_ERROR);
            return;
        }

        if (synchronousCall) {
            List<RpcResponse> rpcResponses;
            try {
                rpcResponses = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.payload(INTERNAL_ERROR);
                return;
            } catch (ExecutionException e) {
                response.payload(INTERNAL_ERROR);
                return;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
                RpcResponseWriter writer = new RpcResponseWriter();
                if (isBatch) {
                    writer.writeArray(generator, rpcResponses.toArray(new RpcResponse[0]));
                } else {
                    writer.write(generator, rpcResponses.get(0));
                }
            } catch (IOException e) {
                response.payload(INTERNAL_ERROR);
                return;
            }
            response.payload(out.toByteArray());
        }
    }

    private Future<List<RpcResponse>> executeCalls(RpcRequest[] requests) throws BusyException {
        FutureTask<List<RpcResponse>> result = new FutureTask<>(() -> {
            List<RpcResponse> rpcResponses = new ArrayList<>(requests.length);
            for (RpcRequest rpcRequest : requests) {
                RpcResponse resp = processRpcRequest(rpcRequest);
                if (resp != null) {
                    rpcResponses.add(resp);
                }
            }
            return rpcResponses;
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
        boolean isNotification = rpcRequest.opt().id().isEmpty();

        if (rpcRequest.opt().jsonrpc().isEmpty() || rpcRequest.opt().method().isEmpty()
                || !JSON_RPC_VERSION.equals(rpcRequest.jsonrpc())) {
            return isNotification ? null : errorResponse(rpcRequest.id(), -32600, "Invalid Request");
        }

        Optional<RpcMethodDescriptor> methodDescriptor = this.descriptor.opt().methods().safe().stream()
                .filter(d -> d.method().equals(rpcRequest.method()))
                .findFirst();
        if (methodDescriptor.isEmpty()) {
            return isNotification ? null : errorResponse(rpcRequest.id(), -32601, "Method not found");
        }

        Object param;
        try {
            param = this.processParam(methodDescriptor.get(), rpcRequest.params());
        } catch (ParamsProcessingException e) {
            log.error("error processing params {} with descriptor {}", rpcRequest.params(), methodDescriptor.get(), e);
            return isNotification ? null : errorResponse(rpcRequest.id(), -32603, "Internal Error");
        }

        Object result;
        try {
            result = methodDescriptor.get().handler().apply(param);
        } catch (RuntimeException e) {
            log.error("handler threw for method {}", rpcRequest.method(), e);
            return isNotification ? null : errorResponse(rpcRequest.id(), -32603, "Internal Error");
        }

        if (isNotification) {
            return null;
        }

        try {
            return RpcResponse.builder()
                    .jsonrpc(JSON_RPC_VERSION)
                    .result(this.processResult(methodDescriptor.get(), result))
                    .id(rpcRequest.id())
                    .build();
        } catch (ResultProcessingException e) {
            log.error("error processing result {} with descriptor {}", result, methodDescriptor.get(), e);
            return errorResponse(rpcRequest.id(), -32603, "Internal Error");
        }
    }

    private RpcResponse errorResponse(String id, int code, String message) {
        return RpcResponse.builder()
                .jsonrpc(JSON_RPC_VERSION)
                .id(id)
                .error(RpcError.builder().code(code).message(message).build())
                .build();
    }

    private Object processParam(RpcMethodDescriptor rpcMethodDescriptor, ObjectValue params) throws ParamsProcessingException {
        try {
            Class paramsValue = rpcMethodDescriptor.paramsValue();
            Method fromMap = paramsValue.getMethod("fromMap", Map.class);
            fromMap.setAccessible(true);
            Object builder = fromMap.invoke(paramsValue, params != null ? params.toMap() : Map.of());
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
        public ParamsProcessingException(String message) { super(message); }
        public ParamsProcessingException(String message, Throwable cause) { super(message, cause); }
    }

    private class ResultProcessingException extends Exception {
        public ResultProcessingException(String message) { super(message); }
        public ResultProcessingException(String message, Throwable cause) { super(message, cause); }
    }

    private class BusyException extends Exception {
        public BusyException(String message) { super(message); }
        public BusyException(String message, Throwable cause) { super(message, cause); }
    }
}
