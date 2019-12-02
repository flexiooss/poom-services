package org.codingmatters.poom.services.domain.repositories.inmemory.property.query;

import org.codingmatters.poom.services.domain.property.query.StackedFilterEvents;
import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;

import java.util.List;
import java.util.function.Predicate;

public class ReflectFilterEvents extends StackedFilterEvents<Predicate> {
    private final PropertyResolver propertyResolver;

    public ReflectFilterEvents(Class valueObjectCalss) {
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
        this.push(o -> Operators.contains(this.propertyResolver.resolve(o, left), right));
        return null;
    }

    @Override
    public Void containsProperty(String left, String right) throws FilterEventError {
        this.push(o -> Operators.contains(this.propertyResolver.resolve(o, left), this.propertyResolver.resolve(o, right)));
        return null;
    }

    @Override
    public Void not() throws FilterEventError {
        this.push(this.pop().negate());
        return null;
    }

    @Override
    public Void and() throws FilterEventError {
        List<Predicate> all = this.reversedPopAll();
        Predicate result = all.get(0);
        for (int i = 1; i < all.size(); i++) {
            result = result.and(all.get(i));
        }
        this.push(result);
        return null;
    }

    @Override
    public Void or() throws FilterEventError {
        List<Predicate> all = this.reversedPopAll();
        Predicate result = all.get(0);
        for (int i = 1; i < all.size(); i++) {
            result = result.or(all.get(i));
        }
        this.push(result);
        return null;
    }


}
