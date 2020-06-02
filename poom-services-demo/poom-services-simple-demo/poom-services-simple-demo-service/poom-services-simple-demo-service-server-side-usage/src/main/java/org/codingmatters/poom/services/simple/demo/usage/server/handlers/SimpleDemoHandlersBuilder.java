package org.codingmatters.poom.services.simple.demo.usage.server.handlers;

import org.codingmatters.poom.apis.simple.demo.api.SimpleDemoHandlers;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;

public class SimpleDemoHandlersBuilder extends SimpleDemoHandlers.Builder {
    public SimpleDemoHandlersBuilder(Repository<Song, PropertyQuery> repository) {
        this.aSongGetHandler(new SongRetrieve(repository));
        this.songListPostHandler(new SongCreate(repository));
        this.songListGetHandler(new SongBrowse(repository));
    }
}
