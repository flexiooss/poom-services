package org.codingmatters.poom.etag.handlers;

import org.codingmatters.poom.etag.storage.Etag;
import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.generated.api.*;
import org.junit.Test;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class ETaggedChangeTest {

    public static final ResourcePutResponse COMPLETE_UNDERLYING_RESPONSE = ResourcePutResponse.builder().status200(status -> status
            .eTag("cryptic")
            .xEntityId("42")
            .cacheControl("must-revalidate")
    ).build();
    private final Repository<Etag, PropertyQuery> etags = InMemoryRepositoryWithPropertyQuery.validating(Etag.class);

    private final HandlerResource<ResourcePutRequest, ResourcePutResponse> put = new HandlerResource<ResourcePutRequest, ResourcePutResponse>() {
        @Override
        protected ResourcePutResponse defaultResponse(ResourcePutRequest request) {
            return null;
        }
    };

    private final TestApiHandlers handlers = new TestApiHandlers.Builder()
            .resourcePutHandler(new ETaggedChange<>(this.etags, "test-cache-control", this.put, ResourcePutResponse.class))
            .build();

    @Test
    public void givenNoETagRequest_andDelegateReturns200__whenDelegateSetsCacheControl__thenUnderlyingResponseReturned_andEtagStored() throws Exception {
        this.put.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        ResourcePutResponse response = this.handlers.resourcePutHandler().apply(ResourcePutRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected status 200, got : " + response));

        assertThat(response, is(COMPLETE_UNDERLYING_RESPONSE));

        Entity<Etag> etagEntity = this.etags.retrieve(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId());
        assertThat(etagEntity.value().etag(), is(COMPLETE_UNDERLYING_RESPONSE.status200().eTag()));
        assertThat(etagEntity.value().cacheControl(), is(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl()));
        assertThat(etagEntity.value().created(), is(around(UTC.now())));
    }

    @Test
    public void givenRequestWithIfMatch__whenNoEtagStored__then412_andUnderlyingNotCalled() throws Exception {
        ResourcePutResponse response = this.handlers.resourcePutHandler().apply(ResourcePutRequest.builder()
                .ifMatch("current")
                .build());

        response.opt().status412().orElseThrow(() -> new AssertionError("expected 412, got : " + response));

        assertThat(response.status412().errorToken(), is(notNullValue()));
        assertThat(this.put.lastRequest(), is(nullValue()));
    }

    @Test
    public void givenRequestWithIfMatch__whenEtagStored_andEtagDiffers__then412_andUnderlyingNotCalled() throws Exception {
        this.etags.createWithId(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId(), Etag.builder()
                .id(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId())
                .cacheControl(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl())
                .etag("not-quite-the-same")
                .created(UTC.now().minusDays(1L))
                .build());

        ResourcePutResponse response = this.handlers.resourcePutHandler().apply(ResourcePutRequest.builder()
                .ifMatch("current")
                .build());

        response.opt().status412().orElseThrow(() -> new AssertionError("expected 412, got : " + response));

        assertThat(response.status412().errorToken(), is(notNullValue()));
        assertThat(this.put.lastRequest(), is(nullValue()));
    }

    @Test
    public void givenRequestWithIfMatch__whenEtagStored_andEtagMatch__thenUnderlyingCalled_and200_andEtagUpdated() throws Exception {
        this.put.nextResponse(request -> COMPLETE_UNDERLYING_RESPONSE);

        this.etags.createWithId(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId(), Etag.builder()
                .id(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId())
                .cacheControl(COMPLETE_UNDERLYING_RESPONSE.status200().cacheControl())
                .etag("current")
                .created(UTC.now().minusDays(1L))
                .build());

        ResourcePutResponse response = this.handlers.resourcePutHandler().apply(ResourcePutRequest.builder()
                .ifMatch("current")
                .build());

        assertThat(this.put.lastRequest(), is(ResourcePutRequest.builder()
                .ifMatch("current")
                .build()));

        assertThat(response, is(COMPLETE_UNDERLYING_RESPONSE));

        assertThat(
                this.etags.retrieve(COMPLETE_UNDERLYING_RESPONSE.status200().xEntityId()).value().etag(),
                is(COMPLETE_UNDERLYING_RESPONSE.status200().eTag())
        );
    }
}