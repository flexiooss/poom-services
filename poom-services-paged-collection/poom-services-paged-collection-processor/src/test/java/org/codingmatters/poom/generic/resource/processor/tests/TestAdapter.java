package org.codingmatters.poom.generic.resource.processor.tests;

import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.EntityLister;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestAdapter implements PagedCollectionAdapter<ObjectValue,ObjectValue, ObjectValue, ObjectValue> {

    public final AtomicInteger listCounter = new AtomicInteger(0);
    public final AtomicInteger searchCounter = new AtomicInteger(0);

    public final AtomicInteger createCounter = new AtomicInteger(0);
    public final AtomicInteger retrieveCounter = new AtomicInteger(0);
    public final AtomicInteger updateCounter = new AtomicInteger(0);
    public final AtomicInteger replaceCounter = new AtomicInteger(0);
    public final AtomicInteger deleteCounter = new AtomicInteger(0);

    public static final ObjectValue OBJECT_VALUE = ObjectValue.builder().property("p", PropertyValue.builder().stringValue("v").build()).build();

    @Override
    public CRUD<ObjectValue,ObjectValue, ObjectValue, ObjectValue> crud() {
        return new CRUD<>() {
            @Override
            public String entityRepositoryUrl() {
                return "flexio-api://go/there";
            }

            @Override
            public Set<Action> supportedActions() {
                return Action.all;
            }

            @Override
            public Optional<Entity<ObjectValue>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                retrieveCounter.incrementAndGet();
                return Optional.of(new ImmutableEntity<>(id, BigInteger.ONE, OBJECT_VALUE));
            }

            @Override
            public Entity<ObjectValue> createEntityFrom(ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                createCounter.incrementAndGet();
                return new ImmutableEntity<>("12", BigInteger.ONE, OBJECT_VALUE);
            }

            @Override
            public Entity<ObjectValue> replaceEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                replaceCounter.incrementAndGet();
                return new ImmutableEntity<>(id, BigInteger.TWO, value);
            }

            @Override
            public Entity<ObjectValue> updateEntityWith(String id, ObjectValue value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                updateCounter.incrementAndGet();
                return new ImmutableEntity<>(id, BigInteger.valueOf(3), value);
            }

            @Override
            public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
                deleteCounter.incrementAndGet();
            }
        };
    }

    @Override
    public Pager<ObjectValue> pager() {
        return new Pager<>() {
            @Override
            public String unit() {
                return "Thing";
            }

            @Override
            public int maxPageSize() {
                return 100;
            }

            @Override
            public EntityLister<ObjectValue, PropertyQuery> lister() {
                return new EntityLister<ObjectValue, PropertyQuery>() {
                    @Override
                    public PagedEntityList<ObjectValue> all(long start, long end) throws RepositoryException {
                        listCounter.incrementAndGet();
                        return new PagedEntityList.DefaultPagedEntityList<ObjectValue>(start, end, 10000, entities((int) (end - start)));
                    }

                    @Override
                    public PagedEntityList<ObjectValue> search(PropertyQuery propertyQuery, long start, long end) throws RepositoryException {
                        searchCounter.incrementAndGet();
                        return new PagedEntityList.DefaultPagedEntityList<ObjectValue>(start, end, 10000, entities((int) (end - start)));
                    }
                };
            }
        };
    }

    static private List<Entity<ObjectValue>> entities(int count) {
        List<Entity<ObjectValue>> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(new ImmutableEntity<>("" + i, BigInteger.ONE, OBJECT_VALUE));
        }
        return results;
    }
}
