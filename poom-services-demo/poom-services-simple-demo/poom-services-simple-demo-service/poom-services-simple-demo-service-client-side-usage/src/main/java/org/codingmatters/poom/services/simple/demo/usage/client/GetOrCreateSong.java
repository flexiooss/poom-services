package org.codingmatters.poom.services.simple.demo.usage.client;

import org.codingmatters.poom.apis.simple.demo.api.*;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.apis.simple.demo.client.SimpleDemoClient;

import java.io.IOException;

public class GetOrCreateSong {

    private final SimpleDemoClient client;

    public GetOrCreateSong(SimpleDemoClient client) {
        this.client = client;
    }

    public Song get(String title, String author) throws IOException {
        SongListGetResponse existsResponse = this.client.songList().get(SongListGetRequest.builder()
                .filter(String.format("title == '%s' && author == '%s'", title, author))
                .build());
        if(existsResponse.opt().status200().isPresent() || existsResponse.opt().status206().isPresent()) {
            ValueList<Song> songs = existsResponse.opt().status200().payload().orElseGet(() -> existsResponse.status206().payload());
            if(songs.size() > 0) return songs.get(0);
        } else {
            throw new IOException("failed querying API, response was : " + existsResponse);
        }

        SongListPostResponse createResponse = this.client.songList().post(SongListPostRequest.builder().payload(
                Song.builder().title(title).author(author).build()
        ).build());

        return createResponse.status201().payload();
    }
}
