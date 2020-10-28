package org.codingmatters.poom.etag.handlers;

import org.generated.api.ResourceGetResponse;
import org.generated.api.resourcegetresponse.Status412;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


public class ETaggedResponseTest {

    @Test
    public void givenBuildedFromResponse__whenStatus200__thenPropertiesFromResponse() throws Exception {
        ResourceGetResponse response = ResourceGetResponse.builder()
                .status200(status -> status.xEntityId("42").eTag("cryptic").cacheControl("private"))
                .build();

        ETaggedResponse<ResourceGetResponse> actual = ETaggedResponse.from(response);

        assertThat(actual.xEntityId(), is("42"));
        assertThat(actual.eTag(), is("cryptic"));
        assertThat(actual.cacheControl(), is("private"));
    }

    @Test
    public void givenBuildedFromResponse_andStatus200__whenChangingProperties__thenPropertiesAreChanged() throws Exception {
        ResourceGetResponse response = ResourceGetResponse.builder()
                .status200(status -> status.xEntityId("42").eTag("cryptic").cacheControl("private"))
                .build();

        ETaggedResponse<ResourceGetResponse> actual = ETaggedResponse.from(response)
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

        ResourceGetResponse actual = ETaggedResponse.from(response)
                .cacheControl("max-age=31536000")
                .response()
                ;

        assertThat(actual, is(response.withStatus200(response.status200().withCacheControl("max-age=31536000"))));
    }

    @Test
    public void givenCreated__whenAs304__thenPropertiesFromArgument() throws Exception {
        ETaggedResponse<ResourceGetResponse> actual = ETaggedResponse.create304(ResourceGetResponse.class, "42", "cryptic", "private");

        assertThat(actual.xEntityId(), is("42"));
        assertThat(actual.eTag(), is("cryptic"));
        assertThat(actual.cacheControl(), is("private"));
    }

    @Test
    public void givenCreated_andAs304__whenChangingProperties__thenPropertiesAreChanged() throws Exception {
        ETaggedResponse<ResourceGetResponse> actual = ETaggedResponse.create304(ResourceGetResponse.class, "42", "cryptic", "private")
                .xEntityId("changed id")
                .eTag("changed etag")
                .cacheControl("changed cc")
                ;

        assertThat(actual.xEntityId(), is("changed id"));
        assertThat(actual.eTag(), is("changed etag"));
        assertThat(actual.cacheControl(), is("changed cc"));
    }

    @Test
    public void givenCreated_andAs304_andChangedProperty__whenBuildingResponse__thenStatus304_andPropertiesChanged() throws Exception {
        ResourceGetResponse actual = ETaggedResponse.create304(ResourceGetResponse.class, "42", "cryptic", "private")
                .cacheControl("max-age=31536000")
                .response()
                ;

        assertThat(actual, is(ResourceGetResponse.builder()
                .status304(status -> status.xEntityId("42").eTag("cryptic").cacheControl("max-age=31536000"))
                .build()));
    }


    @Test
    public void givenCreated__whenAs402__thenNoPropertiesSet() throws Exception {
        ETaggedResponse<ResourceGetResponse> actual = ETaggedResponse.create412(ResourceGetResponse.class, "error");

        assertThat(actual.xEntityId(), is(nullValue()));
        assertThat(actual.eTag(), is(nullValue()));
        assertThat(actual.cacheControl(), is(nullValue()));

    }

    @Test
    public void givenCreated_andAs402__whenBuildingResponse__thenStatus402() throws Exception {
        ResourceGetResponse actual = ETaggedResponse.<ResourceGetResponse>create412(ResourceGetResponse.class, "error").response();

        assertThat(actual, is(ResourceGetResponse.builder().status412(Status412.builder().errorToken("error").build()).build()));
    }


}