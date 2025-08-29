package org.bits.diamabankwalletf.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "CASH_OUT_HIST")
@IdClass(CashOut.CashOutId.class)
public class CashOut {
    @Id
    private Date cashOutDate;
    @Id
    private String agentCode;
    private String customerId;
    private String amount;
    private String bankCode;
    private String phoneNumber;
    private String walletNumber;
    private String branchCode;

    // Composite key
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashOutId implements Serializable {
        private Date cashOutDate;
        private String agentCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CashOutId cashOutId = (CashOutId) o;

            if (!cashOutDate.equals(cashOutId.cashOutDate)) return false;
            return agentCode.equals(cashOutId.agentCode);
        }

        @Override
        public int hashCode() {
            int result = cashOutDate.hashCode();
            result = 31 * result + agentCode.hashCode();
            return result;
        }
    }
}
