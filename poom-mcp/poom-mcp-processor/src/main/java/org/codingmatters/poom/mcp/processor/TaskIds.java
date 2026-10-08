package org.codingmatters.poom.mcp.processor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * {@code taskId} public = base64url(nom de l'outil) + "." + millisecondes de création + "." + identifiant
 * rendu par l'outil. Le serveur étant sans état, c'est ce qui permet à n'importe quel réplica de
 * retrouver l'outil qui sert la tâche et sa date de création. L'identifiant de l'outil est opaque ici :
 * c'est à l'outil d'en vérifier l'appartenance au contexte de la requête.
 */
final class TaskIds {

    record Ref(String toolName, Instant createdAt, String toolTaskId) {}

    private TaskIds() {}

    static String encode(String toolName, Instant createdAt, String toolTaskId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toolName.getBytes(StandardCharsets.UTF_8))
                + "." + createdAt.toEpochMilli() + "." + toolTaskId;
    }

    static Optional<Ref> decode(String taskId) {
        if (taskId == null) return Optional.empty();
        int first = taskId.indexOf('.');
        if (first <= 0) return Optional.empty();
        int second = taskId.indexOf('.', first + 1);
        if (second <= first + 1 || second == taskId.length() - 1) return Optional.empty();
        try {
            String toolName = new String(Base64.getUrlDecoder().decode(taskId.substring(0, first)), StandardCharsets.UTF_8);
            Instant createdAt = Instant.ofEpochMilli(Long.parseLong(taskId.substring(first + 1, second)));
            return Optional.of(new Ref(toolName, createdAt, taskId.substring(second + 1)));
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            return Optional.empty();
        }
    }
}
