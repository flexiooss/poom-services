package org.codingmatters.poom.etag.handlers;

import org.codingmatters.poom.etag.storage.Etag;
import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.generated.api.ResourceGetRequest;
import org.generated.api.ResourceGetResponse;
import org.generated.api.TestApiHandlers;
import org.generated.api.resourcegetresponse.Status304;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ETaggedGetTest {

    public static final ResourceGetResponse COMPLETE_UNDERLYING_RESPONSE = ResourceGetResponse.builder().status200(status -> status
            .eTag("cryptic")
            .xEntityId("42")
            .cacheControl("must-revalidate")
    ).build();
    private final Repository<Etag, PropertyQuery> etags = InMemoryRepositoryWithPropertyQuery.validating(Etag.class);

    @Rule
    public HandlerResource<ResourceGetRequest, ResourceGetResponse> get = new HandlerResource<ResourceGetRequest, ResourceGetResponse>() {
        @Override
        protected ResourceGetResponse defaultResponse(ResourceGetRequest request) {
            return null;
        }
    };

    private final TestApiHandlers handlers = new TestApiHandlers.Builder()
            .resourceGetHandler(new ETaggedGet<>(this.etags, "test-cache-control", this.get, ResourceGetResponse.class))
            .build();

    @Test
    public void givenNoETagRequest_andDelegateReturns200__whenDelegateSetsCacheControl__thenUnderlyingResponseReturned_andEtagStored() throws Exception {
        this.get.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        ResourceGetResponse response = this.handlers.resourceGetHandler().apply(ResourceGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected status 200, got : " + response));

        assertThat(response, is(COMPLETE_UNDERLYING_RESPONSE));

        Entity<Etag> etagEntity = this.etags.retrieve(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId());
        assertThat(etagEntity.value().etag(), is(COMPLETE_UNDERLYING_RESPONSE.status200().eTag()));
        assertThat(etagEntity.value().cacheControl(), is(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl()));
        assertThat(etagEntity.value().created(), is(around(UTC.now())));
    }

    @Test
    public void givenRequestWithIfNoneMatch__whenNoEtagStored__thenUnderlyingResponseReturned_andEtagStored() throws Exception {
        this.get.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        ResourceGetResponse response = this.handlers.resourceGetHandler().apply(ResourceGetRequest.builder()
                .ifNoneMatch(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected status 200, got : " + response));

        assertThat(response, is(COMPLETE_UNDERLYING_RESPONSE));

        Entity<Etag> etagEntity = this.etags.retrieve(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId());
        assertThat(etagEntity.value().etag(), is(COMPLETE_UNDERLYING_RESPONSE.status200().eTag()));
        assertThat(etagEntity.value().cacheControl(), is(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl()));
        assertThat(etagEntity.value().created(), is(around(UTC.now())));
    }

    @Test
    public void givenRequestWithIfNoneMatch__whenEtagStored__then304() throws Exception {
        this.etags.createWithId(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId(), Etag.builder()
                .id(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId())
                .cacheControl(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl())
                .etag(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
                .created(UTC.now().minusDays(1L))
                .build());

        this.get.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        ResourceGetResponse response = this.handlers.resourceGetHandler().apply(ResourceGetRequest.builder()
                .ifNoneMatch(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
                .build());

        response.opt().status304().orElseThrow(() -> new AssertionError("expected status 304, got : " + response));

        assertThat(response, is(ResourceGetResponse.builder().status304(Status304.builder()
                .xEntityId(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId())
                .eTag(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
                .cacheControl(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl())
                .build()).build()));
    }

    @Test
    public void givenRequestWithIfNoneMatch__whenDifferentEtagStored__thenUnderlyingResponseReturned_andEtagNotStored() throws Exception {
        Entity<Etag> etag = this.etags.createWithId(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId(), Etag.builder()
                .id(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId())
                .cacheControl(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl())
                .etag("not-quite-the-same")
                .created(UTC.now().minusDays(1L))
                .build());

        this.get.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        ResourceGetResponse response = this.handlers.resourceGetHandler().apply(ResourceGetRequest.builder()
                .ifNoneMatch(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
                .build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected status 200, got : " + response));

        assertThat(response, is(COMPLETE_UNDERLYING_RESPONSE));

        assertThat(this.etags.retrieve(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId()), is(etag));
    }
}