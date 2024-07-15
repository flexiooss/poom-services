package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.poom.services.domain.property.query.StackedFilterEvents;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ReflectFilterEvents<V> extends StackedFilterEvents<Predicate<V>> {
    private final PropertyResolver propertyResolver;

    public ReflectFilterEvents(Class valueObjectClass) {
        super(o -> true);
        this.propertyResolver = new PropertyResolver(valueObjectClass);
    }

    @Override
    public Void graterThan(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left).value(), right, true));
        return null;
    }

    @Override
    public Void graterThanOrEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left).value(), right, false));
        return null;
    }

    @Override
    public Void graterThanProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value(), true));
        return null;
    }

    @Override
    public Void graterThanOrEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value(), false));
        return null;
    }

    @Override
    public Void lowerThan(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left).value(), right, true));
        return null;
    }

    @Override
    public Void lowerThanProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value(), true));
        return null;
    }

    @Override
    public Void lowerThanOrEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left).value(), right, false));
        return null;
    }

    @Override
    public Void lowerThanOrEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value(), false));
        return null;
    }

    @Override
    public Void isEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.eq(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void isEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.eq(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value()));
        return null;
    }

    @Override
    public Void isNotEquals(String left, Object right) throws FilterEventError {
        this.push(o -> ! Operators.eq(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void isNotEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> ! Operators.eq(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value()));
        return null;
    }

    @Override
    public Void isNull(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property).value() == null);
        return null;
    }

    @Override
    public Void isNotNull(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property).value() != null);
        return null;
    }

    @Override
    public Void isEmpty(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property).value() == null || this.propertyResolver.resolve(o, property).value().toString().isEmpty());
        return null;
    }

    @Override
    public Void isNotEmpty(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property).value() != null && ! this.propertyResolver.resolve(o, property).value().toString().isEmpty());
        return null;
    }

    @Override
    public Void startsWith(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.startsWith(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void startsWithProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.startsWith(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value()));
        return null;
    }

    @Override
    public Void endsWith(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.endsWith(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void endsWithProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.endsWith(this.propertyResolver.resolve(o, left).value(), this.propertyResolver.resolve(o, right).value()));
        return null;
    }

    @Override
    public Void contains(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.containsOne(
                this.propertyResolver.resolve(o, left).value(),
                Collections.singletonList(right)
        ));
        return null;
    }

    @Override
    public Void containsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.containsOne(
                this.propertyResolver.resolve(o, left).value(),
                Collections.singletonList(this.propertyResolver.resolve(o, right).value()))
        );
        return null;
    }

    @Override
    public Void in(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.in(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void containsAny(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.containsOne(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void startsWithAny(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.startsWithOne(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void endsWithAny(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.endsWithOne(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void containsAll(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.containsAll(this.propertyResolver.resolve(o, left).value(), right));
        return null;
    }

    @Override
    public Void not() throws FilterEventError {
        this.push(this.pop().negate());
        return null;
    }

    @Override
    public Void and() throws FilterEventError {
        Predicate<V> right = this.pop();
        Predicate<V> left = this.pop();
        Predicate<V> result = left.and(right);
        this.push(result);
        return null;
    }

    @Override
    public Void or() throws FilterEventError {
        Predicate<V> right = this.pop();
        Predicate<V> left = this.pop();
        Predicate<V> result = left.or(right);
        this.push(result);
        return null;
    }


}
