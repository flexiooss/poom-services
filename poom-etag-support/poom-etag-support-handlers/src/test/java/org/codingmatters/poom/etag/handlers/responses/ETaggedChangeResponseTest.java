package org.codingmatters.poom.etag.handlers.responses;


import org.generated.api.ResourceGetResponse;
import org.generated.api.ResourcePutResponse;
import org.generated.api.resourceputresponse.Status412;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ETaggedChangeResponseTest {

    @Test
    public void givenBuildedFromResponse__whenStatus200__thenPropertiesFromResponse() throws Exception {
        ResourceGetResponse response = ResourceGetResponse.builder()
                .status200(status -> status.xEntityId("42").eTag("cryptic").cacheControl("private"))
                .build();

        ETaggedReadResponse<ResourceGetResponse> actual = ETaggedReadResponse.from(response);

        assertThat(actual.xEntityId(), is("42"));
        assertThat(actual.eTag(), is("cryptic"));
        assertThat(actual.cacheControl(), is("private"));
    }

    @Test
    public void givenBuildedFromResponse_andStatus200__whenChangingProperties__thenPropertiesAreChanged() throws Exception {
        ResourceGetResponse response = ResourceGetResponse.builder()
                .status200(status -> status.xEntityId("42").eTag("cryptic").cacheControl("private"))
                .build();

        ETaggedReadResponse<ResourceGetResponse> actual = ETaggedReadResponse.from(response)
                .xEntityId("changed id")
                .eTag("changed etag")
                .cacheControl("changed cc")
                ;

        assertThat(actual.xEntityId(), is("changed id"));
        assertThat(actual.eTag(), is("changed etag"));
        assertThat(actual.cacheControl(), is("changed cc"));
    }

    @Test
    public void givenBuildedFromResponse_andStatus200_andChangedProperty__whenBuildingResponse__thenResponse() throws Exception {
        ResourceGetResponse response = ResourceGetResponse.builder()
                .status200(status -> status.xEntityId("42").eTag("cryptic").cacheControl("private"))
                .build();

        ResourceGetResponse actual = ETaggedReadResponse.from(response)
                .cacheControl("max-age=31536000")
                .response()
                ;

        assertThat(actual, is(response.withStatus200(response.status200().withCacheControl("max-age=31536000"))));
    }

    @Test
    public void givenCreated__whenAs412__thenNoPropertiesSet() throws Exception {
        ETaggedChangeResponse<ResourcePutResponse> actual = ETaggedChangeResponse.create412(ResourcePutResponse.class, "error");

        assertThat(actual.xEntityId(), is(nullValue()));
        assertThat(actual.eTag(), is(nullValue()));
        assertThat(actual.cacheControl(), is(nullValue()));

    }

    @Test
    public void givenCreated_andAs412__whenBuildingResponse__thenStatus412() throws Exception {
        ResourcePutResponse actual = ETaggedChangeResponse.<ResourcePutResponse>create412(ResourcePutResponse.class, "error").response();

        assertThat(actual, is(ResourcePutResponse.builder().status412(Status412.builder().errorToken("error").build()).build()));
    }



}