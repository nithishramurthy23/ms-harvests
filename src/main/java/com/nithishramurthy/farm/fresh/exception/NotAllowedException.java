package com.nithishramurthy.farm.fresh.exception;

/** The caller is logged in but their role doesn't allow this action (HTTP 403). */
public class NotAllowedException extends RuntimeException {
    public NotAllowedException(String message) {
        super(message);
    }
}
