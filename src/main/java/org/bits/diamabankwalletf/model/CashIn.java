package org.bits.diamabankwalletf.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "CASH_IN_HIST")
@IdClass(CashIn.CashInId.class)
public class CashIn {
    @Id
    private String cashInDate;
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
    public static class CashInId implements Serializable {
        private String cashInDate;
        private String agentCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CashInId cashInId = (CashInId) o;
            if (!cashInDate.equals(cashInId.cashInDate)) return false;
            return agentCode.equals(cashInId.agentCode);
        }

        @Override
        public int hashCode() {
            int result = cashInDate.hashCode();
            result = 31 * result + agentCode.hashCode();
            return result;
        }
    }
}
