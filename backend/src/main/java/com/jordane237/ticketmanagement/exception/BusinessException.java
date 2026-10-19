package com.jordane237.ticketmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception metier avec code HTTP associe.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
