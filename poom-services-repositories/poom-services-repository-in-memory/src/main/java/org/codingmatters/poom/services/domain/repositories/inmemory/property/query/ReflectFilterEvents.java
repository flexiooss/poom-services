package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.poom.services.domain.property.query.StackedFilterEvents;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ReflectFilterEvents extends StackedFilterEvents<Predicate> {
    private final PropertyResolver propertyResolver;

    public ReflectFilterEvents(Class valueObjectCalss) {
        super(o -> true);
        this.propertyResolver = new PropertyResolver(valueObjectCalss);
    }

    @Override
    public Void graterThan(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left), right, true));
        return null;
    }

    @Override
    public Void graterThanOrEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left), right, false));
        return null;
    }

    @Override
    public Void graterThanProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right), true));
        return null;
    }

    @Override
    public Void graterThanOrEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.gt(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right), false));
        return null;
    }

    @Override
    public Void lowerThan(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left), right, true));
        return null;
    }

    @Override
    public Void lowerThanProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right), true));
        return null;
    }

    @Override
    public Void lowerThanOrEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left), right, false));
        return null;
    }

    @Override
    public Void lowerThanOrEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.lt(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right), false));
        return null;
    }

    @Override
    public Void isEquals(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.eq(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void isEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.eq(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right)));
        return null;
    }

    @Override
    public Void isNotEquals(String left, Object right) throws FilterEventError {
        this.push(o -> ! Operators.eq(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void isNotEqualsProperty(String left, String right) throws FilterEventError {
        this.push(o -> ! Operators.eq(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right)));
        return null;
    }

    @Override
    public Void isNull(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property) == null);
        return null;
    }

    @Override
    public Void isNotNull(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property) != null);
        return null;
    }

    @Override
    public Void isEmpty(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property) == null || this.propertyResolver.resolve(o, property).toString().isEmpty());
        return null;
    }

    @Override
    public Void isNotEmpty(String property) throws FilterEventError {
        this.push(o -> this.propertyResolver.resolve(o, property) != null && ! this.propertyResolver.resolve(o, property).toString().isEmpty());
        return null;
    }

    @Override
    public Void startsWith(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.startsWith(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void startsWithProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.startsWith(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right)));
        return null;
    }

    @Override
    public Void endsWith(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.endsWith(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void endsWithProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.endsWith(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right)));
        return null;
    }

    @Override
    public Void contains(String left, Object right) throws FilterEventError {
        this.push(o -> Operators.containsOne(
                this.propertyResolver.resolve(o, left),
                Collections.singletonList(right)
        ));
        return null;
    }

    @Override
    public Void containsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.containsOne(
                this.propertyResolver.resolve(o, left),
                Collections.singletonList(this.propertyResolver.resolve(o, right)))
        );
        return null;
    }

    @Override
    public Void in(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.in(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void containsAny(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.containsOne(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void containsAll(String left, List<Object> right) throws FilterEventError {
        this.push(o -> Operators.containsAll(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void not() throws FilterEventError {
        this.push(this.pop().negate());
        return null;
    }

    @Override
    public Void and() throws FilterEventError {
        Predicate right = this.pop();
        Predicate left = this.pop();
        Predicate result = left.and(right);
        this.push(result);
        return null;
    }

    @Override
    public Void or() throws FilterEventError {
        Predicate right = this.pop();
        Predicate left = this.pop();
        Predicate result = left.or(right);
        this.push(result);
        return null;
    }


}
