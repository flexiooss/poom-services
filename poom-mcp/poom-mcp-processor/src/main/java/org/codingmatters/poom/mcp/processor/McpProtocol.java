package org.codingmatters.poom.mcp.processor;

/** Constantes du protocole MCP 2026-07-28, en un seul endroit : la spec est jeune, ses noms peuvent bouger. */
public final class McpProtocol {
    public static final String VERSION = "2026-07-28";

    public static final String HEADER_PROTOCOL_VERSION = "MCP-Protocol-Version";
    public static final String HEADER_METHOD = "Mcp-Method";
    public static final String HEADER_NAME = "Mcp-Name";

    public static final String META = "_meta";
    public static final String META_PROTOCOL_VERSION = "io.modelcontextprotocol/protocolVersion";
    public static final String META_CLIENT_INFO = "io.modelcontextprotocol/clientInfo";
    public static final String META_CLIENT_CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";
    public static final String META_SERVER_INFO = "io.modelcontextprotocol/serverInfo";

    public static final String TASKS_EXTENSION = "io.modelcontextprotocol/tasks";

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int HEADER_MISMATCH = -32020;
    public static final int MISSING_REQUIRED_CLIENT_CAPABILITY = -32021;
    public static final int UNSUPPORTED_PROTOCOL_VERSION = -32022;

    private McpProtocol() {}
}
