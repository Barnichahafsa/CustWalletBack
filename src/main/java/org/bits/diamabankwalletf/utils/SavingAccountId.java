package org.bits.diamabankwalletf.utils;

import java.io.Serializable;
import java.util.Objects;

public class SavingAccountId implements Serializable {

    private String walletNumber;
    private String bankCode;
    private String branchCode;

    public SavingAccountId() {}

    public SavingAccountId(String walletNumber, String bankCode, String branchCode) {
        this.walletNumber = walletNumber;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavingAccountId)) return false;
        SavingAccountId that = (SavingAccountId) o;
        return Objects.equals(walletNumber, that.walletNumber) &&
                Objects.equals(bankCode, that.bankCode) &&
                Objects.equals(branchCode, that.branchCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletNumber, bankCode, branchCode);
    }
}
