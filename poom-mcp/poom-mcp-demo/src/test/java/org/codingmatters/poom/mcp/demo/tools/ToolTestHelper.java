package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.List;

class ToolTestHelper {

    @SuppressWarnings("unchecked")
    static CallToolParams params(Object... kvPairs) {
        ObjectValue.Builder b = ObjectValue.builder();
        for (int i = 0; i < kvPairs.length; i += 2) {
            String key = (String) kvPairs[i];
            Object val = kvPairs[i + 1];
            if (val instanceof String s) {
                b.property(key, pv -> pv.stringValue(s));
            } else if (val instanceof List<?> list) {
                String[] arr = ((List<String>) list).toArray(new String[0]);
                b.property(key, PropertyValue.multipleString(arr));
            }
        }
        return CallToolParams.builder().name("tool").arguments(b.build()).build();
    }

    static CallToolParams noArgs() {
        return CallToolParams.builder().name("tool").build();
    }
}
