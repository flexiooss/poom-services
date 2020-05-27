package org.codingmatters.poom.services.simple.demo.usage.client;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.apis.simple.demo.api.*;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.apis.simple.demo.client.SimpleDemoClient;
import org.codingmatters.poom.apis.simple.demo.client.SimpleDemoRequesterClient;
import org.codingmatters.rest.api.client.RequesterFactory;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

import java.io.IOException;

public class ClientUsage {
    public static void main(String[] args) {
        String baseUrl = args[0];

        JsonFactory jsonFactory = new JsonFactory();

        /*
        In order to use an API client, you need a requester factory.
        For now on, the only requester factory implementation is an OK HTTP implementation, but, more to come eventually.
         */
        RequesterFactory requesterFactory = new OkHttpRequesterFactory(
                OkHttpClientWrapper.build(),
                () -> baseUrl
        );

        /*
        Now that we have a requester factory, we can create a requester client for our API.
        The client client is an interface named: <title of the api>Client.
        The concrete class used here is named : <title of the api>RequesterClient.
         */
        SimpleDemoClient client = new SimpleDemoRequesterClient(requesterFactory, jsonFactory, () -> baseUrl);
        /*
        You might ask, why a lambda () -> baseUrl to set the base URL. As it seems to be overkill here, it enables to
        implement different strategies for geting the base URL. One that we use if to get a list of URLs for the instances
        of an APIs and load ballance the queries.

        You might still ask, why are we passing that twice... well that's the result of a bad design choice made early
        in the development that we don't manage to get fixed...
         */
        try {
            /*
            Now that we have a client, we can issue some queries :
             */
            SongListGetResponse songListResponse = client.songList().get(SongListGetRequest.builder()
                    .filter("my funny valentine") // here we set a query parameter
                    .build()
            );

            /*
            Note the usage of optionals here
             */
            if(songListResponse.opt().status200().isPresent() || songListResponse.opt().status206().isPresent()) {
                ValueList<Song> songs = songListResponse.opt().status200().payload().orElseGet(() -> songListResponse.status206().payload());
                System.out.println("SONGS :: " + songs);

                ASongGetResponse songResponse = client.songList().aSong().get(ASongGetRequest.builder()
                        .songId(songs.get(0).id()) // here we set an uri parameter
                        .build());

                if(songResponse.opt().status200().isPresent()) {
                    System.out.println("SONG :: " + songResponse.status200().payload());
                } else {
                    System.err.println("failed getting a song, response was " + songResponse);
                }
            } else {
                System.err.println("failed getting song list, response was " + songListResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
