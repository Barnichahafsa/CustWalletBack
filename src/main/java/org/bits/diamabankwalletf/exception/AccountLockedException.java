package org.bits.diamabankwalletf.exception;

public class AccountLockedException extends RuntimeException {
    private final String code;

    public AccountLockedException(String code) {
        super("Account is locked");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
