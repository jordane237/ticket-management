package com.jordane237.ticketmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Requete invalide (parametre ou champ non conforme).
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
