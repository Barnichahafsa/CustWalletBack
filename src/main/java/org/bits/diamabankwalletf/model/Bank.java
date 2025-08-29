package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "BANK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bank {

    @Id
    @Column(name = "BANK_CODE", length = 5)
    private String bankCode;

    @Column(name = "WORDING", length = 30)
    private String wording;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "TVA_RATE", length = 6)
    private String tvaRate;

    @Column(name = "DAYS_BEFORE_RENEW")
    private Integer daysBeforeRenew;

    @Column(name = "ADDRESS", length = 1000)
    private String address;

    @Column(name = "COUNTRY_CODE", length = 3)
    private String countryCode;

    @Column(name = "ZIP_CODE", length = 20)
    private String zipCode;

    @Column(name = "CITY_CODE", length = 5)
    private String cityCode;

    @Column(name = "BANK_TYPE", length = 1)
    private String bankType;

    @Column(name = "BANK_API_SERVICES", length = 100)
    private String bankApiServices;

    @Column(name = "MASTER_BANK_CODE", length = 5)
    private String masterBankCode;

    @Column(name = "EXPIRATION_FLAG", length = 1)
    private String expirationFlag;

    @Column(name = "VERIFICATION_FLAG", length = 1)
    private String verificationFlag;

    @Column(name = "GH_BANK_CODE", length = 6)
    private String ghBankCode;

    @Column(name = "GIP_CERTIFIED", length = 20)
    private String gipCertified;

    @Column(name = "AGENT_VERIFICATION", length = 20)
    private String agentVerification;

    @Column(name = "DEBIT_PULL_OTP", length = 20)
    private String debitPullOtp;
}
