package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "MOBILE_MONEY_OPERATOR") // Exactly as in Queries
@Data
public class MobileMoneyOperator {

    @Id
    @Column(name = "PROVIDER_CODE")
    private String providerCode;

    @Column(name = "WORDING")
    private String wording;

    @Column(name = "WORDING_AIRTIME")
    private String wordingAirtime;
}
