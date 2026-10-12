package com.nithishramurthy.farm.fresh.exception;

/** Thrown when a requested record doesn't exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

