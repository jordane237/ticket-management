package com.jordane237.ticketmanagement.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Gestion centralisee des erreurs : toutes les reponses d'erreur
 * suivent un format commun { status, error, message, timestamp }.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining(" "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corps de la requete illisible ou JSON invalide.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parametre invalide : " + ex.getName() + ".");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "RG-02 : un ticket avec ce titre existe deja (violation de la contrainte d'uniquite en base).");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne inattendue du serveur.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                errorLabel(status),
                message,
                LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    private String errorLabel(HttpStatus status) {
        if (status == HttpStatus.BAD_REQUEST) {
            return "VALIDATION_KO";
        }
        if (status == HttpStatus.CONFLICT) {
            return "CONFLIT_METIER";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "RESSOURCE_INTROUVABLE";
        }
        return "ERREUR_SERVEUR";
    }

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
    }
}
