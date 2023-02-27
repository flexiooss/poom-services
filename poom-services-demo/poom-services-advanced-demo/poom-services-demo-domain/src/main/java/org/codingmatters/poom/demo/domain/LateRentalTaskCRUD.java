package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.apis.demo.api.types.LateRentalTask;
import org.codingmatters.poom.demo.domain.rentals.LateRentalProcessor;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class LateRentalTaskCRUD implements PagedCollectionAdapter.CRUD<LateRentalTask, ObjectValue, Void, Void> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(LateRentalTaskCRUD.class);

    private final Repository<LateRentalTask, PropertyQuery> repository;
    private final LateRentalProcessor processor;
    private final ExecutorService pool;

    public LateRentalTaskCRUD(LateRentalProcessor processor, ExecutorService pool) {
        this.processor = processor;
        this.pool = pool;
        this.repository = this.processor.taskRepository();
    }

    @Override
    public String entityRepositoryUrl() {
        return "/late-rental-tasks";
    }

    @Override
    public Optional<Entity<LateRentalTask>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            return Optional.ofNullable(this.repository.retrieve(id));
        } catch (RepositoryException e) {
            throw new UnexpectedException(
                    Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("unexpected error creating task", e))
                            .build(),
                    "unexpected error creating task",
                    e);
        }
    }

    @Override
    public Entity<LateRentalTask> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            Entity<LateRentalTask>  taskEntity = this.repository.create(LateRentalTask.builder().start(UTC.now()).status(LateRentalTask.Status.RUNNING).build());
            this.repository.update(taskEntity, taskEntity.value().withId(taskEntity.id()));
            this.pool.submit(() -> processor.process(taskEntity));
            return taskEntity;
        } catch (RepositoryException e) {
            throw new UnexpectedException(
                    Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("unexpected error creating task", e))
                            .build(),
                    "unexpected error creating task",
                    e);
        }
    }

    @Override
    public Entity<LateRentalTask> replaceEntityWith(String id, Void value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new BadRequestException(
                Error.builder()
                        .code(Error.Code.ENTITY_REPLACEMENT_NOT_ALLOWED)
                        .token(log.tokenized().error("not implemented"))
                        .build(),
                "not implemented");
    }

    @Override
    public Entity<LateRentalTask> updateEntityWith(String id, Void value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new BadRequestException(
                Error.builder()
                        .code(Error.Code.ENTITY_UPDATE_NOT_ALLOWED)
                        .token(log.tokenized().error("not implemented"))
                        .build(),
                "not implemented");
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new BadRequestException(
                Error.builder()
                        .code(Error.Code.BAD_REQUEST)
                        .token(log.tokenized().error("not implemented"))
                        .build(),
                "not implemented");
    }
}
