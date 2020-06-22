package org.codingmatters.poom.services.simple.demo.usage.client;

import org.codingmatters.poom.apis.simple.demo.api.*;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status200;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status206;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status400;
import org.codingmatters.poom.apis.simple.demo.api.songlistpostresponse.Status201;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.apis.simple.demo.client.SimpleDemoClient;
import org.codingmatters.poom.apis.simple.demo.client.SimpleDemoHandlersClient;
import org.codingmatters.poom.handler.HandlerResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


public class GetOrCreateSongTest {

    private static final Song LOOKED_UP = Song.builder().id("42").title("looked up title").author("looked up author").build();
    private static final Song CREATED = Song.builder().id("12").title("created title").author("created author").build();

    public static final SongListGetResponse COMPLETE_LIST_WITH_FOUND_SONG = SongListGetResponse.builder().status200(Status200.builder()
            .payload(LOOKED_UP)
            .build()).build();
    public static final SongListGetResponse PARTIAL_LIST_WITH_FOUND_SONG = SongListGetResponse.builder().status206(Status206.builder()
            .payload(LOOKED_UP)
            .build()).build();
    public static final SongListGetResponse EMPTY_LIST = SongListGetResponse.builder().status200(Status200.builder()
            .payload(new Song[0])
            .build()).build();
    public static final SongListGetResponse ERROR_LIST = SongListGetResponse.builder().status400(Status400.builder().build()).build();

    @Rule
    public HandlerResource<SongListGetRequest, SongListGetResponse> retrieveHandler = new HandlerResource<SongListGetRequest, SongListGetResponse>() {
        @Override
        protected SongListGetResponse defaultResponse(SongListGetRequest request) {
            return EMPTY_LIST;
        }
    };
    @Rule
    public HandlerResource<SongListPostRequest, SongListPostResponse> createHandler = new HandlerResource<SongListPostRequest, SongListPostResponse>() {
        @Override
        protected SongListPostResponse defaultResponse(SongListPostRequest request) {
            return SongListPostResponse.builder().status201(Status201.builder()
                    .xEntityId(CREATED.id())
                    .payload(CREATED)
                    .build()).build();
        }
    };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SimpleDemoClient client = new SimpleDemoHandlersClient(
            new SimpleDemoHandlers.Builder()
                    .songListGetHandler(this.retrieveHandler)
                    .songListPostHandler(this.createHandler)
                    .build(),
            Executors.newSingleThreadExecutor()
    );

    @Test
    public void givenListQueryReturnsCompleteNonEmptyList__whenSongFound__thenListIsQueriesWithFilter_andCreateIsNotQueried_andExistingSongIsReturned() throws Exception {
        this.retrieveHandler.nextResponse(request -> COMPLETE_LIST_WITH_FOUND_SONG);

        Song actual = new GetOrCreateSong(this.client).get("a title", "an author");

        assertThat(this.retrieveHandler.lastRequest(), is(SongListGetRequest.builder().filter("title == 'a title' && author == 'an author'").build()));
        assertThat(this.createHandler.lastRequest(), is(nullValue()));
        assertThat(actual, is(LOOKED_UP));
    }

    @Test
    public void givenListQueryReturnsPartialList__whenSongFound__thenListIsQueriesWithFilter_andCreateIsNotQueried_andExistingSongIsReturned() throws Exception {
        this.retrieveHandler.nextResponse(request -> PARTIAL_LIST_WITH_FOUND_SONG);

        Song actual = new GetOrCreateSong(this.client).get("a title", "an author");

        assertThat(this.retrieveHandler.lastRequest(), is(SongListGetRequest.builder().filter("title == 'a title' && author == 'an author'").build()));
        assertThat(this.createHandler.lastRequest(), is(nullValue()));
        assertThat(actual, is(LOOKED_UP));
    }

    @Test
    public void givenListQueryReturnsEmptyList__whenSongFound__thenListIsQueriesWithFilter_andCreateIsQueried_andCreatedSongIsReturned() throws Exception {
        this.retrieveHandler.nextResponse(request -> EMPTY_LIST);

        Song actual = new GetOrCreateSong(this.client).get("a title", "an author");

        assertThat(this.retrieveHandler.lastRequest(), is(SongListGetRequest.builder().filter("title == 'a title' && author == 'an author'").build()));
        assertThat(this.createHandler.lastRequest(), is(SongListPostRequest.builder().payload(Song.builder().title("a title").author("an author").build()).build()));
        assertThat(actual, is(CREATED));
    }

    @Test
    public void givenListQueryReturnsAnError__whenSongFound__thenListIsQueriesWithFilter_andCreateIsQueried_andCreatedSongIsReturned() throws Exception {
        this.retrieveHandler.nextResponse(request -> ERROR_LIST);

        thrown.expect(IOException.class);
        new GetOrCreateSong(this.client).get("a title", "an author");
    }
}