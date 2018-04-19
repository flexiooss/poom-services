package org.codingmatters.poom.ci.pipeline.api.service.handlers;

import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostRequest;
import org.codingmatters.poom.ci.pipeline.api.GithubTriggersPostResponse;
import org.codingmatters.poom.ci.pipeline.api.service.repository.PoomCIRepository;
import org.codingmatters.poom.ci.pipeline.api.types.Error;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.triggers.GithubPushEvent;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.rest.api.Processor;

import java.util.function.Consumer;
import java.util.function.Function;

public class GithubTriggerCreation implements Function<GithubTriggersPostRequest, GithubTriggersPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GithubTriggerCreation.class);

    private final Repository<GithubPushEvent, String> githubPushEventRepository;
    private final Consumer<PipelineTrigger> triggerCreatedListener;

    public GithubTriggerCreation(PoomCIRepository repository, Consumer<PipelineTrigger> triggerCreatedListener) {
        this.githubPushEventRepository = repository.githubPushEventRepository();
        this.triggerCreatedListener = triggerCreatedListener;
    }

    @Override
    public GithubTriggersPostResponse apply(GithubTriggersPostRequest request) {
        try {
            Entity<GithubPushEvent> trigger = this.githubPushEventRepository.create(request.payload());
            log.audit().info("trigger created for github push event {}", trigger);

            this.triggerCreatedListener.accept(PipelineTrigger.builder()
                    .type(PipelineTrigger.Type.GITHUB_PUSH)
                    .triggerId(trigger.id())
                    .build());
            return GithubTriggersPostResponse.builder()
                    .status201(status -> status
                        .xEntityId(trigger.id())
                        .location(Processor.Variables.API_PATH.token() + "/triggers/git-hub/" + trigger.id())
                    )
                    .build();

        } catch (RepositoryException e) {
            return GithubTriggersPostResponse.builder().status500(status -> status.payload(error -> error
                            .token(log.tokenized().error("error while storing push event to repository", e))
                            .code(Error.Code.UNEXPECTED_ERROR)
                    ))
                    .build();
        }
    }
}
