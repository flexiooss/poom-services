package org.codingmatters.poom.mcp.processor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.poom.mcp.types.json.McpIdNormalizingParser;
import org.codingmatters.poom.mcp.types.json.McpRequestReader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpIdNormalizingParserTest {
    private final JsonFactory jsonFactory = new JsonFactory();

    @Test
    void numericIntIdIsNormalizedToString() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"initialize\",\"params\":{}}";
        assertThat(parse(json).id(), is("42"));
    }

    @Test
    void stringIdPassesThrough() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"initialize\",\"params\":{}}";
        assertThat(parse(json).id(), is("abc"));
    }

    @Test
    void numericIdInsideNestedParamsIsUntouched() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/call\","
                + "\"params\":{\"arguments\":{\"id\":999}}}";
        McpRequest req = parse(json);
        assertThat(req.id(), is("1"));
        assertThat(
            req.params()
               .property("arguments").single().objectValue()
               .property("id").single().longValue(),
            is(999L)
        );
    }

    private McpRequest parse(String json) throws Exception {
        try (JsonParser parser = new McpIdNormalizingParser(jsonFactory.createParser(json))) {
            return new McpRequestReader().read(parser);
        }
    }
}
