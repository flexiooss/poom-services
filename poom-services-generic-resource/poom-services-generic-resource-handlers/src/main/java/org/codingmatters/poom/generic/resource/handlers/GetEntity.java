package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.EntityGetRequest;
import org.codingmatters.poom.api.generic.resource.api.EntityGetResponse;
import org.codingmatters.poom.api.generic.resource.api.entitygetresponse.*;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.api.generic.resource.api.types.Message;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.function.Function;

public class GetEntity implements Function<EntityGetRequest, EntityGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(GetEntity.class);

    private final GenericResourceAdapter.Provider<ObjectValue, ObjectValue, ObjectValue, ObjectValue> adapterProvider;

    public GetEntity(GenericResourceAdapter.Provider<ObjectValue,ObjectValue, ObjectValue, ObjectValue> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    @Override
    public EntityGetResponse apply(EntityGetRequest request) {
        GenericResourceAdapter adapter = null;
        try {
            adapter = this.adapterProvider.adapter();
        } catch (Exception e) {
            String token = log.tokenized().error("failed getting adapter for " + request, e);
            return EntityGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        if(adapter.crud() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, crud should not ne null for {}",
                    adapter.getClass(), request);
            return EntityGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(! request.opt().entityId().isPresent()) {
            String token = log.tokenized().info("no entity id provided to update entity : {}", request);
            return EntityGetResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.BAD_REQUEST)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        Optional<Entity<ObjectValue>> entity;
        try {
            entity = adapter.crud().retrieveEntity(request.entityId());
        } catch (BadRequestException e) {
            return EntityGetResponse.builder().status400(Status400.builder().payload(e.error()).build()).build();
        } catch (ForbiddenException e) {
            return EntityGetResponse.builder().status403(Status403.builder().payload(e.error()).build()).build();
        } catch (NotFoundException e) {
            return EntityGetResponse.builder().status404(Status404.builder().payload(e.error()).build()).build();
        } catch (UnauthorizedException e) {
            return EntityGetResponse.builder().status401(Status401.builder().payload(e.error()).build()).build();
        } catch (UnexpectedException e) {
            return EntityGetResponse.builder().status500(Status500.builder().payload(e.error()).build()).build();
        }

        if(entity.isPresent()) {
            return EntityGetResponse.builder().status200(Status200.builder()
                    .xEntityId(entity.get().id())
                    .location(String.format("%s/%s", adapter.crud().entityRepositoryUrl(), entity.get().id()))
                    .payload(entity.get().value())
                    .build()).build();
        } else {
            String token = log.tokenized().info("no entity found in repository {} for request {}", adapter.crud().entityRepositoryUrl(), request);
            return EntityGetResponse.builder().status404(Status404.builder().payload(Error.builder()
                    .token(token)
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .messages(
                            Message.builder().key(MessageKeys.ENTITY_NOT_FOUND).args(request.entityId()).build(),
                            Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build()
                    )
                    .build()).build()).build();
        }
    }
}
