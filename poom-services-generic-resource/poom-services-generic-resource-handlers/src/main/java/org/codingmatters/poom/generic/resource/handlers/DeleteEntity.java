package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.EntityDeleteRequest;
import org.codingmatters.poom.api.generic.resource.api.EntityDeleteResponse;
import org.codingmatters.poom.api.generic.resource.api.entitydeleteresponse.*;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.api.generic.resource.api.types.Message;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class DeleteEntity implements Function<EntityDeleteRequest, EntityDeleteResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(DeleteEntity.class);

    private final GenericResourceAdapter.Provider<ObjectValue> adapterProvider;

    public DeleteEntity(GenericResourceAdapter.Provider<ObjectValue> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    @Override
    public EntityDeleteResponse apply(EntityDeleteRequest request) {
        GenericResourceAdapter adapter;
        try {
            adapter = this.adapterProvider.adapter();
        } catch (Exception e) {
            String token = log.tokenized().error("failed getting adapter for " + request, e);
            return EntityDeleteResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(adapter.crud() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, crud should not ne null for {}",
                    adapter.getClass(), request);
            return EntityDeleteResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(! request.opt().entityId().isPresent()) {
            String token = log.tokenized().info("no entity id provided to update entity : {}", request);
            return EntityDeleteResponse.builder().status400(Status400.builder().payload(Error.builder()
                    .code(Error.Code.BAD_REQUEST)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        try {
            adapter.crud().deleteEntity(request.entityId());
        } catch (BadRequestException e) {
            return EntityDeleteResponse.builder().status400(Status400.builder().payload(e.error()).build()).build();
        } catch (ForbiddenException e) {
            return EntityDeleteResponse.builder().status403(Status403.builder().payload(e.error()).build()).build();
        } catch (NotFoundException e) {
            return EntityDeleteResponse.builder().status404(Status404.builder().payload(e.error()).build()).build();
        } catch (UnauthorizedException e) {
            return EntityDeleteResponse.builder().status401(Status401.builder().payload(e.error()).build()).build();
        } catch (UnexpectedException e) {
            return EntityDeleteResponse.builder().status500(Status500.builder().payload(e.error()).build()).build();
        }

        return EntityDeleteResponse.builder().status204(Status204.builder().build()).build();
    }
}
