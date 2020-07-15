package org.codingmatters.poom.paged.collection.generation.generators.source;

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
        Resource entity = this.entityResource(collection);
        PagedCollectionDescriptor.Builder builder = PagedCollectionDescriptor.builder()
                .name(this.naming.type(collection.displayName().value()))
                .entityIdParam(this.entityIdParam(collection))
                .types(this.types(collection))
                .browse(this.resourceAction(collection, "get"))
                .create(this.resourceAction(collection, "post"))
                .retrieve(this.resourceAction(entity, "get"))
                .replace(this.resourceAction(entity, "put"))
                .update(this.resourceAction(entity, "patch"))
                .delete(this.resourceAction(entity, "delete"))
                ;

        return builder.build();
    }

    private Action resourceAction(Resource collection, String method) {
        return Action.builder()
                .requestValueObject(this.handlersHelper.handlerFunctionType(collection.displayName().value(), method).typeArguments.get(0).toString())
                .responseValueObject(this.handlersHelper.handlerFunctionType(collection.displayName().value(), method).typeArguments.get(1).toString())
                .build();
    }

    private String entityIdParam(Resource collection) throws RamlSpecException {
        Resource entityResource = this.entityResource(collection);
        return entityResource.uriParameters().get(entityResource.uriParameters().size() - 1).name();
    }

    private Resource entityResource(Resource collection) throws RamlSpecException {
        Optional<Resource> entityResource = collection.resources().stream().filter(resource -> this.isPagedCollectionEntity(resource)).findFirst();
        if(entityResource.isPresent()) {
            return entityResource.get();
        } else {
            throw new RamlSpecException("must have resource annotated with paged-collection-entity under " + collection.resourcePath());
        }
    }

    private Types types(Resource collection) throws RamlSpecException {
        Types.Builder builder = Types.builder();

        Optional<Method> method = collection.methods().stream().filter(m -> m.method().equals("get")).findFirst();
        if(method.isPresent()) {
            builder.entity(new BodyTypeResolver(this.status200Response(method.get()).body().get(0), this.typesPackage).resolve().build().typeRef());
            builder.error(this.typesPackage + ".Error");
            builder.message(this.typesPackage + ".Message");
        }

        method = collection.methods().stream().filter(m -> m.method().equals("post")).findFirst();
        if(method.isPresent()) {
            builder.create(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
        }

        Resource entityResource = this.entityResource(collection);
        method = entityResource.methods().stream().filter(m -> m.method().equals("put")).findFirst();
        if(method.isPresent()) {
            builder.replace(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
        }
        method = entityResource.methods().stream().filter(m -> m.method().equals("patch")).findFirst();
        if(method.isPresent()) {
            builder.update(new BodyTypeResolver(method.get().body().get(0), this.typesPackage).resolve().build().typeRef());
        }

        return builder.build();
    }

    private Response status200Response(Method method) {
        return method.responses().stream().filter(response -> response.code().value().equals("200")).findAny().get();
    }

    private boolean isPagedCollectionEntity(Resource resource) {
        return resource.annotations().stream().anyMatch(
                annotationRef -> "paged-collection-entity".equals(annotationRef.annotation().name())
        );
    }
}
