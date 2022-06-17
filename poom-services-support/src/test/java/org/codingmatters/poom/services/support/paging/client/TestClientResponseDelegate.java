package org.codingmatters.poom.services.support.paging.client;

import org.codingmatters.rest.api.client.ResponseDelegate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TestClientResponseDelegate implements ResponseDelegate {

    private int code = 200;
    private HashMap<String, List<String>> headers = new HashMap<>();
    private String contentType;
    private byte[] body;

    public TestClientResponseDelegate() {
    }

    public TestClientResponseDelegate withCode(int code) {
        this.code = code;
        return this;
    }

    public TestClientResponseDelegate withHeader(String name, String ... values) {
        this.headers.put(name, values == null ? new LinkedList<>() : Arrays.stream(values).collect(Collectors.toList()));
        return this;
    }

    public TestClientResponseDelegate withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public TestClientResponseDelegate withBody(byte[] body) {
        this.body = body;
        return this;
    }

    @Override
    public int code() {
        return this.code;
    }

    @Override
    public byte[] body() throws IOException {
        return this.body;
    }

    @Override
    public InputStream bodyStream() throws IOException {
        return new ByteArrayInputStream(this.body);
    }

    @Override
    public String[] header(String s) {
        return this.headers.containsKey(s) ? this.headers.get(s).toArray(new String[0]) : new String[0];
    }

    @Override
    public String[] headerNames() {
        return this.headers.keySet().toArray(new String[0]);
    }

    @Override
    public String[] rawHeaderNames() {
        return headerNames();
    }

    @Override
    public String contentType() {
        return this.contentType;
    }
}
