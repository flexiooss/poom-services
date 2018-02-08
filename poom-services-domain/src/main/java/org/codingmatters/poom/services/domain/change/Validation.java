package org.codingmatters.poom.services.domain.change;

import java.util.Objects;

/**
 * Created by nelt on 6/23/17.
 */
public class Validation {
    static public Validation valid() {
        return new Validation(true, null);
    }

    static public Validation invalid(String message, Object ... args) {
        return new Validation(false, String.format(message, args));
    }

    private final boolean isValid;
    private final String message;

    public Validation(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Validation that = (Validation) o;
        return isValid == that.isValid &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isValid, message);
    }

    @Override
    public String toString() {
        return "Validation{" +
                "isValid=" + isValid +
                ", message='" + message + '\'' +
                '}';
    }
}
