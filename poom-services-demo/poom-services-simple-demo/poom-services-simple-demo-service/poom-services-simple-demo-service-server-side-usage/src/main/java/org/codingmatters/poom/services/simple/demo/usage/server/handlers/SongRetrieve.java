package org.codingmatters.poom.services.simple.demo.usage.server.handlers;

import org.codingmatters.poom.apis.simple.demo.api.ASongGetRequest;
import org.codingmatters.poom.apis.simple.demo.api.ASongGetResponse;
import org.codingmatters.poom.apis.simple.demo.api.asonggetresponse.Status200;
import org.codingmatters.poom.apis.simple.demo.api.asonggetresponse.Status400;
import org.codingmatters.poom.apis.simple.demo.api.asonggetresponse.Status500;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.function.Function;

public class SongRetrieve implements Function<ASongGetRequest, ASongGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(SongRetrieve.class);

    private final Repository<Song, PropertyQuery> repository;

    public SongRetrieve(Repository<Song, PropertyQuery> repository) {
        this.repository = repository;
    }

    @Override
    public ASongGetResponse apply(ASongGetRequest request) {
        if(! request.opt().songId().isPresent()) {
            return ASongGetResponse.builder().status400(Status400.builder().payload(
                    ObjectValue.builder()
                            .property("error-token", PropertyValue.builder()
                                    .stringValue(log.tokenized().info("invalid request : {}", request)).build()
                            )
                            .property("error", PropertyValue.builder()
                                    .stringValue("must provide a song-id").build()
                            )
                            .build()
            ).build()).build();
        }

        Entity<Song> entity;
        try {
            entity = this.repository.retrieve(request.songId());
        } catch (RepositoryException e) {
            return ASongGetResponse.builder().status500(Status500.builder().payload(ObjectValue.builder()
                    .property("error-token", PropertyValue.builder()
                            .stringValue(log.tokenized().error("error accessing repository", e)).build()
                    )
                    .build()).build()).build();
        }
        return ASongGetResponse.builder().status200(Status200.builder()
                .payload(entity.value())
                .build()).build();
    }
}
