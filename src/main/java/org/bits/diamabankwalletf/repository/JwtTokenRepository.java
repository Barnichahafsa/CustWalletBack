package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long > {
    Optional<JwtToken> findByTokenId(String tokenId);

    List<JwtToken> findByMerchantWallet(String merchantWallet);

    List<JwtToken> findByStatus(String status);

    List<JwtToken> findByMerchantWalletAndStatus(String merchantWallet, String status);

    @Query("SELECT j FROM JwtToken j WHERE j.status = 'ACTIVE' AND j.expiresAt < :now")
    List<JwtToken> findExpiredActiveTokens(@Param("now") LocalDateTime now);

    @Query("SELECT j FROM JwtToken j WHERE j.merchantWallet = :merchantWallet " +
            "AND j.createdAt BETWEEN :startDate AND :endDate ORDER BY j.createdAt DESC")
    List<JwtToken> findByMerchantWalletAndDateRange(
            @Param("merchantWallet") String merchantWallet,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(j) FROM JwtToken j WHERE j.merchantWallet = :merchantWallet " +
            "AND j.status = 'ACTIVE'")
    long countActiveTokensByMerchant(@Param("merchantWallet") String merchantWallet);

}
