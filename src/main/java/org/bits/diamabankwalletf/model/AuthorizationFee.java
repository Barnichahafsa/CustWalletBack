package org.bits.diamabankwalletf.model;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "AUTHORIZATION_FEES")
@Data
public class AuthorizationFee {

    @Id
    @Column(name = "INDEX_FEES")
    private Long indexFees;

    @Column(name = "PROCESSING_CODE", length = 2)
    private String processingCode;

    @Column(name = "MCC_CODE", length = 4)
    private String mccCode;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "MESSAGE_TYPE", length = 5)
    private String messageType;

    @Column(name = "INTERFACE_ORIGIN", length = 6)
    private String interfaceOrigin;

    @Column(name = "WALLET_PRODUCT_CODE", length = 3)
    private String walletProductCode;

    @Column(name = "NETWORK_CODE", length = 2)
    private String networkCode;

    @Column(name = "ACQUIRER_COUNTRY_CODE", length = 3)
    private String acquirerCountryCode;

    @Column(name = "WALLET_NUMBER", length = 100)
    private String walletNumber;

    @Column(name = "TERMINAL_NUMBER", length = 15)
    private String terminalNumber;

    @Column(name = "WALLET_TYPE", length = 1)
    private String walletType;

    @Column(name = "ACTION_CODE", length = 3)
    private String actionCode;

    @Column(name = "BANK_CODE", length = 5)
    private String bankCode;

    @Column(name = "ORIGIN_TRANSACTION", length = 1)
    private String originTransaction;

    @Column(name = "WORDING", length = 64)
    private String wording;

    @Column(name = "FEE_CURRENCY_CODE", length = 3)
    private String feeCurrencyCode;

    @Column(name = "RATE", precision = 9, scale = 6)
    private BigDecimal rate;

    @Column(name = "FIXED_AMOUNT", precision = 15, scale = 3)
    private BigDecimal fixedAmount;

    @Column(name = "MAXIMUM", precision = 15, scale = 3)
    private BigDecimal maximum;

    @Column(name = "MINIMUM", precision = 15, scale = 3)
    private BigDecimal minimum;

    @Column(name = "THRESHOLD", precision = 15, scale = 3)
    private BigDecimal threshold;

    @Column(name = "GRACE", precision = 15, scale = 3)
    private BigDecimal grace;

    @Column(name = "WALLET_NBR_TRUNK", length = 22)
    private String walletNbrTrunk;
}
