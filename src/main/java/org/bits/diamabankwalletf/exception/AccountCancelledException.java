package org.bits.diamabankwalletf.exception;


import org.springframework.security.authentication.AccountStatusException;

public class AccountCancelledException extends AccountStatusException {
    public AccountCancelledException(String msg) {
        super(msg);
    }

    public AccountCancelledException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
