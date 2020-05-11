package org.codingmatters.poom.generic.resource.handlers;

import org.codingmatters.poom.api.generic.resource.api.GenericResourceHandlers;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.value.objects.values.ObjectValue;

public class GenericResourceHandlersBuilder extends GenericResourceHandlers.Builder {
    public GenericResourceHandlersBuilder(GenericResourceAdapter.Provider<ObjectValue> adapterProvider) {
        this.pagedCollectionPostHandler(new CreateEntity(adapterProvider));
        this.entityPutHandler(new ReplaceEntity(adapterProvider));
        this.entityPatchHandler(new UpdateEntity(adapterProvider));
        this.entityGetHandler(new GetEntity(adapterProvider));
        this.entityDeleteHandler(new DeleteEntity(adapterProvider));
        this.pagedCollectionGetHandler(new BrowseEntities(adapterProvider));
    }
}
