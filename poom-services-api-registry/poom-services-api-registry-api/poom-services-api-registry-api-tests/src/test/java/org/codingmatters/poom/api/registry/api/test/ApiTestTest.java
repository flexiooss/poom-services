package org.codingmatters.poom.api.registry.api.test;

import org.codingmatters.poom.api.registry.api.AnApiGetRequest;
import org.codingmatters.poom.api.registry.api.AnApiGetResponse;
import org.codingmatters.poom.api.registry.api.types.ApiSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ApiTestTest {
    @RegisterExtension
    static private ApiRegistryTestApi api = new ApiRegistryTestApi();

    @Test
    void pass1() throws Exception {
        assertThat(this.api.anApiGet().lastRequest(), is(nullValue()));

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api0").build()), is(nullValue()));

        this.api.anApiGet().nextResponse(r -> AnApiGetResponse.builder().status200(st -> st.payload(ApiSpec.builder().name(r.api()).build())).build());

        assertThat(this.api.anApiGet().lastRequest().api(), is("api0"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api0"));

        this.api.anApiGet().reset();

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api1").build()).status200().payload().name(), is("api1"));
        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api2").build()).status200().payload().name(), is("api2"));

        assertThat(this.api.anApiGet().lastRequest().api(), is("api2"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api1", "api2"));
    }

    @Test
    void pass2() throws Exception {
        assertThat(this.api.anApiGet().lastRequest(), is(nullValue()));

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api0").build()), is(nullValue()));

        this.api.anApiGet().nextResponse(r -> AnApiGetResponse.builder().status200(st -> st.payload(ApiSpec.builder().name(r.api()).build())).build());

        assertThat(this.api.anApiGet().lastRequest().api(), is("api0"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api0"));

        this.api.anApiGet().reset();

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api1").build()).status200().payload().name(), is("api1"));
        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api2").build()).status200().payload().name(), is("api2"));

        assertThat(this.api.anApiGet().lastRequest().api(), is("api2"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api1", "api2"));
    }

    @Test
    void pass3() throws Exception {
        assertThat(this.api.anApiGet().lastRequest(), is(nullValue()));

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api0").build()), is(nullValue()));

        this.api.anApiGet().nextResponse(r -> AnApiGetResponse.builder().status200(st -> st.payload(ApiSpec.builder().name(r.api()).build())).build());

        assertThat(this.api.anApiGet().lastRequest().api(), is("api0"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api0"));

        this.api.anApiGet().reset();

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api1").build()).status200().payload().name(), is("api1"));
        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api2").build()).status200().payload().name(), is("api2"));

        assertThat(this.api.anApiGet().lastRequest().api(), is("api2"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api1", "api2"));
    }

    @Test
    void pass4() throws Exception {
        assertThat(this.api.anApiGet().lastRequest(), is(nullValue()));

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api0").build()), is(nullValue()));

        this.api.anApiGet().nextResponse(r -> AnApiGetResponse.builder().status200(st -> st.payload(ApiSpec.builder().name(r.api()).build())).build());

        assertThat(this.api.anApiGet().lastRequest().api(), is("api0"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api0"));

        this.api.anApiGet().reset();

        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api1").build()).status200().payload().name(), is("api1"));
        assertThat(this.api.client().apis().anApi().get(AnApiGetRequest.builder().api("api2").build()).status200().payload().name(), is("api2"));

        assertThat(this.api.anApiGet().lastRequest().api(), is("api2"));
        assertThat(this.api.anApiGet().requests().stream().map(r -> r.api()).toList(), contains("api1", "api2"));
    }
}
