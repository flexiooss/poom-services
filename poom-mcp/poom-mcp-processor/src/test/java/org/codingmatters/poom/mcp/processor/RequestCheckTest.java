package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.types.McpRequest;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RequestCheckTest {

    private static McpRequest withMeta(ObjectValue meta) {
        ObjectValue.Builder params = ObjectValue.builder();
        if (meta != null) params.property(McpProtocol.META, v -> v.objectValue(meta));
        return McpRequest.builder().method("tools/call").params(params.build()).build();
    }

    private static McpRequest withCapabilities(ObjectValue capabilities) {
        return withMeta(ObjectValue.builder()
                .property(McpProtocol.META_CLIENT_CAPABILITIES, v -> v.objectValue(capabilities)).build());
    }

    private static McpRequest withElicitation(java.util.function.Consumer<org.codingmatters.value.objects.values.PropertyValue.Builder> value) {
        return withCapabilities(ObjectValue.builder().property("elicitation", value).build());
    }

    @Test
    void givenNoCapabilities__whenDeclaresElicitation__thenFalse() {
        assertThat(RequestCheck.clientDeclaresElicitation(withCapabilities(ObjectValue.builder().build())), is(false));
    }

    @Test
    void givenEmptyElicitation__whenDeclaresElicitation__thenTrue() {
        assertThat(RequestCheck.clientDeclaresElicitation(withElicitation(v -> v.objectValue(ObjectValue.builder().build()))), is(true));
    }

    @Test
    void givenFormElicitation__whenDeclaresElicitation__thenTrue() {
        assertThat(RequestCheck.clientDeclaresElicitation(withElicitation(v -> v.objectValue(ObjectValue.builder()
                .property("form", f -> f.objectValue(ObjectValue.builder().build())).build()))), is(true));
    }

    @Test
    void givenNullElicitation__whenDeclaresElicitation__thenFalse() {
        assertThat(RequestCheck.clientDeclaresElicitation(withElicitation(v -> v.stringValue((String) null))), is(false));
    }

    @Test
    void givenNonObjectElicitation__whenDeclaresElicitation__thenFalse() {
        assertThat(RequestCheck.clientDeclaresElicitation(withElicitation(v -> v.stringValue("yes"))), is(false));
    }

    @Test
    void givenCapabilitiesKeyAbsentFromMeta__whenClientCapabilities__thenEmptyNotNull() {
        ObjectValue capabilities = RequestCheck.clientCapabilities(withMeta(ObjectValue.builder().build()));

        assertThat(capabilities, is(notNullValue()));
        assertThat(capabilities.propertyNames().length, is(0));
    }
}
