package org.codingmatters.poom.services.runtime.handlers.delegate;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.runtime.handlers.RequestLoggingProcessor;
import org.codingmatters.rest.api.ResponseDelegate;
import org.codingmatters.rest.io.Content;

import java.io.*;
import java.util.*;

public class LoggingResponseDelegate implements ResponseDelegate {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(LoggingResponseDelegate.class);

    private final ResponseDelegate delegate;
    private final Map<String, Object> data = new LinkedHashMap<>();
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private Content content = null;
    private String contentType = null;

    public LoggingResponseDelegate(ResponseDelegate delegate) {
        this.delegate = delegate;
        this.data.put("headers", this.headers);
    }

    public Map<String, Object> data() {
        if(this.content != null) {
            if(this.isPayloadLogged()) {
                try (InputStream in = this.content.asStream(); Reader reader = new InputStreamReader(in)) {
                    StringBuilder payload = new StringBuilder();
                    char[] buffer = new char[1024];
                    for (int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                        payload.append(buffer, 0, read);
                    }
                    this.data.put("payload", payload.toString());
                } catch (IOException e) {
                    log.warn("error reading payload", e);
                    this.data.put("payload", "error reading payload");
                }
            } else {
                this.data.put("payload", "payload not logged");
            }
        } else {
            this.data.put("payload", null);
        }
        return this.data;
    }

    private boolean isPayloadLogged() {
        return this.contentType != null && RequestLoggingProcessor.isPayloadLogged(this.contentType);
    }

    @Override
    public ResponseDelegate contenType(String contenType) {
        this.contentType = contenType;
        this.data.put("content-type", contenType);
        delegate.contenType(contenType);
        return this;
    }

    @Override
    public ResponseDelegate status(int code) {
        this.data.put("status", code);
        delegate.status(code);
        return this;
    }

    @Override
    public ResponseDelegate addHeader(String name, String... value) {
        this.headers.put(name, value != null ? Arrays.asList(value) : Collections.emptyList());
        if(name.equalsIgnoreCase("content-type") && value != null && value.length > 0) {
            this.contentType = value[0];
        }
        delegate.addHeader(name, value);
        return this;
    }

    @Override
    public ResponseDelegate addHeaderIfNot(String name, String... value) {
        if(! this.headers.containsKey(name)) {
            this.headers.put(name, value != null ? Arrays.asList(value) : Collections.emptyList());
            if(name.equalsIgnoreCase("content-type") && value != null && value.length > 0) {
                this.contentType = value[0];
            }
        }
        delegate.addHeaderIfNot(name, value);
        return this;
    }

    @Override
    public ResponseDelegate clearHeader(String name) {
        this.delegate.clearHeader(name);
        return this;
    }

    @Override
    public ResponseDelegate payload(String payload, String charset) {
        try {
            this.content = Content.from(payload.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            log.warn("error building payload log", e);
        }
        delegate.payload(payload, charset);
        return this;
    }

    @Override
    public ResponseDelegate payload(byte[] bytes) {
        this.content = Content.from(bytes);
        delegate.payload(bytes);
        return this;
    }

    @Override
    public ResponseDelegate payload(InputStream in) {
        try {
            this.content = Content.from(in);
            in = this.content.asStream();
        } catch (IOException e) {
            log.warn("error building stream payload log", e);
        }
        delegate.payload(in);
        return this;
    }

    @Override
    public void close() throws Exception {
        this.delegate.close();
    }

}
