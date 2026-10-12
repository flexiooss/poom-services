package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

/**
 * Contrôle d'un {@code InputRequiredResult} avant sa sérialisation. Une erreur ici est une erreur de
 * programmation de l'outil : elle lève, et le répondeur la rend en erreur interne {@code -32603}.
 */
final class InputRequiredGuard {

    private InputRequiredGuard() {}

    static CallToolResult check(CallToolResult result, boolean clientElicitation) {
        if (result == null || result.inputRequired() == null) return result;
        ObjectValue requests = result.inputRequired().inputRequests();
        if (requests == null && result.inputRequired().requestState() == null) {
            throw new IllegalStateException("input_required result carries neither inputRequests nor requestState");
        }
        if (requests != null && !clientElicitation) {
            for (String key : requests.propertyNames()) {
                PropertyValue request = requests.property(key);
                ObjectValue object = request == null || request.isNullValue() ? null : request.single().objectValue();
                String method = object == null ? null : Params.string(object, "method");
                if ("elicitation/create".equals(method)) {
                    throw new IllegalStateException("tool requested an elicitation from a client that does not declare it");
                }
            }
        }
        return result;
    }
}
