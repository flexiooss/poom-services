package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.impl.*;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;

public interface PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> {
    @FunctionalInterface
    interface Provider<EntityTpe, CreationType, ReplaceType, UpdateType> {
        PagedCollectionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> adapter() throws Exception;
    }

    @FunctionalInterface
    interface FromRequestProvider<Request, EntityTpe, CreationType, ReplaceType, UpdateType> {
        PagedCollectionAdapter<EntityTpe, CreationType, ReplaceType, UpdateType> adapter(Request request) throws Exception;
    }

    @FunctionalInterface
    interface PagerProvider<EntityType> {
        Pager<EntityType> pager() throws Exception;
    }

    CRUD<EntityType, CreationType, ReplaceType, UpdateType> crud();
    Pager<EntityType> pager();

    interface Pager<EntityType> {
        String unit();
        int maxPageSize();
        default int defaultPageSize() { return this.maxPageSize();}
        EntityLister<EntityType, PropertyQuery> lister();
    }

    interface CRUD<EntityType, CreationType, ReplaceType, UpdateType> extends
            EntityCreator<EntityType, CreationType>,
            BatchEntityCreator<CreationType>,
            EntityRetriever<EntityType>,
            EntityReplacer<EntityType, ReplaceType>,
            EntityUpdater<EntityType, UpdateType>,
            EntityDeleter
        {
            default BatchCreateResponse createEntitiesFrom(CreationType... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
                return new BatchEntityCreatorFromEntityCreator<>(this).createEntitiesFrom(values);
            }
        }

    static <EntityType, CreationType, ReplaceType, UpdateType> DefaultAdapterBuilder<EntityType, CreationType, ReplaceType, UpdateType> builder() {
        return new DefaultAdapterBuilder<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> badRequestAdapter() {
        return new BadRequestAdapter<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> notFoundAdapter() {
        return new NotFoundAdapter<>();
    }

    static <EntityType, CreationType, ReplaceType, UpdateType> PagedCollectionAdapter<EntityType, CreationType, ReplaceType, UpdateType> unexpectedExceptionAdapter() {
        return new UnexpectedExceptionAdapter<>();
    }

}
