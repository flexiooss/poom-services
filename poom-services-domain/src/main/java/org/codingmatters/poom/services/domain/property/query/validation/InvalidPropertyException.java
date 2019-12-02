package org.codingmatters.poom.services.domain.property.query.validation;

public class InvalidPropertyException extends Exception {
    public InvalidPropertyException(String s) {
        super(s);
    }

    public InvalidPropertyException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
