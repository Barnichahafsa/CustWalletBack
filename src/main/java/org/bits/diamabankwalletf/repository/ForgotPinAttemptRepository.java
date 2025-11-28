package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.ForgotPinAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ForgotPinAttemptRepository extends JpaRepository<ForgotPinAttempt, Long> {

    @Query("SELECT COUNT(f) FROM ForgotPinAttempt f WHERE f.phoneNumber = :phoneNumber AND f.attemptTime > :since")
    long countAttemptsSince(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(f) FROM ForgotPinAttempt f WHERE f.ipAddress = :ipAddress AND f.attemptTime > :since")
    long countAttemptsByIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    Optional<ForgotPinAttempt> findBySessionTokenAndPhoneNumber(String sessionToken, String phoneNumber);

    Optional<ForgotPinAttempt> findByVerificationTokenAndPhoneNumber(String verificationToken, String phoneNumber);

    Optional<ForgotPinAttempt> findByResetTokenAndPhoneNumber(String resetToken, String phoneNumber);
}
