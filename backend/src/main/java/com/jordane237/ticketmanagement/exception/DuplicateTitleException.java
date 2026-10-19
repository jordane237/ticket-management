package com.jordane237.ticketmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Conflit metier RG-02 : le titre du ticket existe deja.
 * Couvre aussi les violations de contrainte d'unicite detectees par la base.
 */
public class DuplicateTitleException extends BusinessException {

    public DuplicateTitleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
