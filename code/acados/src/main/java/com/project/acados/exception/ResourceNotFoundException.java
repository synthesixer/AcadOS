package com.project.acados.exception;

/**
 * Thrown by services when the requested data does not exist or does not belong to the caller.
 * Reference: class diagram.puml (exception)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
