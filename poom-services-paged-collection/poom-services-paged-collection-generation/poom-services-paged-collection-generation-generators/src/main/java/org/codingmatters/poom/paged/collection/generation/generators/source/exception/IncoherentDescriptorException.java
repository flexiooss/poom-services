package org.codingmatters.poom.paged.collection.generation.generators.source.exception;

public class IncoherentDescriptorException extends Exception {
    public IncoherentDescriptorException(String message) {
        super(message);
    }

    public IncoherentDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }
}
