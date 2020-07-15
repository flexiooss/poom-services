package org.codingmatters.poom.paged.collection.generation.generators.raml;

import org.codingmatters.poom.paged.collection.generation.spec.Action;
import org.codingmatters.poom.paged.collection.generation.spec.PagedCollectionDescriptor;
import org.codingmatters.poom.paged.collection.generation.spec.pagedcollectiondescriptor.Types;
import org.codingmatters.rest.api.generator.exception.RamlSpecException;
import org.codingmatters.rest.api.generator.handlers.HandlersHelper;
import org.codingmatters.rest.api.generator.utils.BodyTypeResolver;
import org.codingmatters.value.objects.generation.Naming;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.bodies.Response;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PagedCollectionDescriptorFromRamlParser {
    private final RamlModelResult ramlModel;
    private final Naming naming;
    private final HandlersHelper handlersHelper;
    private final String typesPackage;

    public PagedCollectionDescriptorFromRamlParser(RamlModelResult ramlModel, String apiPackage, String typesPackage) {
        this.ramlModel = ramlModel;
        this.typesPackage = typesPackage;
        this.naming = new Naming();
        this.handlersHelper = new HandlersHelper(apiPackage, this.naming);
    }

    public PagedCollectionDescriptor[] parse() throws RamlSpecException {
        List<PagedCollectionDescriptor> results = new LinkedList<>();
        for (Resource collection : this.collectPageCollections(this.ramlModel.getApiV10().resources())) {
            results.add(this.descriptor(collection));
        }
        return results.toArray(new PagedCollectionDescriptor[results.size()]);
    }

    private List<Resource> collectPageCollections(List<Resource> resources) {
        List<Resource> collections = new LinkedList<>();
        for (Resource resource : resources) {
            if(this.isPagedCollection(resource)) {
                collections.add(resource);
            }
            collections.addAll(this.collectPageCollections(resource.resources()));
        }
        return collections;
    }

    private boolean isPagedCollection(Resource resource) {
        return resource.annotations().stream().anyMatch(
                annotationRef -> "paged-collection".equals(annotationRef.annotation().name())
        );
    }

    private PagedCollectionDescriptor descriptor(Resource collection) throws RamlSpecException {
        Optional<Resource> entity = this.entityResource(collection);
        PagedCollectionDescriptor.Builder builder = PagedCollectionDescriptor.builder()
                .name(this.naming.type(collection.displayName().value()))
                .entityIdParam(this.entityIdParam(collection))
                .types(this.types(collection))
                .browse(this.resourceAction(Optional.of(collection), "get"))
                .create(this.resourceAction(Optional.of(collection), "post"))
                .retrieve(this.resourceAction(entity, "get"))
                .replace(this.resourceAction(entity, "put"))
                .update(this.resourceAction(entity, "patch"))
                .delete(this.resourceAction(entity, "delete"))
                ;

        return builder.build();
    }

    private Action resourceAction(Optional<Resource> resource, String method) {
        if(! resource.isPresent()) return null;
        if(resource.get().methods().stream().anyMatch(m -> m.method().equals(method))) {
            return Action.builder()
                    .requestValueObject(this.handlersHelper.handlerFunctionType(resource.get().displayName().value(), method).typeArguments.get(0).toString())
                    .responseValueObject(this.handlersHelper.handlerFunctionType(resource.get().displayName().value(), method).typeArguments.get(1).toString())
                    .build();
        } else {
            return null;
        }
    }

    private String entityIdParam(Resource collection) throws RamlSpecException {
        Optional<Resource> entityResource = this.entityResource(collection);
        if(entityResource.isPresent()) {
            return entityResource.get().uriParameters().get(entityResource.get().uriParameters().size() - 1).name();
        } else {
            return null;
        }
    }

    private Optional<Resource> entityResource(Resource collection) {
        return collection.resources().stream().filter(resource -> this.isPagedCollectionEntity(resource)).findFirst();
    }

    private Types types(Resource collection) throws RamlSpecException {
        Types.Builder builder = Types.builder()
                .error(this.typesPackage + ".Error")
                .message(this.typesPackage + ".Message");

        Optional<Method> method = this.method(collection, "post");
        if(method.isPresent()) {
            builder.create(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
        }

        Optional<Resource> entityResource = this.entityResource(collection);
        if(entityResource.isPresent()) {
            method = this.method(entityResource.get(), "put");
            if (method.isPresent()) {
                builder.replace(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
            }
            method = this.method(entityResource.get(), "patch");
            if (method.isPresent()) {
                builder.update(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
            }
        }

        builder.entity(this.resolveEntity(collection));

        return builder.build();
    }

    private Optional<Method> method(Resource collection, String methodeName) {
        return collection.methods().stream().filter(m -> m.method().equals(methodeName)).findFirst();
    }

    private String resolveEntity(Resource collection) throws RamlSpecException {
        Optional<Method> method = this.method(collection, "get");
        if(! method.isPresent()) {
            method = this.method(collection, "post");
        }
        if(! method.isPresent()) {
            Optional<Resource> entityResource = this.entityResource(collection);
            if(entityResource.isPresent()) {
                method = this.method(entityResource.get(), "get");
                if(! method.isPresent()) {
                    method = this.method(entityResource.get(), "put");
                }
                if(! method.isPresent()) {
                    method = this.method(entityResource.get(), "patch");
                }
            }
        }

        if(method.isPresent()) {
            return new BodyTypeResolver(this.status200Response(method.get()).body().get(0), this.typesPackage).resolve().build().typeRef();
        } else {
            return null;
        }
    }

    private Response status200Response(Method method) {
        for (Response response : method.responses()) {
            if(response.code().value().matches("20\\d+")) {
                return response;
            }
        }
        return null;
    }

    private boolean isPagedCollectionEntity(Resource resource) {
        return resource.annotations().stream().anyMatch(
                annotationRef -> "paged-collection-entity".equals(annotationRef.annotation().name())
        );
    }
}
