package com.jordane237.ticketmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Ressource inexistante (par exemple identifiant de ticket inconnu).
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
