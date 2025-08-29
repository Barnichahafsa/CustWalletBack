package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BRANCH")
@Getter
@Setter
public class Branch {
    @Id
    @Column(name = "BRANCH_CODE")
    private String branchCode;

    @Column(name = "BANK_CODE")
    private String bankCode;

    @Column(name = "CITY_CODE")
    private String cityCode;

    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "WORDING")
    private String wording;

    @Column(name = "DAYS_BEFORE_RENEW")
    private Integer daysBeforeRenew;

    @Column(name = "BANK_LOCATION")
    private String bankLocation;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "ZIP_CODE")
    private String zipCode;
}
