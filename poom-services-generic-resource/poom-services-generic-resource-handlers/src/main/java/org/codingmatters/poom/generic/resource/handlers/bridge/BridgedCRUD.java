package org.codingmatters.poom.generic.resource.handlers.bridge;

import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class BridgedCRUD<EntityTpe, CreationType, ReplaceType, UpdateType> implements GenericResourceAdapter.CRUD<ObjectValue, ObjectValue, ObjectValue, ObjectValue> {
    private final GenericResourceAdapter.CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> delegate;

    private final Function<EntityTpe, ObjectValue> fromEntityType;
    private final Function<ObjectValue, CreationType> toCreationType;
    private final Function<ObjectValue, ReplaceType> toReplaceType;
    private final Function<ObjectValue, UpdateType> toUpdateType;


    public BridgedCRUD(
            GenericResourceAdapter.CRUD<EntityTpe, CreationType, ReplaceType, UpdateType> delegate,
            Function<EntityTpe, ObjectValue> fromEntityType,
            Function<ObjectValue, CreationType> toCreationType,
            Function<ObjectValue, ReplaceType> toReplaceType,
            Function<ObjectValue, UpdateType> toUpdateType
    ) {
        this.delegate = delegate;
        this.fromEntityType = fromEntityType;
        this.toCreationType = toCreationType;
        this.toReplaceType = toReplaceType;
        this.toUpdateType = toUpdateType;
    }

    @Override
    public String entityRepositoryUrl() {
        return this.delegate.entityRepositoryUrl();
    }

    @Override
    public Set<Action> supportedActions() {
        return this.delegate.supportedActions();
    }

    @Override
    public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        Optional<Entity<EntityTpe>> entity = this.delegate.retrieveEntity(id);
        if(entity.isPresent()) {
            return Optional.of(this.fromEntity(entity.get()));
        }
        return Optional.empty();
    }

    @Override
    public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        return this.fromEntity(this.delegate.createEntityFrom(this.toCreationType.apply(value)));
    }

    @Override
    public Entity<ObjectValue> replaceEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        return this.fromEntity(this.delegate.replaceEntityWith(id, this.toReplaceType.apply(value)));
    }

    @Override
    public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        return this.fromEntity(this.delegate.updateEntityWith(id, this.toUpdateType.apply(value)));
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        this.delegate.deleteEntity(id);
    }

    private Entity<ObjectValue> fromEntity(Entity<EntityTpe> entity) {
        return new ImmutableEntity<>(entity.id(), entity.version(), this.fromEntityType.apply(entity.value()));
    }
}
