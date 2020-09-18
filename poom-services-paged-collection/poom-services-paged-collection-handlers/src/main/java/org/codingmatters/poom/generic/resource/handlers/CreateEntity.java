package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.paged.collection.api.PagedCollectionPostRequest;
import org.codingmatters.poom.api.paged.collection.api.PagedCollectionPostResponse;
import org.codingmatters.poom.api.paged.collection.api.pagedcollectionpostresponse.*;
import org.codingmatters.poom.api.paged.collection.api.types.Error;
import org.codingmatters.poom.api.paged.collection.api.types.Message;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

public class CreateEntity implements Function<PagedCollectionPostRequest, PagedCollectionPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CreateEntity.class);

    private final PagedCollectionAdapter.Provider<ObjectValue,ObjectValue, ObjectValue, ObjectValue> adapterProvider;

    public CreateEntity(PagedCollectionAdapter.Provider<ObjectValue,ObjectValue, ObjectValue, ObjectValue> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    @Override
    public PagedCollectionPostResponse apply(PagedCollectionPostRequest request) {
        PagedCollectionAdapter adapter = null;
        try {
            adapter = this.adapterProvider.adapter();
        } catch (Exception e) {
            String token = log.tokenized().error("failed getting adapter for " + request, e);
            return PagedCollectionPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(adapter.crud() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, crud should not ne null for {}",
                    adapter.getClass(), request);
            return PagedCollectionPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        if(! adapter.crud().supportedActions().contains(Action.CREATE)) {
            String token = log.tokenized().info("{} action not supported, adapter only supports {}, request was {}",
                    Action.CREATE, adapter.crud().supportedActions(), request);
            return PagedCollectionPostResponse.builder().status405(Status405.builder().payload(Error.builder()
                    .code(Error.Code.ENTITY_CREATION_NOT_ALLOWED)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        ObjectValue value = request.opt().payload().orElseGet(() -> ObjectValue.builder().build());

        Entity<ObjectValue> entity;
        try {
            entity = adapter.crud().createEntityFrom(value);
        } catch (BadRequestException e) {
            return PagedCollectionPostResponse.builder().status400(Status400.builder().payload(e.error()).build()).build();
        } catch (ForbiddenException e) {
            return PagedCollectionPostResponse.builder().status403(Status403.builder().payload(e.error()).build()).build();
        } catch (NotFoundException e) {
            return PagedCollectionPostResponse.builder().status404(Status404.builder().payload(e.error()).build()).build();
        } catch (UnauthorizedException e) {
            return PagedCollectionPostResponse.builder().status401(Status401.builder().payload(e.error()).build()).build();
        } catch (UnexpectedException e) {
            return PagedCollectionPostResponse.builder().status500(Status500.builder().payload(e.error()).build()).build();
        } catch (MethodNotAllowedException e) {
            return PagedCollectionPostResponse.builder().status405(Status405.builder().payload(e.error()).build()).build();
        }

        if(entity == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, created entity is null for {}",
                    adapter.getClass(), request);
            return PagedCollectionPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        return PagedCollectionPostResponse.builder().status201(Status201.builder()
                .xEntityId(entity.id())
                .location(String.format("%s/%s", adapter.crud().entityRepositoryUrl(), entity.id()))
                .payload(entity.value())
                .build()).build();
    }
}
