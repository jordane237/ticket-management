package com.jordane237.ticketmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Conflit metier RG-03 : operation de changement d'etat interdite.
 */
public class IllegalStatusTransitionException extends BusinessException {

    public IllegalStatusTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
