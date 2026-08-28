package com.exelynt.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlerClass {

        // 403 - Forbidden (custom business-logic ownership check, e.g. USER accessing
        // another user's reservation)
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        // 403 - Forbidden (Spring Security method-level access denial, e.g.
        // @PreAuthorize failures)
        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<String> handleSpringAccessDeniedException(
                        org.springframework.security.access.AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("You do not have permission to perform this action");
        }

        // 400 - Bad Request
        @ExceptionHandler(InvalidBookingException.class)
        public ResponseEntity<String> handleInvalidBookingException(InvalidBookingException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

        // 404 - Resource Not Found
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        // 401 - Bad Credentials (login failure)
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        // 400 - Validation Errors
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {

                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        // 500 - Fallback for anything unhandled
        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleGenericException(Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
}