package org.codingmatters.poom.ci.github.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.api.PoomCIPipelineAPIHandlers;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIHandlersClient;

import java.util.UUID;
import java.util.concurrent.Executors;

public class GithubWebhookServiceTest {

    public static void main(String[] args) {
        if(args.length < 1) throw new RuntimeException("usage: <webhook api secure token>");
        new GithubWebhookService(
                args[0],
                new JsonFactory(),
                new PoomCIPipelineAPIHandlersClient(new PoomCIPipelineAPIHandlers.Builder()
                        .githubTriggersPostHandler(GithubWebhookServiceTest::triggerPosted)
                        .build(), Executors.newFixedThreadPool(4))
        ).start();
    }

    private static GithubTriggersPostResponse triggerPosted(GithubTriggersPostRequest request) {
        String id = UUID.randomUUID().toString();
        return GithubTriggersPostResponse.builder()
                .status201(status -> status.location("http://localhost/triggers/git-hub/" + id).xEntityId(id))
                .build();
    }


}