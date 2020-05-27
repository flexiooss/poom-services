package org.codingmatters.poom.services.simple.demo.usage.server.data;

import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;

public class SongDemoData {
    public void fill(Repository<Song, PropertyQuery> repository) {
        try {
            this.createSong(repository, "Once", "Eddie Vedder, Stone Gossard");
            this.createSong(repository, "Even Flow", "Eddie Vedder, Stone Gossard");
            this.createSong(repository, "Alive", "Eddie Vedder, Stone Gossard");
            this.createSong(repository, "Why Go", "Eddie Vedder, Jeff Ament");
            this.createSong(repository, "Black", "Eddie Vedder, Stone Gossard");
            this.createSong(repository, "Jeremy", "Eddie Vedder, Jeff Ament");
            this.createSong(repository, "Oceans", "Eddie Vedder, Stone Gossard, Jeff Ament");
            this.createSong(repository, "Porch", "Eddie Vedder");
            this.createSong(repository, "Garden", "Eddie Vedder, Stone Gossard, Jeff Ament");
            this.createSong(repository, "Deep", "Eddie Vedder, Stone Gossard, Jeff Ament");
            this.createSong(repository, "Release", "Eddie Vedder, Stone Gossard, Jeff Ament, Mike McCready, Dave Krusen");
        } catch (RepositoryException e) {
            throw new RuntimeException("failed creating demo data", e);
        }
    }

    private void createSong(Repository<Song, PropertyQuery> repository, String title, String author) throws RepositoryException {
        Entity<Song> entity = repository.create(Song.builder().title(title).author(author).build());
        repository.update(entity, entity.value().withId(entity.id()));
    }
}
