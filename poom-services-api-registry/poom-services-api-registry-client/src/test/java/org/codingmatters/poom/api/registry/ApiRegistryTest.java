package org.codingmatters.poom.api.registry;

import org.codingmatters.poom.api.registry.api.*;
import org.codingmatters.poom.api.registry.api.types.ApiSpec;
import org.codingmatters.poom.api.registry.api.types.Error;
import org.codingmatters.poom.api.registry.api.types.optional.OptionalApiSpec;
import org.codingmatters.poom.api.registry.client.ApiRegistryClient;
import org.codingmatters.poom.api.registry.client.ApiRegistryHandlersClient;
import org.codingmatters.poom.handler.CumulatingHandlerResource;
import org.codingmatters.poom.handler.HandlerResource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ApiRegistryTest {

    public static final int REFRESH_TICK = 200;
    @Rule
    public HandlerResource<AnApiGetRequest, AnApiGetResponse> retrieveAnApi = new HandlerResource<>() {
        @Override
        protected AnApiGetResponse defaultResponse(AnApiGetRequest request) {
            return AnApiGetResponse.builder().status405(status -> status.payload(e -> e.code(Error.Code.BAD_REQUEST))).build();
        }
    };
    @Rule
    public CumulatingHandlerResource<ApisGetRequest, ApisGetResponse> browseApis = new CumulatingHandlerResource<>() {
        @Override
        protected ApisGetResponse defaultResponse(ApisGetRequest request) {
            return ApisGetResponse.builder().status405(status -> status.payload(e -> e.code(Error.Code.BAD_REQUEST))).build();
        }
    };
    public ApiRegistryClient registryClient = new ApiRegistryHandlersClient(new ApiRegistryHandlers.Builder()
            .anApiGetHandler(this.retrieveAnApi)
            .apisGetHandler(this.browseApis)
            .build(), Executors.newSingleThreadExecutor());
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @After
    public void tearDown() throws Exception {
        this.scheduler.shutdownNow();
    }

    @Test
    public void givenNoRefreshement__whenNotGettingApi__thenRetrieveNotCalled_andBrowseNotCalled() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            assertThat(this.browseApis.lastRequest(), is(nullValue()));
            assertThat(this.retrieveAnApi.lastRequest(), is(nullValue()));
        }
    }

    @Test
    public void givenNoRefreshement__whenGettingApi_andRetrieveReturns404__thenRetrieveCalled_andBrowseNotCalled_andSpecIsEmpty() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status404(status -> status.payload(e -> e.code(Error.Code.RESOURCE_NOT_FOUND))).build());
            OptionalApiSpec spec = registry.api("an-api");

            assertThat(this.browseApis.lastRequest(), is(nullValue()));
            assertThat(this.retrieveAnApi.lastRequest(), is(AnApiGetRequest.builder().api("an-api").build()));
            assertThat(spec.isEmpty(), is(true));
        }
    }

    @Test
    public void givenNoRefreshement__whenGettingApi_andRetrieveReturns200__thenRetrieveCalled_andBrowseNotCalled_andSpecIsReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            ApiSpec expectedSpec = ApiSpec.builder()
                    .name("an-api").version("1").endpoint("/pat/to/api")
                    .build();
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(expectedSpec)).build());
            OptionalApiSpec spec = registry.api("an-api");

            assertThat(this.browseApis.lastRequest(), is(nullValue()));
            assertThat(this.retrieveAnApi.lastRequest(), is(AnApiGetRequest.builder().api("an-api").build()));
            assertThat(spec.isPresent(), is(true));
            assertThat(spec.get(), is(expectedSpec));
        }
    }

    @Test
    public void givenNoRefreshement__whenGettingApi_andRetrieveReturns200_andPaylodEmpty__thenRetrieveCalled_andBrowseNotCalled_andSpecIsEmpty() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload((ApiSpec) null)).build());
            OptionalApiSpec spec = registry.api("an-api");

            assertThat(this.browseApis.lastRequest(), is(nullValue()));
            assertThat(this.retrieveAnApi.lastRequest(), is(AnApiGetRequest.builder().api("an-api").build()));
            assertThat(spec.isPresent(), is(false));
        }
    }

    @Test
    public void givenNoRefreshement__whenGettingApiTwice_andRetrieveReturns200__thenRetrieveNotCalledOnSecondGet_andSameSpecReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            ApiSpec expectedSpec = ApiSpec.builder()
                    .name("an-api").version("1").endpoint("/pat/to/api")
                    .build();
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(expectedSpec)).build());
            OptionalApiSpec spec = registry.api("an-api");

            this.retrieveAnApi.reset();

            OptionalApiSpec secondSpec = registry.api("an-api");
            assertThat(this.retrieveAnApi.lastRequest(), is(nullValue()));

            assertThat(spec.isPresent(), is(true));
            assertThat(secondSpec.get(), is(spec.get()));
        }
    }

    @Test
    public void givenNoRefreshement__whenFirstReturns404_andSecondReturns200__thenRetrieveCalledOnBothGet_andSpecReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, null)) {
            ApiSpec expectedSpec = ApiSpec.builder()
                    .name("an-api").version("1").endpoint("/pat/to/api")
                    .build();

            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status404(status -> status.payload(e -> e.code(Error.Code.RESOURCE_NOT_FOUND))).build());

            OptionalApiSpec spec = registry.api("an-api");

            assertThat(spec.isEmpty(), is(true));
            assertThat(this.retrieveAnApi.lastRequest(), is(AnApiGetRequest.builder().api("an-api").build()));

            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(expectedSpec)).build());
            this.retrieveAnApi.reset();

            OptionalApiSpec secondSpec = registry.api("an-api");
            assertThat(this.retrieveAnApi.lastRequest(), is(AnApiGetRequest.builder().api("an-api").build()));

            assertThat(secondSpec.isPresent(), is(true));
            assertThat(secondSpec.get(), is(expectedSpec));
        }
    }

    @Test
    public void givenRefreshment__whenNotGettingApi__thenNoRefreshmentAfterTwoTicks() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, this.scheduler, REFRESH_TICK)) {
            Thread.sleep(400);
            assertThat(this.browseApis.lastRequest(), is(nullValue()));
        }
    }

    @Test
    public void givenRefreshment__whenGettingAnApi_andGettingSameApiAfterTick__thenRetrieveNotCalled_andBrowseCalledWithCachedApis_andNewVersionReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, this.scheduler, REFRESH_TICK)) {
            ApiSpec spec = ApiSpec.builder().name("an-api").version("1").build();
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(spec)).build());
            registry.api("an-api");

            this.retrieveAnApi.reset();
            this.browseApis.nextResponse(request -> ApisGetResponse.builder().status200(status -> status.payload(
                    spec.withVersion("2")
            )).build());

            Thread.sleep(2 * REFRESH_TICK);

            assertThat(this.retrieveAnApi.lastRequest(), is(nullValue()));
            assertThat(this.browseApis.lastRequest(), is(ApisGetRequest.builder()
                    .filter("name in ('an-api')")
                    .build()));
            assertThat(registry.api("an-api").get().version(), is("2"));
        }
    }

    @Test
    public void givenRefreshment__whenGettingApis_andGettingSameApisAfterTick__thenRetrieveNotCalled_andBrowseCalledWithCachedApis_andNewVersionReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, this.scheduler, REFRESH_TICK)) {
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(ApiSpec.builder().name(request.api()).version("1").build())).build());
            OptionalApiSpec spec1 = registry.api("an-api");
            OptionalApiSpec spec2 = registry.api("another-api");


            this.retrieveAnApi.reset();
            this.browseApis.nextResponse(request -> ApisGetResponse.builder().status200(status -> status.payload(
                    spec1.get().withVersion("2"),
                    spec2.get().withVersion("2")
            )).build());

            Thread.sleep(2 * REFRESH_TICK);

            assertThat(this.retrieveAnApi.lastRequest(), is(nullValue()));
            assertThat(this.browseApis.lastRequest(), is(ApisGetRequest.builder()
                    .filter("name in ('an-api','another-api')")
                    .build()));
            assertThat(registry.api("an-api").get().version(), is("2"));
            assertThat(registry.api("another-api").get().version(), is("2"));
        }
    }



    @Test
    public void givenRefreshment__whenGettingAnApi_andGettingSameApiAfterTick_andBrowsePaginating__thenRetrieveNotCalled_andBrowseCalledWithCachedApis_andNewVersionReturned() throws Exception {
        try(ApiRegistry registry = new ApiRegistry(this.registryClient, this.scheduler, REFRESH_TICK)) {
            ApiSpec spec1 = ApiSpec.builder().name("an-api").version("1").build();
            ApiSpec spec2 = ApiSpec.builder().name("another-api").version("1").build();
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(spec1)).build());
            registry.api("an-api");
            this.retrieveAnApi.nextResponse(request -> AnApiGetResponse.builder().status200(status -> status.payload(spec2)).build());
            registry.api("another-api");

            this.retrieveAnApi.reset();
            this.browseApis.nextResponse(request -> ApisGetResponse.builder().status206(status -> status
                    .contentRange("0-0/1")
                    .acceptRange("ApiSpec 10")
                    .payload(spec1.withVersion("2"))
            ).build());
            this.browseApis.nextResponse(request -> ApisGetResponse.builder().status200(status -> status.payload(spec2.withVersion("2"))).build());

            Thread.sleep(2 * REFRESH_TICK);

            assertThat(this.retrieveAnApi.lastRequest(), is(nullValue()));
            assertThat(this.browseApis.requests().get(0), is(ApisGetRequest.builder()
                    .filter("name in ('an-api','another-api')")
                    .build()));
            assertThat(this.browseApis.requests().get(1), is(ApisGetRequest.builder()
                    .filter("name in ('an-api','another-api')")
                    .range("1-10")
                    .build()));
            assertThat(registry.api("an-api").get().version(), is("2"));
            assertThat(registry.api("another-api").get().version(), is("2"));
        }
    }


}