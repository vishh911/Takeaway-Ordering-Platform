package com.campuseateries.service;

/** Domain-facing validation faults surfaced cleanly to MVC layers. */
public class BusinessRuleException extends Exception {

    public BusinessRuleException(String message) {
        super(message);
    }
}
