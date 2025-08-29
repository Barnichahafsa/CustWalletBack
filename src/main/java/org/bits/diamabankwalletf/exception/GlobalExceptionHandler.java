package org.bits.diamabankwalletf.exception;

import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PasswordErrorException.class)
    public ResponseEntity<AuthResponse> handlePasswordErrorException(PasswordErrorException ex) {
        log.error("Password error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse(false, null, ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AuthResponse> handleAccountLockedException(AccountLockedException ex) {
        log.error("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthResponse(false, null, "Account is locked"));
    }
}
