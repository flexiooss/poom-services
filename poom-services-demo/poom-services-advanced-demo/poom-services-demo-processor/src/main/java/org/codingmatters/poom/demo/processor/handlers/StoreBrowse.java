package org.codingmatters.poom.demo.processor.handlers;

import org.codingmatters.poom.apis.demo.api.StoresGetRequest;
import org.codingmatters.poom.apis.demo.api.StoresGetResponse;
import org.codingmatters.poom.apis.demo.api.storesgetresponse.Status200;
import org.codingmatters.poom.apis.demo.api.storesgetresponse.Status206;
import org.codingmatters.poom.apis.demo.api.storesgetresponse.Status416;
import org.codingmatters.poom.apis.demo.api.storesgetresponse.Status500;
import org.codingmatters.poom.apis.demo.api.types.Error;
import org.codingmatters.poom.demo.domain.StoreManager;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StoreBrowse implements Function<StoresGetRequest, StoresGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(StoreBrowse.class);

    private final StoreManager storeManager;

    public StoreBrowse(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public StoresGetResponse apply(StoresGetRequest request) {
        Rfc7233Pager.Page<Store> page = null;
        try {
            page = Rfc7233Pager.forRequestedRange(request.range())
                    .unit("Store")
                    .maxPageSize(10)
                    .pager(this.storeManager.storeLister())
                    .page();
        } catch (RepositoryException e) {
            return StoresGetResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("error accessing store repository while browsing store, request was " + request, e))
                    .build()).build()).build();
        }

        if(! page.isValid()) {
            return StoresGetResponse.builder().status416(Status416.builder()
                    .acceptRange(page.acceptRange())
                    .contentRange(page.contentRange())
                    .payload(Error.builder()
                            .code(Error.Code.ILLEGAL_RANGE_SPEC)
                            .token(log.tokenized().info("specified range is not acceptable : {} ; request was {}", page.validationMessage(), request))
                            .description(page.validationMessage())
                            .build())
                    .build()).build();
        } else {
            List<ObjectValue> list = page.list().stream()
                    .map(storeEntity -> ObjectValue.builder()
                            .property("store name", PropertyValue.builder().stringValue(storeEntity.value().name()).build())
                            .build())
                    .collect(Collectors.toList());
            if(page.isPartial()) {
                return StoresGetResponse.builder().status206(Status206.builder()
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(list)
                        .build()).build();
            } else {
                return StoresGetResponse.builder().status200(Status200.builder()
                        .acceptRange(page.acceptRange())
                        .contentRange(page.contentRange())
                        .payload(list)
                        .build()).build();
            }
        }
    }
}
