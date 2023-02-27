package org.codingmatters.poom.services.simple.demo.usage.server.handlers;

import org.codingmatters.poom.apis.simple.demo.api.SongListPostRequest;
import org.codingmatters.poom.apis.simple.demo.api.SongListPostResponse;
import org.codingmatters.poom.apis.simple.demo.api.songlistpostresponse.Status201;
import org.codingmatters.poom.apis.simple.demo.api.songlistpostresponse.Status400;
import org.codingmatters.poom.apis.simple.demo.api.songlistpostresponse.Status500;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.function.Function;

public class SongCreate implements Function<SongListPostRequest, SongListPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(SongCreate.class);

    private final Repository<Song, PropertyQuery> repository;

    public SongCreate(Repository<Song, PropertyQuery> repository) {
        this.repository = repository;
    }

    @Override
    public SongListPostResponse apply(SongListPostRequest request) {
        if(!(request.opt().payload().title().isPresent() && request.opt().payload().author().isPresent())) {
            return SongListPostResponse.builder().status400(Status400.builder().payload(ObjectValue.builder()
                    .property("error-token", PropertyValue.builder()
                            .stringValue(log.tokenized().info("invalid request : {}", request)).build()
                    )
                    .property("error", PropertyValue.builder()
                            .stringValue("must provide a a title and an author").build()
                    )
                    .build()).build()).build();
        }

        Entity<Song> entity;
        try {
            entity = this.repository.create(request.payload());
            entity = this.repository.update(entity, entity.value().withId(entity.id()));
            log.info("new song created : {}", entity);
        } catch (RepositoryException e) {
            return SongListPostResponse.builder().status500(Status500.builder().payload(ObjectValue.builder()
                    .property("error-token", PropertyValue.builder()
                            .stringValue(log.tokenized().error("error accessing repository", e)).build()
                    )
                    .build()).build()).build();
        }

        return SongListPostResponse.builder().status201(Status201.builder()
                .xEntityId(entity.id())
                .payload(entity.value())
                .build()).build();
    }
}
