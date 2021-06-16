package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.poom.services.domain.property.query.events.FilterEventError;

import java.util.List;

public interface FilterEvents<T> {

    default T graterThan(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T graterThanProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T graterThanOrEquals(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T graterThanOrEqualsProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T lowerThan(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T lowerThanProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T lowerThanOrEquals(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T lowerThanOrEqualsProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isNull(String property) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isNotNull(String property) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isEmpty(String property) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isNotEmpty(String property) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isEquals(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isEqualsProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isNotEquals(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T isNotEqualsProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T startsWith(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T startsWithProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T endsWith(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T endsWithProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T contains(String left, Object right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T containsProperty(String left, String right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T in(String left, List<Object> right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T containsAny(String left, List<Object> right) throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T containsAll(String left, List<Object> right) throws FilterEventError {throw new FilterEventError("feature not implemented");}

    default T not() throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T and() throws FilterEventError {throw new FilterEventError("feature not implemented");}
    default T or() throws FilterEventError {throw new FilterEventError("feature not implemented");}

    static <T> FilterEvents<T> noop() {
        return new FilterEvents<T>() {
            @Override
            public T graterThan(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T graterThanProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T graterThanOrEquals(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T graterThanOrEqualsProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T lowerThan(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T lowerThanProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T lowerThanOrEquals(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T lowerThanOrEqualsProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T isEquals(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T isEqualsProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T startsWith(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T startsWithProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T endsWith(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T endsWithProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T contains(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T containsProperty(String left, String right) throws FilterEventError {
                return null;
            }

            @Override
            public T not() throws FilterEventError {
                return null;
            }

            @Override
            public T in(String left, List<Object> right) throws FilterEventError {
                return null;
            }

            @Override
            public T containsAny(String left, List<Object> right) throws FilterEventError {
                return null;
            }

            @Override
            public T containsAll(String left, List<Object> right) throws FilterEventError {
                return null;
            }

            @Override
            public T and() throws FilterEventError {
                return null;
            }

            @Override
            public T or() throws FilterEventError {
                return null;
            }

            @Override
            public T isNull(String property) throws FilterEventError {
                return null;
            }

            @Override
            public T isNotNull(String property) throws FilterEventError {
                return null;
            }

            @Override
            public T isEmpty(String property) throws FilterEventError {
                return null;
            }

            @Override
            public T isNotEmpty(String property) throws FilterEventError {
                return null;
            }

            @Override
            public T isNotEquals(String left, Object right) throws FilterEventError {
                return null;
            }

            @Override
            public T isNotEqualsProperty(String left, String right) throws FilterEventError {
                return null;
            }
        };
    }
}
