package org.codingmatters.poom.ci.runners.pipeline;

import org.codingmatters.poom.ci.pipeline.api.PipelineGetResponse;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTermination;
import org.codingmatters.poom.ci.pipeline.api.types.PipelineTrigger;
import org.codingmatters.poom.ci.pipeline.api.types.StageTermination;
import org.codingmatters.poom.ci.pipeline.client.PoomCIPipelineAPIClient;
import org.codingmatters.poom.ci.pipeline.descriptors.StageHolder;
import org.codingmatters.poom.ci.runners.pipeline.loggers.DirectStageLogger;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PipelineJobProcessor implements JobProcessor {
    static private CategorizedLogger log = CategorizedLogger.getLogger(PipelineJobProcessor.class);

    private final Job job;
    private final PipelineContext.PipelineContextProvider pipelineContextProvider;
    private final PipelineExecutor.PipelineExecutorProvider pipelineExecutorProvider;
    private final PoomCIPipelineAPIClient pipelineAPIClient;

    public PipelineJobProcessor(Job job, PipelineContext.PipelineContextProvider pipelineContextProvider, PipelineExecutor.PipelineExecutorProvider pipelineExecutorProvider, PoomCIPipelineAPIClient pipelineAPIClient) {
        this.job = job;
        this.pipelineContextProvider = pipelineContextProvider;
        this.pipelineExecutorProvider = pipelineExecutorProvider;
        this.pipelineAPIClient = pipelineAPIClient;
    }

    @Override
    public Job process() throws JobProcessingException {
        PipelineContext context = this.initializeContext();
        PipelineExecutor executor = this.pipelineExecutorProvider.forContext(context);

        PipelineTermination.Exit status;
        try {
            this.initializeExecution(context, executor);
            status = this.executeStages(context, context.stages(), executor);
            if(status.equals(PipelineTermination.Exit.SUCCESS)) {
                status = this.executeStages(context, context.onSuccessStages(), executor);
            } else {
                status = this.executeStages(context, context.onErrorStages(), executor);
            }
        } catch (JobProcessingException e) {
            log.audit().info("failed executing pipeline {}", context.pipelineId());
            this.notifyPipelineTerminationStatus(context, PipelineTermination.Exit.FAILURE);
            throw e;
        }

        log.audit().info("successfully executed pipeline {} with exit status {}", context.pipelineId(), status);
        this.notifyPipelineTerminationStatus(context, status);

        return this.job
                .withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .withProcessing(this.job.processing().withFinished(LocalDateTime.now(ZoneOffset.UTC.normalized())));
    }

    private PipelineContext initializeContext() throws JobProcessingException {
        String pipelineId = this.job.arguments().get(0);
        log.audit().info("starting pipeline {} execution", pipelineId);

        PipelineTrigger trigger = this.retrievePipelineTrigger(pipelineId);
        return this.createContext(pipelineId, trigger);
    }

    private PipelineTrigger retrievePipelineTrigger(String pipelineId) throws JobProcessingException {
        log.audit().info("retrieving pipeline {} trigger", pipelineId);
        try {
            PipelineGetResponse response = this.pipelineAPIClient.pipelines().pipeline().get(req -> req.pipelineId(pipelineId));
            return response.opt().status200()
                    .payload().trigger()
                    .orElseThrow(() -> {
                        String errorToken = log.tokenized().error("while retrieving trigger for pipeline {}, got response : {}",
                                pipelineId, response);
                        return new JobProcessingException("couldn't retrieve pipeline trigger, see logs with " + errorToken);
                    });
        } catch (IOException e) {
            throw new JobProcessingException("failed accessing pipeline API", e);
        }
    }

    private PipelineContext createContext(String pipelineId, PipelineTrigger trigger) throws JobProcessingException {
        try {
            return this.pipelineContextProvider.pipelineContext(pipelineId, trigger);
        } catch (IOException e) {
            String errorToken = log.personalData().tokenized().error("couldn't initialize pipeline context", e);
            throw new JobProcessingException("error initializing pipeline context, see logs with error-token=" + errorToken);
        }
    }

    private void initializeExecution(PipelineContext context, PipelineExecutor executor) throws JobProcessingException {
        try {
            executor.initialize();
            log.audit().info("pipeline {} executor initialized", context.pipelineId());
        } catch (IOException e) {
            String errorToken = log.personalData().tokenized().error(String.format(
                    "couldn't initialize pipeline %s execution",
                    context.pipelineId()),
                    e);
            throw new JobProcessingException("error initializing pipeline execution, see logs with error-token=" + errorToken);
        }
    }

    private PipelineTermination.Exit executeStages(PipelineContext context, StageHolder[] stages, PipelineExecutor executor) throws JobProcessingException {
        if(stages != null) {
            for (StageHolder stage : stages) {
                StageTermination.Exit status = this.executeStage(context, executor, stage);
                if (status.equals(StageTermination.Exit.FAILURE)) {
                    return PipelineTermination.Exit.FAILURE;
                }
            }
        }
        return PipelineTermination.Exit.SUCCESS;
    }

    private StageTermination.Exit executeStage(PipelineContext context, PipelineExecutor executor, StageHolder stage) throws JobProcessingException {
        log.audit().info("executing pipeline {} stage {}", context.pipelineId(), stage);
        try {
            this.notifyStageExecutionStart(context, stage);
            StageTermination.Exit status = executor.execute(stage.stage().name(), this.stageLogListener(context, stage));
            this.notifyStageExecutionEnd(context, stage, status);
            return status;
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error executing pipeline %s stage %s",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private PipelineExecutor.StageLogListener stageLogListener(PipelineContext context, StageHolder stage) {
        return new DirectStageLogger(context.pipelineId(), stage, this.pipelineAPIClient);
    }

    private void notifyStageExecutionStart(PipelineContext context, StageHolder stage) throws JobProcessingException {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().post(req -> req
                    .pipelineId(context.pipelineId())
                    .stageType(stage.type().name())
                    .payload(creation -> creation
                            .name(stage.stage().name())
                    )
            );
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error notifying pipeline %s stage %s execution start",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private void notifyStageExecutionEnd(PipelineContext context, StageHolder stage, StageTermination.Exit status) throws JobProcessingException {
        try {
            this.pipelineAPIClient.pipelines().pipeline().pipelineStages().pipelineStage().patch(req -> req
                    .pipelineId(context.pipelineId())
                    .stageType(stage.type().name())
                    .stageName(stage.stage().name())
                    .payload(term -> term.exit(status))
            );
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error notifying pipeline %s stage %s execution start",
                    context.pipelineId(), stage),
                    e);
            throw new JobProcessingException("error executing pipeline stage, see logs with error-token=" + errorToken);
        }
    }

    private void notifyPipelineTerminationStatus(PipelineContext context, PipelineTermination.Exit status) throws JobProcessingException {
        try {
            this.pipelineAPIClient.pipelines().pipeline().patch(req -> req
                    .pipelineId(context.pipelineId())
                    .payload(term -> term.exit(status))
            );
        } catch (IOException e) {
            String errorToken = log.tokenized().error(String.format(
                    "error notifying pipeline %s termination status %s",
                    context.pipelineId(), status),
                    e);
            throw new JobProcessingException("error executing pipeline, see logs with error-token=" + errorToken);
        }
    }
}
