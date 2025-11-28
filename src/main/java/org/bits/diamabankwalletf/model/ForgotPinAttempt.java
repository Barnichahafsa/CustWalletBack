package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "FORGOT_PIN_ATTEMPTS")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPinAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_number", nullable = false)
    private String walletNumber;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ForgotPinStatus status;

    @Column(name = "session_token")
    private String sessionToken;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "failure_reason")
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        if (attemptTime == null) {
            attemptTime = LocalDateTime.now();
        }
    }

    public enum ForgotPinStatus {
        INITIATED,
        OTP_SENT,
        OTP_VERIFIED,
        QUESTIONS_VERIFIED,
        COMPLETED,
        FAILED,
        EXPIRED
    }
}
