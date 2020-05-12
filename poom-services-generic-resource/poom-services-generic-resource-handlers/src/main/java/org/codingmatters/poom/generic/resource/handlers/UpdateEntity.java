package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.EntityPatchRequest;
import org.codingmatters.poom.api.generic.resource.api.EntityPatchResponse;
import org.codingmatters.poom.api.generic.resource.api.entitypatchresponse.*;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.api.generic.resource.api.types.Message;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class UpdateEntity implements Function<EntityPatchRequest, EntityPatchResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(UpdateEntity.class);

    private final GenericResourceAdapter.Provider<ObjectValue> adapterProvider;

    public UpdateEntity(GenericResourceAdapter.Provider<ObjectValue> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    @Override
    public EntityPatchResponse apply(EntityPatchRequest request) {
        GenericResourceAdapter adapter = null;
        try {
            adapter = this.adapterProvider.adapter();
        } catch (Exception e) {
            String token = log.tokenized().error("failed getting adapter for " + request, e);
            return EntityPatchResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(adapter.crud() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, crud should not ne null for {}",
                    adapter.getClass(), request);
            return EntityPatchResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(! adapter.crud().supportedActions().contains(Action.UPDATE)) {
            String token = log.tokenized().error("{} action not suppoprted, adapter only supports {}, request was {}",
                    Action.UPDATE, adapter.crud().supportedActions(), request);
            return EntityPatchResponse.builder().status405(Status405.builder().payload(Error.builder()
                    .code(Error.Code.ENTITY_UPDATE_NOT_ALLOWED)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(! request.opt().entityId().isPresent()) {
            String token = log.tokenized().info("no entity id provided to update entity : {}", request);
            return EntityPatchResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.BAD_REQUEST)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        ObjectValue value = request.opt().payload().orElseGet(() -> ObjectValue.builder().build());

        Entity<ObjectValue> entity;
        try {
            entity = adapter.crud().updateEntityWith(request.entityId(), value);
        } catch (BadRequestException e) {
            return EntityPatchResponse.builder().status400(Status400.builder().payload(e.error()).build()).build();
        } catch (ForbiddenException e) {
            return EntityPatchResponse.builder().status403(Status403.builder().payload(e.error()).build()).build();
        } catch (NotFoundException e) {
            return EntityPatchResponse.builder().status404(Status404.builder().payload(e.error()).build()).build();
        } catch (UnauthorizedException e) {
            return EntityPatchResponse.builder().status401(Status401.builder().payload(e.error()).build()).build();
        } catch (UnexpectedException e) {
            return EntityPatchResponse.builder().status500(Status500.builder().payload(e.error()).build()).build();
        }

        if(entity == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, created entity is null for {}",
                    adapter.getClass(), request);
            return EntityPatchResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        return EntityPatchResponse.builder().status200(Status200.builder()
                .xEntityId(entity.id())
                .location(String.format("%s/%s", adapter.crud().entityRepositoryUrl(), entity.id()))
                .payload(entity.value())
                .build()).build();
    }
}
