package org.codingmatters.poom.paged.collection.generation.generators.test;

import org.codingmatters.poom.paged.collection.generation.spec.Action;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.paged.collection.generation.spec.pagedcollectiondescriptor.Types;
import org.generated.api.*;
import org.generated.api.types.*;
import org.generated.api.types.Error;

public interface TestData {
    PagedCollectionDescriptor FULL_COLLECTION = PagedCollectionDescriptor.builder()
                .name("NoParams").entityIdParam("entity-id")
                .types(Types.builder()
                        .entity(org.generated.api.types.Entity.class.getName())
                        .create(Create.class.getName())
                        .replace(Replace.class.getName())
                        .update(Update.class.getName())
                        .error(Error.class.getName())
                        .message(Message.class.getName())
                        .build())
                .browse(Action.builder()
                        .name("ignored, collection name accounted instead")
                        .requestValueObject(NoParamsGetRequest.class.getName())
                        .responseValueObject(NoParamsGetResponse.class.getName())
                        .build())
                .create(Action.builder()
                        .name("ignored, collection name accounted instead")
                        .requestValueObject(NoParamsPostRequest.class.getName())
                        .responseValueObject(NoParamsPostResponse.class.getName())
                        .build())
                .retrieve(Action.builder()
                        .name("ignored, collection name accounted instead")
                        .requestValueObject(NoParamsElementGetRequest.class.getName())
                        .responseValueObject(NoParamsElementGetResponse.class.getName())
                        .build())
                .build();
}
