package org.codingmatters.poom.services.simple.demo.usage.server;

import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.RepositoryIterator;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class SongFinder {
    public static final long PAGE_SIZE = 99L;
    private final Repository<Song, PropertyQuery> repo;

    public SongFinder(Repository<Song, PropertyQuery> repo) {
        this.repo = repo;
    }

    public Optional<Song> firstSong(String ofAuthor) throws RepositoryException {
        PagedEntityList<Song> page = this.repo.search(PropertyQuery.builder().filter(String.format("author == '%s'")).build(), 0, 0);
        if(page.total() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(page.get(0).value());
        }
    }

    public List<Song> allSongs(String ofAuthor) throws RepositoryException {
        List<Song> result = new LinkedList<>();
        long start = 0L;
        long end = start + PAGE_SIZE;
        PagedEntityList<Song> page = this.repo.search(PropertyQuery.builder().filter(String.format("author == '%s'")).build(), start, end);
        while(! page.isEmpty()) {
            result.addAll(page.valueList(songEntity -> songEntity.value()));
            start = end + 1;
            end = end + 99L;
            page = this.repo.search(PropertyQuery.builder().filter(String.format("author == '%s'")).build(), start, end);
        }
        return result;
    }

    public List<Song> enhancedAllSongs(String ofAuthor) throws RepositoryException {
        List<Song> results = new LinkedList<>();
        RepositoryIterator<Song, PropertyQuery> songIterator = RepositoryIterator.search(
                this.repo,
                PropertyQuery.builder().filter(String.format("author == '%s'")).build(),
                100);
        while(songIterator.hasNext()) {
            results.add(songIterator.next().value());
        }
        return results;
    }
}
