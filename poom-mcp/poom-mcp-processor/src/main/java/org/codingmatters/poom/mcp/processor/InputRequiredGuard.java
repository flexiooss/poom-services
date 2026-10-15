package org.codingmatters.poom.mcp.processor;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Map;

/**
 * Contrôle d'un {@code InputRequiredResult} avant sa sérialisation. Une erreur ici est une erreur de
 * programmation de l'outil : elle lève, et le répondeur la rend en erreur interne {@code -32603}.
 * MRTR 2026-07-28 : les valeurs de {@code inputRequests} sont des ElicitRequest, CreateMessageRequest ou
 * ListRootsRequest, et le serveur ne doit pas demander ce que le client n'a pas déclaré.
 */
final class InputRequiredGuard {

    /** Méthode de requête MRTR admise, et capacité client qu'elle exige. */
    private static final Map<String, String> CAPABILITY_BY_METHOD = Map.of(
            "elicitation/create", "elicitation",
            "sampling/createMessage", "sampling",
            "roots/list", "roots");

    private InputRequiredGuard() {}

    static CallToolResult check(CallToolResult result, ObjectValue clientCapabilities) {
        if (result == null || result.inputRequired() == null) return result;
        ObjectValue requests = result.inputRequired().inputRequests();
        boolean noRequests = requests == null || requests.propertyNames().length == 0; // {} vaut absent
        if (noRequests && result.inputRequired().requestState() == null) {
            throw new IllegalStateException("input_required result carries neither inputRequests nor requestState");
        }
        if (noRequests) return result;
        for (String key : requests.propertyNames()) {
            ObjectValue request = Params.object(requests, key);
            if (request == null) {
                throw new IllegalStateException("input request '" + key + "' is not an object");
            }
            String method = Params.string(request, "method");
            String capability = method == null ? null : CAPABILITY_BY_METHOD.get(method);
            if (capability == null) {
                throw new IllegalStateException("input request '" + key + "' has an unsupported method: " + method);
            }
            if (Params.object(clientCapabilities, capability) == null) {
                throw new IllegalStateException("tool requested " + method + " from a client that does not declare " + capability);
            }
        }
        return result;
    }
}
