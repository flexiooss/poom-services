package org.codingmatters.poom.mcp.demo.prompts;

import org.codingmatters.poom.mcp.types.GetPromptParams;
import org.codingmatters.poom.mcp.types.GetPromptResult;
import org.codingmatters.poom.mcp.types.PromptMessage;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.value.objects.values.ObjectValue;

class PromptHelper {

    static PromptMessage userMessage(String text) {
        return PromptMessage.builder()
                .role(PromptMessage.Role.user)
                .content(ObjectValue.builder()
                        .property(ToolContent.names_().type(), v -> v.stringValue("text"))
                        .property(ToolContent.names_().text(), v -> v.stringValue(text))
                        .build())
                .build();
    }

    static GetPromptResult error(String msg) {
        return GetPromptResult.builder()
                .description("Error")
                .messages(userMessage(msg))
                .build();
    }

    static String stringArg(GetPromptParams params, String name) {
        if (params.arguments() == null || params.arguments().property(name) == null) return null;
        var pv = params.arguments().property(name);
        return pv.isSingle() ? pv.single().stringValue() : null;
    }
}
