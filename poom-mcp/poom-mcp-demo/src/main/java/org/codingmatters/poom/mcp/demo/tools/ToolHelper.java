package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.ToolContent;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.ArrayList;
import java.util.List;

class ToolHelper {

    static CallToolResult success(String text) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type(ToolContent.Type.text).text(text).build())
                .isError(false)
                .build();
    }

    static CallToolResult error(String text) {
        return CallToolResult.builder()
                .content(ToolContent.builder().type(ToolContent.Type.text).text(text).build())
                .isError(true)
                .build();
    }

    /** Extracts a single string from an ObjectValue property. Null-safe. */
    static String arg(ObjectValue args, String name) {
        if (args == null || args.property(name) == null) return null;
        PropertyValue pv = args.property(name);
        if (pv.isSingle()) return pv.single().stringValue();
        return null;
    }

    /** Extracts a list of strings from a single or multiple-valued ObjectValue property. Null-safe. */
    static List<String> argList(ObjectValue args, String name) {
        if (args == null || args.property(name) == null) return null;
        PropertyValue pv = args.property(name);
        List<String> result = new ArrayList<>();
        if (pv.isSingle()) {
            String v = pv.single().stringValue();
            if (v != null) result.add(v);
        } else if (pv.isMultiple()) {
            for (PropertyValue.Value v : pv.multiple()) {
                if (v.stringValue() != null) result.add(v.stringValue());
            }
        }
        return result.isEmpty() ? null : result;
    }
}
