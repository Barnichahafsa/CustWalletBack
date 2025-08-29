package org.bits.diamabankwalletf.utils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class CustomerActivityId implements Serializable {
    private Timestamp opTimestamp;
    private String userCode;

    public CustomerActivityId() {}

    public CustomerActivityId(Timestamp opTimestamp, String userCode) {
        this.opTimestamp = opTimestamp;
        this.userCode = userCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerActivityId that = (CustomerActivityId) o;
        return Objects.equals(opTimestamp, that.opTimestamp) &&
                Objects.equals(userCode, that.userCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opTimestamp, userCode);
    }
}
