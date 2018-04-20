package org.codingmatters.poom.ci.pipeline.api.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.PipelinesPostRequest;
import org.codingmatters.poom.ci.pipeline.api.service.handlers.AbstractPoomCITest;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PoomCIApiTest extends AbstractPoomCITest {

    private final AtomicReference<JobCollectionPostRequest> lastJobPost = new AtomicReference<>(null);

    private PoomjobsJobRegistryAPIClient jobRegistryAPIClient = new PoomjobsJobRegistryAPIHandlersClient(
            new PoomjobsJobRegistryAPIHandlers.Builder()
                    .jobCollectionPostHandler(req -> {
                        lastJobPost.set(req);
                        return JobCollectionPostResponse.builder().status201(status -> status.location("OK")).build();
                    })
                    .build(),
            Executors.newFixedThreadPool(4));
    private PoomCIApi api = new PoomCIApi(this.repository(), "/", new JsonFactory(), this.jobRegistryAPIClient);

    @Test
    public void whenPipelineIsPosted__thenJobIsPosted() {
        String pipelineId = this.api.handlers().pipelinesPostHandler().apply(PipelinesPostRequest.builder()
                .payload(trigger -> trigger.type(PipelineTrigger.Type.GITHUB_PUSH).triggerId("trigger-id"))
                .build())
                .opt().status201().xEntityId()
                .orElseThrow(() -> new AssertionError("pipeline post failed"));

        assertThat(lastJobPost.get().accountId(), is("poom-ci"));
        assertThat(lastJobPost.get().payload().category(), is("poom-ci"));
        assertThat(lastJobPost.get().payload().name(), is("github-pipeline"));
        assertThat(lastJobPost.get().payload().arguments().toArray(), is(arrayContaining(pipelineId)));
    }

    @Test
    public void whenTriggerIsPosted__thenPipelineIsPosted() throws Exception {
        String triggerId = this.api.handlers().githubTriggersPostHandler().apply(GithubTriggersPostRequest.builder()
                .payload(trigger -> trigger.ref("just-triggered"))
                .build()).opt().status201().xEntityId()
                .orElseThrow(() -> new AssertionError("should have a 201"));

        assertThat(
                this.repository().pipelineRepository().all(0, 1).get(0).value().trigger(),
                is(PipelineTrigger.builder().type(PipelineTrigger.Type.GITHUB_PUSH).triggerId(triggerId).build())
        );
    }
}