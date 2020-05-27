package org.codingmatters.poom.services.simple.demo.usage.server.handlers;

import org.codingmatters.poom.apis.simple.demo.api.SongListGetRequest;
import org.codingmatters.poom.apis.simple.demo.api.SongListGetResponse;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status200;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status206;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status416;
import org.codingmatters.poom.apis.simple.demo.api.songlistgetresponse.Status500;
import org.codingmatters.poom.apis.simple.demo.api.types.Song;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.util.Optional;
import java.util.function.Function;

public class SongBrowse implements Function<SongListGetRequest, SongListGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(SongBrowse.class);

    private final Repository<Song, PropertyQuery> repository;

    public SongBrowse(Repository<Song, PropertyQuery> repository) {
        this.repository = repository;
    }

    @Override
    public SongListGetResponse apply(SongListGetRequest request) {
        Rfc7233Pager.Page<Song> page;
        try {
            page = Rfc7233Pager
                    .forRequestedRange(request.range())
                    .unit("Song")
                    /*
                    here max size is ridiculously low to have the demo paging, a real good default would be 500
                     */
                    .maxPageSize(5)
                    .pager(this.repository)
                    .page(this.queryFrom(request));
        } catch (RepositoryException e) {
            return SongListGetResponse.builder().status500(Status500.builder().payload(ObjectValue.builder()
                    .property("error-token", PropertyValue.builder()
                            .stringValue(log.tokenized().error("error accessing repository", e)).build()
                    )
                    .build()).build()).build();
        }

        if(! page.isValid()) {
            return SongListGetResponse.builder().status416(Status416.builder()
                    .acceptRange(page.acceptRange())
                    .contentRange(page.contentRange())
                    .payload(ObjectValue.builder()
                            .property("error-token", PropertyValue.builder()
                                    .stringValue(log.tokenized().info("invalid paged request, {} : {}", page.validationMessage(), request)).build()
                            )
                            .property("error", PropertyValue.builder()
                                    .stringValue(page.validationMessage()).build()
                            )
                            .build())
                    .build()).build();
        }
        if(page.isPartial()) {
            return SongListGetResponse.builder().status206(Status206.builder()
                .acceptRange(page.acceptRange())
                .contentRange(page.contentRange())
                .payload(page.list().valueList())
                .build()).build();
        }
        return SongListGetResponse.builder().status200(Status200.builder()
                .acceptRange(page.acceptRange())
                .contentRange(page.contentRange())
                .payload(page.list().valueList())
                .build()).build();
    }

    private Optional<PropertyQuery> queryFrom(SongListGetRequest request) {
        if(request.opt().filter().isPresent() || request.opt().orderBy().isPresent()) {
            return Optional.of(PropertyQuery.builder()
                    .filter(request.filter())
                    .sort(request.orderBy())
                    .build());
        } else {
            return Optional.empty();
        }
    }
}
