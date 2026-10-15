package org.codingmatters.poom.mcp.processor;

import java.time.Duration;

/**
 * Seuils du transport.
 * <ul>
 *   <li>{@code jsonWindow} : en dessous, la réponse est {@code application/json} ; au-delà, flux SSE.</li>
 *   <li>{@code keepalive} : intervalle des commentaires SSE, sous les 40 s d'inactivité des clients et proxies.</li>
 *   <li>{@code taskAfter} : au-delà, un outil à tâches répond par un {@code CreateTaskResult} si le client déclare l'extension.</li>
 *   <li>{@code streamMax} : durée maximale d'un flux ; au-delà, erreur {@code no_result}.</li>
 *   <li>{@code toolPollInterval} : rythme de lecture d'un outil à tâches tant que le flux est ouvert.</li>
 *   <li>{@code taskTtl}, {@code clientPollInterval} : valeurs annoncées au client dans {@code CreateTaskResult}.</li>
 * </ul>
 */
public record McpTimings(Duration jsonWindow, Duration keepalive, Duration taskAfter, Duration streamMax,
                         Duration toolPollInterval, Duration taskTtl, Duration clientPollInterval) {

    public static McpTimings defaults() {
        return new McpTimings(Duration.ofSeconds(1), Duration.ofSeconds(15), Duration.ofSeconds(20),
                Duration.ofMinutes(5), Duration.ofSeconds(1), Duration.ofHours(1), Duration.ofSeconds(2));
    }

    public McpTimings withStreamMax(Duration streamMax) {
        return new McpTimings(this.jsonWindow, this.keepalive, this.taskAfter, streamMax,
                this.toolPollInterval, this.taskTtl, this.clientPollInterval);
    }
}
