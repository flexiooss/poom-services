package org.codingmatters.poom.mcp.processor;

import org.codingmatters.rest.api.SseChannel;

public class McpSession {

    private final String id;
    private volatile SseChannel sseChannel;

    public McpSession(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public void setSseChannel(SseChannel channel) {
        this.sseChannel = channel;
    }

    public SseChannel sseChannel() {
        return sseChannel;
    }

    public boolean hasSseChannel() {
        return sseChannel != null && sseChannel.isOpen();
    }
}
