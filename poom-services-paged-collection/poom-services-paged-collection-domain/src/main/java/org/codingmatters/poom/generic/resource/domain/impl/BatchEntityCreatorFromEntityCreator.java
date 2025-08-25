package org.codingmatters.poom.generic.resource.domain.impl;

import org.codingmatters.poom.api.paged.collection.api.types.BatchCreateResponse;
import org.codingmatters.poom.api.paged.collection.api.types.batchcreateresponse.Errors;
import org.codingmatters.poom.generic.resource.domain.BatchEntityCreator;
import org.codingmatters.poom.generic.resource.domain.EntityCreator;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.value.objects.values.ObjectValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class BatchEntityCreatorFromEntityCreator<EntityType, CreationType> implements BatchEntityCreator<CreationType> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BatchEntityCreatorFromEntityCreator.class);

    private final EntityCreator<EntityType, CreationType> entityCreator;

    public BatchEntityCreatorFromEntityCreator(EntityCreator<EntityType, CreationType> entityCreator) {
        this.entityCreator = entityCreator;
    }

    @Override
    public BatchCreateResponse createEntitiesFrom(CreationType ... values) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException, MethodNotAllowedException {
        if(values != null) {
            BatchCreateResponse.Builder result = BatchCreateResponse.builder();
            for (CreationType value : values) {
                try {
                    Entity<EntityType> entity = this.entityCreator.createEntityFrom(value);
                    result.successAdd(entity.id());
                } catch (BadRequestException e) {
                    result.errorsAdd(Errors.builder()
                                    .error(e.error())
                                    .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                } catch (ForbiddenException e) {
                    result.errorsAdd(Errors.builder()
                            .error(e.error())
                            .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                } catch (NotFoundException e) {
                    result.errorsAdd(Errors.builder()
                            .error(e.error())
                            .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                } catch (UnauthorizedException e) {
                    result.errorsAdd(Errors.builder()
                            .error(e.error())
                            .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                } catch (UnexpectedException e) {
                    result.errorsAdd(Errors.builder()
                            .error(e.error())
                            .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                } catch (MethodNotAllowedException e) {
                    result.errorsAdd(Errors.builder()
                            .error(e.error())
                            .entity(ObjectValue.fromMap(this.mapFrom(value)).build())
                            .build());
                }
            }
            return result.build();
        } else {
            return BatchCreateResponse.builder().success(List.of()).build();
        }
    }

    private Map mapFrom(CreationType value) {
        try {
            Method toMap = value.getClass().getMethod("toMap");
            return (Map) toMap.invoke(value);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassCastException e) {
            log.warn("paged collection on non value object entities");
            return Map.of("str", value.toString());
        }
    }

    @Override
    public String entityType() {
        return this.entityCreator.entityType();
    }

    @Override
    public String entityRepositoryUrl() {
        return this.entityCreator.entityRepositoryUrl();
    }
}
