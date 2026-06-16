package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.apis.demo.api.types.Movie;
import org.codingmatters.poom.generic.resource.domain.PagedCollectionAdapter;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poom.services.domain.entities.PagedEntityList;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MovieOrderedLister implements PagedCollectionAdapter.OrderedLister<Movie> {

    private static final String ASC_SORT = "facts.releaseDate asc, id asc";
    private static final String DESC_SORT = "facts.releaseDate desc, id desc";

    private final Repository<Movie, PropertyQuery> repository;

    public MovieOrderedLister(Repository<Movie, PropertyQuery> repository) {
        this.repository = repository;
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Movie> initLatest(Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
        List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
        long total = all.size();
        if (total == 0) return emptyPage();
        long from = Math.max(0, total - end - 1);
        long to = total - start - 1;
        if (from > to) return emptyPage();
        List<Entity<Movie>> page = all.subList((int) from, (int) to + 1);
        return orderedPage(
            toPagedList(page, from, to, total),
            Optional.of(cursorFor(page.get(page.size() - 1))),
            Optional.of(cursorFor(page.get(0)))
        );
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Movie> initOldest(Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
        List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
        long total = all.size();
        if (total == 0) return emptyPage();
        long to = Math.min(end, total - 1);
        if (start > to) return emptyPage();
        List<Entity<Movie>> page = all.subList((int) start, (int) to + 1);
        return orderedPage(
            toPagedList(page, start, to, total),
            Optional.of(cursorFor(page.get(page.size() - 1))),
            Optional.empty()
        );
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Movie> since(String since, Optional<String> optBefore, Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
        String[] sinceParts = parseCursor(since);
        List<Entity<Movie>> all = fetchAll(ASC_SORT, userQuery);
        List<Entity<Movie>> filtered = all.stream()
            .filter(e -> isAfterCursor(e.value(), sinceParts))
            .filter(e -> optBefore.map(b -> isBeforeCursor(e.value(), parseCursor(b))).orElse(true))
            .collect(Collectors.toList());
        long total = filtered.size();
        if (total == 0 || start >= total) return orderedPage(emptyPagedList(), Optional.empty(), optBefore);
        long to = Math.min(end, total - 1);
        List<Entity<Movie>> page = filtered.subList((int) start, (int) to + 1);
        return orderedPage(
            toPagedList(page, start, to, total),
            Optional.of(cursorFor(page.get(page.size() - 1))),
            optBefore
        );
    }

    @Override
    public PagedCollectionAdapter.OrderedPage<Movie> before(String before, Optional<PropertyQuery> userQuery, long start, long end) throws RepositoryException {
        String[] beforeParts = parseCursor(before);
        List<Entity<Movie>> all = fetchAll(DESC_SORT, userQuery);
        List<Entity<Movie>> filtered = all.stream()
            .filter(e -> isBeforeCursor(e.value(), beforeParts))
            .collect(Collectors.toList());
        long total = filtered.size();
        if (total == 0 || start >= total) return emptyPage();
        long to = Math.min(end, total - 1);
        List<Entity<Movie>> page = filtered.subList((int) start, (int) to + 1);
        return orderedPage(
            toPagedList(page, start, to, total),
            Optional.empty(),
            Optional.of(cursorFor(page.get(page.size() - 1)))
        );
    }

    private List<Entity<Movie>> fetchAll(String sort, Optional<PropertyQuery> userQuery) throws RepositoryException {
        PropertyQuery.Builder builder = PropertyQuery.builder().sort(sort);
        userQuery.flatMap(q -> q.opt().filter()).filter(f -> !f.isEmpty()).ifPresent(builder::filter);
        return new ArrayList<>(this.repository.search(builder.build(), 0, 9999));
    }

    private String cursorFor(Entity<Movie> entity) {
        Movie m = entity.value();
        return m.facts().releaseDate().toString() + "|" + m.id();
    }

    private String[] parseCursor(String cursor) {
        return cursor.split("\\|", 2);
    }

    private boolean isAfterCursor(Movie movie, String[] cursorParts) {
        LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
        int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
        if (dateCmp != 0) return dateCmp > 0;
        return movie.id().compareTo(cursorParts[1]) > 0;
    }

    private boolean isBeforeCursor(Movie movie, String[] cursorParts) {
        LocalDate cursorDate = LocalDate.parse(cursorParts[0]);
        int dateCmp = movie.facts().releaseDate().compareTo(cursorDate);
        if (dateCmp != 0) return dateCmp < 0;
        return movie.id().compareTo(cursorParts[1]) < 0;
    }

    private PagedEntityList<Movie> toPagedList(List<Entity<Movie>> items, long startIndex, long endIndex, long total) {
        return new PagedEntityList.DefaultPagedEntityList<>(startIndex, endIndex, total, items);
    }

    private PagedEntityList<Movie> emptyPagedList() {
        return new PagedEntityList.DefaultPagedEntityList<>(0, 0, 0, Collections.emptyList());
    }

    private PagedCollectionAdapter.OrderedPage<Movie> emptyPage() {
        return orderedPage(emptyPagedList(), Optional.empty(), Optional.empty());
    }

    private PagedCollectionAdapter.OrderedPage<Movie> orderedPage(PagedEntityList<Movie> list, Optional<String> since, Optional<String> before) {
        return new PagedCollectionAdapter.OrderedPage<Movie>() {
            @Override public PagedEntityList<Movie> list() { return list; }
            @Override public Optional<String> since() { return since; }
            @Override public Optional<String> before() { return before; }
        };
    }
}
