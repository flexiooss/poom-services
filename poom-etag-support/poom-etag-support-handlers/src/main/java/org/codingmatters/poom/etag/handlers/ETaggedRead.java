package org.codingmatters.poom.etag.handlers;

import org.codingmatters.poom.etag.api.ETaggedReadRequest;
import org.codingmatters.poom.etag.handlers.exception.UnETaggable;
import org.codingmatters.poom.etag.handlers.responses.ETaggedReadResponse;
import org.codingmatters.poom.etag.storage.Etag;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.function.Function;

public class ETaggedRead<Request extends ETaggedReadRequest, Response> implements Function<Request, Response> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ETaggedRead.class);

    private final Repository<Etag, PropertyQuery> etagRepository;
    private final String defaultCacheControl;
    private final Function<Request, Response> delegate;
    private Class responseType;

    public ETaggedRead(Repository<Etag, PropertyQuery> etagRepository, String defaultCacheControl, Function<Request, Response> delegate, Class<? extends Response> responseType) {
        this.etagRepository = etagRepository;
        this.defaultCacheControl = defaultCacheControl;
        this.delegate = delegate;
        this.responseType = responseType;
    }

    @Override
    public Response apply(Request request) {
        if(request.ifNoneMatch() != null && ! request.ifNoneMatch().isEmpty()) {
            try {
                PagedEntityList<Etag> stored = this.etagRepository.search(PropertyQuery.builder()
                        .filter("etag == '%s'", request.ifNoneMatch())
                        .build(), 0, 0);
                if(stored.total() != 0) {
                    try {
                        return (Response) ETaggedReadResponse.create304(
                                this.responseType,
                                stored.get(0).value().id(),
                                stored.get(0).value().etag(),
                                stored.get(0).value().cacheControl()
                                ).response();
                    } catch (UnETaggable e) {
                        return (Response) ETaggedReadResponse.create500(this.responseType, log.tokenized().error("failed creating 304 response"));
                    }
                } else {
                    return this.delegateAndStoreEtag(request);
                }
            } catch (RepositoryException e) {
                return (Response) ETaggedReadResponse.create500(this.responseType, log.tokenized().error("failed getting etag", e));
            }
        } else {
            return this.delegateAndStoreEtag(request);
        }
    }

    private Response delegateAndStoreEtag(Request request) {
        Response response = this.delegate.apply(request);
        try {
            ETaggedReadResponse<Response> etagged = ETaggedReadResponse.from(response);
            if(this.etagRepository.retrieve(etagged.xEntityId()) == null) {
                this.etagRepository.createWithId(etagged.xEntityId(), Etag.builder()
                        .etag(etagged.eTag())
                        .id(etagged.xEntityId())
                        .cacheControl(etagged.cacheControl())
                        .created(UTC.now())
                        .build());
            }
        } catch (UnETaggable e) {
            return (Response) ETaggedReadResponse.create500(this.responseType, log.tokenized().error("failed creating etagged response from delegate response : " + response, e));
        } catch (RepositoryException e) {
            return (Response) ETaggedReadResponse.create500(this.responseType, log.tokenized().error("failed storing etag", e));
        }
        return response;
    }

}
