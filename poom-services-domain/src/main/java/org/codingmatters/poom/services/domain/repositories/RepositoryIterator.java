package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.poom.services.domain.repositories.impl.DefaultRepositoryIterator;
import org.codingmatters.poom.services.domain.entities.Entity;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface RepositoryIterator<V, Q> extends Iterator<Entity<V>> {

    static <V, Q> RepositoryIterator<V, Q> all(EntityLister<V, Q> lister, int pageSize) {
        return new DefaultRepositoryIterator<>(lister, pageSize);
    }

    static <V, Q> Stream<Entity<V>> allStreamed(EntityLister<V, Q> lister, int pageSize) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(all(lister, pageSize), 0), false);
    }

    static <V, Q> RepositoryIterator<V, Q> search(EntityLister<V, Q> lister, Q searched, int pageSize) {
        return new DefaultRepositoryIterator.SearchRepositoryIterator<>(lister, searched, pageSize);
    }

    static <V, Q> Stream<Entity<V>> searchStreamed(EntityLister<V, Q> lister, Q searched, int pageSize) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(search(lister, searched, pageSize), 0), false);
    }
}
