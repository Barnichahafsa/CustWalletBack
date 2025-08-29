package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "JWT_PAYMENT_TOKENS")
public class JwtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOKEN_ID", unique = true, nullable = false, length = 100)
    private String tokenId;

    @Column(name = "JWT_TOKEN", nullable = false, length = 4000)
    private String jwtToken;

    @Column(name = "MERCHANT_WALLET", nullable = false, length = 22)
    private String merchantWallet;

    @Column(name = "MERCHANT_NUMBER", length = 20)
    private String merchantNumber;

    @Column(name = "MERCHANT_NAME", length = 40)
    private String merchantName;

    @Column(name = "BANK_CODE", length = 5)
    private String bankCode;

    @Column(name = "AMOUNT", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3)
    private String currency;

    @Column(name = "STATUS", length = 20)
    private String status; // ACTIVE, USED, EXPIRED, REVOKED

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @Column(name = "CUSTOMER_WALLET", length = 22)
    private String customerWallet; // Set when token is used

    @Column(name = "TRANSACTION_REF", length = 50)
    private String transactionRef; // Set when payment is processed

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status) && !isExpired();
    }

    public void markAsUsed(String customerWallet, String transactionRef) {
        this.status = "USED";
        this.usedAt = LocalDateTime.now();
        this.customerWallet = customerWallet;
        this.transactionRef = transactionRef;
    }

    public void markAsExpired() {
        this.status = "EXPIRED";
    }

    public void revoke() {
        this.status = "REVOKED";
    }

}
