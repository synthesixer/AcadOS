package com.project.acados.exception;

/**
 * Thrown by services when a request breaks a business rule (BR-xx) or an invalid state transition.
 * Reference: class diagram.puml (exception)
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
