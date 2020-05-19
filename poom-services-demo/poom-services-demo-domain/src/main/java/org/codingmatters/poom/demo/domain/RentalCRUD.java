package org.codingmatters.poom.demo.domain;

import org.codingmatters.poom.api.generic.resource.api.types.Error;
import org.codingmatters.poom.apis.demo.api.types.*;
import org.codingmatters.poom.apis.demo.api.types.rental.Billing;
import org.codingmatters.poom.demo.domain.billing.BillingProcessor;
import org.codingmatters.poom.generic.resource.domain.GenericResourceAdapter;
import org.codingmatters.poom.generic.resource.domain.exceptions.*;
import org.codingmatters.poom.generic.resource.domain.spec.Action;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public class RentalCRUD implements GenericResourceAdapter.CRUD<Rental, RentalRequest, Void, RentalAction> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RentalCRUD.class);

    private final Set<Action> actions;
    private final String store;
    private final Repository<Rental, PropertyQuery> repository;
    private final Movie movie;
    private final BillingProcessor billingProcessor;

    public RentalCRUD(Set<Action> actions, String store, Repository<Rental, PropertyQuery> repository, Movie movie, BillingProcessor billingProcessor) {
        this.actions = actions;
        this.store = store;
        this.repository = repository;
        this.movie = movie;
        this.billingProcessor = billingProcessor;
    }

    @Override
    public String entityRepositoryUrl() {
        return String.format("/%s/movies/%s/rentals", this.store, this.movie.id());
    }

    @Override
    public Set<Action> supportedActions() {
        return this.actions;
    }

    @Override
    public Optional<Entity<Rental>> retrieveEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            return Optional.ofNullable(this.repository.retrieve(id));
        } catch (RepositoryException e) {
            throw new UnexpectedException(
                    Error.builder().code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error accessing rental repository", e))
                            .build(),
                    "error accessing rental repository", e);
        }
    }

    @Override
    public Entity<Rental> createEntityFrom(RentalRequest request) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        if(request.opt().customer().orElse("").isEmpty()) {
            throw new BadRequestException(
                    Error.builder()
                            .token(log.tokenized().info("rental request without customer : {}", request))
                            .code(Error.Code.INVALID_OBJECT_FOR_CREATION)
                            .description("must specify a customer")
                            .build(),
                    "no customer provided for rental request"
            );
        }

        try {
            if(! this.movieFreeForRent()) {
                throw new BadRequestException(
                        Error.builder()
                                .token(log.tokenized().info("request for a rental with a movie not free for rent : {}", request))
                                .code(Error.Code.INVALID_OBJECT_FOR_CREATION)
                                .description("movie is not free for rent")
                                .build(),
                        "movie is not free for rent"
                );
            }
            Entity<Rental> result = this.repository.create(Rental.builder()
                    .status(Rental.Status.OUT)
                    .start(UTC.now())
                    .movie(this.movie)
                    .customer(request.customer())
                    .start(UTC.now())
                    .dueDate(UTC.now().plusDays(3L))
                    .build());
            return this.repository.update(result, result.value().withId(result.id()));
        } catch (RepositoryException e) {
            throw new UnexpectedException(
                    Error.builder().code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error accessing rental repository", e))
                            .build(),
                    "error accessing rental repository", e);
        }
    }

    private boolean movieFreeForRent() throws RepositoryException {
        return this.repository.search(PropertyQuery.builder()
                .filter(String.format("movie.id == '%s' && status == '%s'", this.movie.id(), Rental.Status.OUT.name()))
                .build(),
                0, 0).total() == 0;
    }

    @Override
    public Entity<Rental> replaceEntityWith(String id, Void value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new UnexpectedException(Error.builder().code(Error.Code.UNEXPECTED_ERROR).build(), "not implemented");
    }

    @Override
    public Entity<Rental> updateEntityWith(String id, RentalAction value) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        try {
            Entity<Rental> rental = this.repository.retrieve(id);
            if(rental != null) {
                if(RentalAction.Type.RETURN.equals(value.type())) {
                    return this.repository.update(rental, rental.value().changed(builder -> builder
                            .status(Rental.Status.RETURNED)
                            .returnedDate(UTC.now())
                            .billing(this.processBilling(rental.value(), UTC.now()))
                    ));
                } else {
                    return this.repository.update(rental, rental.value().changed(builder -> builder
                            .status(Rental.Status.OUT)
                            .dueDate(UTC.now().plusDays(3L))
                    ));
                }
            } else {
                throw new NotFoundException(
                    Error.builder().code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().info("request for a un existent movie rental : {} for movie {}", id, this.movie))
                            .build(),
                    "no rental with that id");
            }
        } catch (RepositoryException e) {
            throw new UnexpectedException(
                    Error.builder().code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("error accessing rental repository", e))
                            .build(),
                    "error accessing rental repository", e);
        }
    }

    private Billing processBilling(Rental rental, LocalDateTime at) {
        return this.billingProcessor.process(rental, at);
    }

    @Override
    public void deleteEntity(String id) throws BadRequestException, ForbiddenException, NotFoundException, UnauthorizedException, UnexpectedException {
        throw new UnexpectedException(Error.builder().code(Error.Code.UNEXPECTED_ERROR).build(), "not implemented");
    }
}
