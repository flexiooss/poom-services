package org.codingmatters.poom.generic.resource.handlers.bridge;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;
import java.util.stream.Collectors;

public class BridgedLister<EntityTpe> implements EntityLister<ObjectValue, PropertyQuery> {

    private final EntityLister<EntityTpe, PropertyQuery> delegate;
    private final Function<EntityTpe, ObjectValue> fromEntityType;

    public BridgedLister(EntityLister<EntityTpe, PropertyQuery> delegate, Function<EntityTpe, ObjectValue> fromEntityType) {
        this.delegate = delegate;
        this.fromEntityType = fromEntityType;
    }

    @Override
    public PagedEntityList<ObjectValue> all(long startIndex, long endIndex) throws RepositoryException {
        return this.fromPage(this.delegate.all(startIndex, endIndex));
    }

    @Override
    public PagedEntityList<ObjectValue> search(PropertyQuery query, long startIndex, long endIndex) throws RepositoryException {
        return this.fromPage(this.delegate.search(query, startIndex, endIndex));
    }

    private PagedEntityList<ObjectValue> fromPage(PagedEntityList<EntityTpe> page) {
        return new PagedEntityList.DefaultPagedEntityList<>(
                page.startIndex(),
                page.endIndex(),
                page.total(),
                page.stream().map(this::fromEntity).collect(Collectors.toList())
        );
    }

    private Entity<ObjectValue> fromEntity(Entity<EntityTpe> entity) {
        return new ImmutableEntity<>(entity.id(), entity.version(), this.fromEntityType.apply(entity.value()));
    }
}
