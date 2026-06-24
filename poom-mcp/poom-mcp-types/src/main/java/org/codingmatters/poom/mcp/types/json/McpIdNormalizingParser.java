package org.codingmatters.poom.mcp.types.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.IOException;

public class McpIdNormalizingParser extends JsonParserDelegate {
    private int depth = 0;
    private boolean awaitingIdValue = false;
    private String idStringValue = null;

    public McpIdNormalizingParser(JsonParser delegate) {
        super(delegate);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = super.nextToken();
        idStringValue = null;

        if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
            depth++;
        } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
            depth--;
        }

        if (depth == 1 && token == JsonToken.FIELD_NAME && "id".equals(getCurrentName())) {
            awaitingIdValue = true;
        } else if (awaitingIdValue) {
            awaitingIdValue = false;
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                idStringValue = super.getText();
                return JsonToken.VALUE_STRING;
            }
        }
        return token;
    }

    @Override
    public JsonToken currentToken() {
        return idStringValue != null ? JsonToken.VALUE_STRING : super.currentToken();
    }

    @Override
    public String getText() throws IOException {
        return idStringValue != null ? idStringValue : super.getText();
    }
}
