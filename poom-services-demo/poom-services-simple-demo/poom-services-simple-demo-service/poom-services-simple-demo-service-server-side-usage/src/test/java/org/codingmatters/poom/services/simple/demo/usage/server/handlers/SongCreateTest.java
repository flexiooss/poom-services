package org.codingmatters.poom.services.simple.demo.usage.server.handlers;

import org.codingmatters.poom.apis.simple.demo.api.SongListPostRequest;
import org.codingmatters.poom.apis.simple.demo.api.SongListPostResponse;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


public class SongCreateTest {

    private final Repository<Song, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Song.class);
    private final SongCreate handler = new SongCreate(this.repository);

    @Test
    public void givenRepositoryIsEmpty_andPostingRequest__whenTitleAndAuthorSettedInRequestPayload__thenSongIsCreatedInRepository_andXEntityIdIsTheIdOfTheCreatedSong_andTheCreatedSongHasTheTitleAndTheAuthorSetted_andTheCreatedSongIsReturnedAsPayload() throws Exception {
        SongListPostResponse response = this.handler.apply(SongListPostRequest.builder()
                .payload(Song.builder().title("Once").author("Eddie Vedder, Stone Gossard").build())
                .build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(this.repository.all(0, 0).total(), is(1L));

        assertThat(response.status201().xEntityId(), is(notNullValue()));
        assertThat(
                this.repository.retrieve(response.status201().xEntityId()).value(),
                is(Song.builder()
                        .id(response.status201().xEntityId())
                        .title("Once")
                        .author("Eddie Vedder, Stone Gossard")
                        .build())
        );
        assertThat(response.status201().payload(), is(this.repository.retrieve(response.status201().xEntityId()).value()));
    }

    @Test
    public void givenRepositoryIsEmpty_andPostingRequest__whenTitleNotSet__then400_andRepositoryIsStillEmpty() throws Exception {
        SongListPostResponse response = this.handler.apply(SongListPostRequest.builder()
                .payload(Song.builder().author("Eddie Vedder, Stone Gossard").build())
                .build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenRepositoryIsEmpty_andPostingRequest__whenAuthorNotSet__then400_andRepositoryIsStillEmpty() throws Exception {
        SongListPostResponse response = this.handler.apply(SongListPostRequest.builder()
                .payload(Song.builder().title("Once").build())
                .build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, got " + response));

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }
}