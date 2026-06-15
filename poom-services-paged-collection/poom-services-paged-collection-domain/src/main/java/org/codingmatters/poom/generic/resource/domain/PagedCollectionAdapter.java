package org.codingmatters.poom.generic.resource.domain;

import org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.impl.*;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import java.util.Optional;

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

    interface OrderedPage<EntityType> {
        PagedEntityList<EntityType> list();
        Optional<String> since();
        Optional<String> before();
    }

    interface OrderedLister<EntityType> {
        /**
         * Returns the last elements of the collection, ordered oldest-to-newest.
         * Response since: cursor before the first returned element (for forward paging).
         * Response before: cursor after the last returned element (for backward paging).
         */
        OrderedPage<EntityType> initLatest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException;

        /**
         * Returns the first elements of the collection, ordered oldest-to-newest.
         * Response since: cursor before the first returned element (for forward paging).
         * Response before: Optional.empty() — not meaningful for head initialization.
         */
        OrderedPage<EntityType> initOldest(Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException;

        /**
         * Returns elements after the since cursor, ordered oldest-to-newest.
         * optionalBefore, if present, acts as an exclusive upper bound (filter only, ordering unchanged).
         * Response since: cursor after the last returned element (for continued forward paging).
         * Response before: value of optionalBefore if present, otherwise Optional.empty().
         */
        OrderedPage<EntityType> since(String since, Optional<String> before,
                                       Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException;

        /**
         * Returns elements before the before cursor, ordered newest-to-oldest.
         * Response before: cursor before the oldest returned element (for continued backward paging).
         * Response since: Optional.empty().
         */
        OrderedPage<EntityType> before(String before,
                                        Optional<PropertyQuery> query, long start, long end)
            throws RepositoryException;
    }

    interface Pager<EntityType> {
        String unit();
        int maxPageSize();
        default int defaultPageSize() { return this.maxPageSize();}
        EntityLister<EntityType, PropertyQuery> lister();
        /** Returns null if this pager does not support cursor-based ordered browsing. */
        default OrderedLister<EntityType> orderedLister() { return null; }
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
