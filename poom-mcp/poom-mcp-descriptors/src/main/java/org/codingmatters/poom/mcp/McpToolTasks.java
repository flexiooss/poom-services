package org.codingmatters.poom.mcp;

import org.codingmatters.poom.mcp.types.CallToolParams;
import org.codingmatters.poom.mcp.types.CallToolResult;

/**
 * Un outil dont le travail survit à la requête qui l'a lancé : il en fournit le stockage durable.
 * Le serveur MCP est sans état, donc {@link #get} et {@link #cancel} peuvent être appelés depuis
 * un autre réplica que celui qui a appelé {@link #start} : l'identifiant rendu doit suffire à
 * retrouver le travail, et l'implémentation vérifie qu'il appartient bien au contexte de la
 * requête courante.
 *
 * <p>Quand un descripteur porte {@code tasks}, le processeur l'utilise et ignore {@code handler}.
 */
public interface McpToolTasks {

    /** Démarre le travail. Un refus immédiat (argument manquant, accès refusé) se rend en {@link Start.Done}. */
    Start start(CallToolParams params);

    /** Lit l'état du travail. Lève {@link McpTaskNotFoundException} si l'identifiant est inconnu ou étranger au contexte. */
    McpTaskState get(String toolTaskId);

    /** Demande l'arrêt, sans l'attendre. Ne lève pas si l'arrêt n'est pas possible. */
    void cancel(String toolTaskId);

    sealed interface Start permits Start.Running, Start.Done {
        record Running(String toolTaskId) implements Start {}
        record Done(CallToolResult result) implements Start {}
    }
}
