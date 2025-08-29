package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bits.diamabankwalletf.utils.TransferWalletPK;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TRANSFER_WALLET_TO_WALLET")
@IdClass(TransferWalletPK.class)
public class TransferWalletToWallet {

    @Id
    @Column(name = "INTERNAL_REFERENCE_NUMBER", length = 12, nullable = false)
    private String internalReferenceNumber;

    @Id
    @Column(name = "BANK_CODE", length = 5, nullable = false)
    private String bankCode;

    @Id
    @Column(name = "WALLET_NUMBER", length = 22, nullable = false)
    private String walletNumber;

    @Id
    @Column(name = "SIGN_OPERATION", length = 1, nullable = false)
    private String signOperation;

    @Id
    @Column(name = "SEQUENCE_NUMBER", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "TRX_ID", length = 15)
    private String trxId;

    @Column(name = "BRANCH_CODE", length = 5)
    private String branchCode;

    @Column(name = "ACCOUNT_NUMBER", length = 24)
    private String accountNumber;

    @Column(name = "DEST_ACCOUNT_NUMBER", length = 24)
    private String destAccountNumber;

    @Column(name = "DEST_BANK_CODE", length = 5)
    private String destBankCode;

    @Column(name = "SRC_ACCOUNT_NUMBER", length = 24)
    private String srcAccountNumber;

    @Column(name = "ACCOUNT_CURRENCY", length = 3)
    private String accountCurrency;

    @Column(name = "DEST_WALLET_NUMBER", length = 24)
    private String destWalletNumber;

    @Column(name = "LINE_NUMBER", length = 22)
    private String lineNumber;

    @Column(name = "PROVIDER", length = 10)
    private String provider;

    @Column(name = "EXPIRY_DATE")
    @Temporal(TemporalType.DATE)
    private Date expiryDate;

    @Column(name = "PROCESSING_CODE", length = 2)
    private String processingCode;

    @Column(name = "BUSINESS_DATE")
    @Temporal(TemporalType.DATE)
    private Date businessDate;

    @Column(name = "PROCESSING_DATE")
    @Temporal(TemporalType.DATE)
    private Date processingDate;

    @Column(name = "WALLET_PRODUCT", length = 3)
    private String walletProduct;

    @Column(name = "TRANSACTION_AMOUNT", precision = 12, scale = 3)
    private BigDecimal transactionAmount;

    @Column(name = "TRANSACTION_CURRENCY", length = 3)
    private String transactionCurrency;

    @Column(name = "TRANSACTION_DATE")
    @Temporal(TemporalType.DATE)
    private Date transactionDate;

    @Column(name = "LOCAL_AMOUNT", precision = 12, scale = 3)
    private BigDecimal localAmount;

    @Column(name = "LOCAL_CURRENCY", length = 3)
    private String localCurrency;

    @Column(name = "SETTLEMENT_AMOUNT", precision = 12, scale = 3)
    private BigDecimal settlementAmount;

    @Column(name = "SETTLEMENT_CURRENCY", length = 3)
    private String settlementCurrency;

    @Column(name = "TRANSACTION_TYPE", length = 1)
    private String transactionType;

    @Column(name = "TRANSACTION_ENTITY", length = 24)
    private String transactionEntity;

    @Column(name = "USER_CAPTURING", length = 19)
    private String userCapturing;

    @Column(name = "USER_VALIDATING", length = 64)
    private String userValidating;

    @Column(name = "MEMO", length = 100)
    private String memo;

    @Column(name = "TRANSACTION_REASON", length = 100)
    private String transactionReason;

    @Column(name = "DATE_VALIDATION")
    @Temporal(TemporalType.DATE)
    private Date dateValidation;

    @Column(name = "REVERSAL_FLAG", length = 1)
    private String reversalFlag;

    @Column(name = "REVERSAL_REASON", length = 2)
    private String reversalReason;

    @Column(name = "TRANSACTION_ORIGIN", length = 1)
    private String transactionOrigin;

    @Column(name = "ONLINE_TRANSACTION_FLAG", length = 1)
    private String onlineTransactionFlag;

    @Column(name = "FLAG_PROCESSED", length = 1)
    private String flagProcessed;

    @Column(name = "WALLET_NBR_TRUNK", length = 22)
    private String walletNbrTrunk;

    @Column(name = "ACCOUNTING_PROCESS", length = 8)
    private String accountingProcess;

    @Column(name = "ACC_DATE")
    @Temporal(TemporalType.DATE)
    private Date accDate;

    @Column(name = "LOG_DATE")
    @Temporal(TemporalType.DATE)
    private Date logDate;

    @Column(name = "VALIDATION_FLAG", length = 1)
    private String validationFlag;

    @Column(name = "STATUS", length = 4)
    private String status;

    @Column(name = "TOKEN", length = 20)
    private String token;

    @Column(name = "EXPIRATION_DATE")
    @Temporal(TemporalType.DATE)
    private Date expirationDate;

    @Column(name = "AQUIRER_REFERENCE_NUMBER", length = 30)
    private String aquirerReferenceNumber;

    @Column(name = "FEES_AMOUNT", precision = 12, scale = 3)
    private BigDecimal feesAmount;

    @Column(name = "QR_DATA", length = 1024)
    private String qrData;

    @Column(name = "ACTION_CODE", length = 20)
    private String actionCode;

    @Column(name = "TERMINAL_ID", length = 12)
    private String terminalId;

    @Column(name = "MCC", length = 20)
    private String mcc;

    @Column(name = "OPENING_BALANCE", precision = 9, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "CLOSING_BALANCE", precision = 9, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "TAX", precision = 12, scale = 3)
    private BigDecimal tax;

    @Column(name = "ORIGINAL_DATA", length = 33)
    private String originalData;

    @Column(name = "COMISSION", precision = 9, scale = 2)
    private BigDecimal commission;

    @Column(name = "ELEVY_ID", length = 24)
    private String elevyId;

    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = new Date();
        }
        if (this.businessDate == null) {
            this.businessDate = new Date();
        }
        if (this.processingDate == null) {
            this.processingDate = new Date();
        }
        if (this.logDate == null) {
            this.logDate = new Date();
        }
    }
}
