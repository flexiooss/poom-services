package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.PagedCollectionGetRequest;
import org.codingmatters.poom.api.generic.resource.api.PagedCollectionGetResponse;
import org.codingmatters.poom.api.generic.resource.api.pagedcollectiongetresponse.*;
import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.api.generic.resource.api.types.Message;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.Optional;
import java.util.function.Function;

public class BrowseEntities implements Function<PagedCollectionGetRequest, PagedCollectionGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(BrowseEntities.class);

    private final GenericResourceAdapter.Provider<ObjectValue> adapterProvider;

    public BrowseEntities(GenericResourceAdapter.Provider<ObjectValue> adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    @Override
    public PagedCollectionGetResponse apply(PagedCollectionGetRequest request) {
        GenericResourceAdapter adapter = null;
        try {
            adapter = this.adapterProvider.adapter();
        } catch (Exception e) {
            String token = log.tokenized().error("failed getting adapter for " + request, e);
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        GenericResourceAdapter.Pager pager = adapter.pager();
        if(pager == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, pager cannot be null, request was : {}",
                    adapter.getClass(), request
            );
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(pager.unit() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, pager unit cannot be null, request was : {}",
                    adapter.getClass(), request
            );
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(pager.lister() == null) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, pager lister cannot be null, request was : {}",
                    adapter.getClass(), request
            );
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }
        if(pager.maxPageSize() <= 0) {
            String token = log.tokenized().error("adapter {} implementation breaks contract, pager max page size  cannot be lower or equal to 0, request was : {}",
                    adapter.getClass(), request
            );
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }


        Rfc7233Pager.Page<ObjectValue> page = null;
        try {
            page = Rfc7233Pager.forRequestedRange(request.range())
                    .unit(pager.unit())
                    .maxPageSize(pager.maxPageSize())
                    .pager(pager.lister()).page(this.parseQuery(request));
        } catch (RepositoryException e) {
            String token = log.tokenized().error("unexpected error listing entities " + request, e);
            return PagedCollectionGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(token)
                    .messages(Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build())
                    .build()).build()).build();
        }

        if(! page.isValid()) {
            String token = log.tokenized().info("illegal search for entities, request was {}", request);
            return PagedCollectionGetResponse.builder().status416(Status416.builder()
                    .acceptRange(page.acceptRange())
                    .contentRange(page.contentRange())
                    .payload(Error.builder()
                            .code(Error.Code.ILLEGAL_RANGE_SPEC)
                            .token(token)
                            .messages(
                                    Message.builder().key(MessageKeys.ILLEGAL_SEARCH_QUERY).args(request.range(), request.filter(), request.orderBy()).build(),
                                    Message.builder().key(MessageKeys.SEE_LOGS_WITH_TOKEN).args(token).build()
                            )
                            .build())
                    .build()).build();
        }
        if(page.isPartial()) {
            return PagedCollectionGetResponse.builder().status206(Status206.builder()
                    .acceptRange(page.acceptRange())
                    .contentRange(page.contentRange())
                    .payload(page.list().valueList())
                    .build()).build();
        } else {
            return PagedCollectionGetResponse.builder().status200(Status200.builder()
                    .acceptRange(page.acceptRange())
                    .contentRange(page.contentRange())
                    .payload(page.list().valueList())
                    .build()).build();
        }
    }

    private Optional<PropertyQuery> parseQuery(PagedCollectionGetRequest request) {
        if(request.opt().filter().isPresent() || request.opt().orderBy().isPresent()) {
            return Optional.of(PropertyQuery.builder().filter(request.filter()).sort(request.orderBy()).build());
        } else {
            return Optional.empty();
        }
    }
}
