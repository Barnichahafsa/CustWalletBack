package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.utils.WalletDataPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, WalletDataPK> {
    Optional<Wallet> findByPhoneNumber(String phoneNumber);

    @Query("SELECT w FROM Wallet w WHERE w.phoneNumber = :phoneNumber AND w.bankCode = :bankCode")
    Optional<Wallet> findByPhoneNumberAndBankCode(@Param("phoneNumber") String phoneNumber, @Param("bankCode") String bankCode);

    @Query("SELECT w.bankCode FROM Wallet w WHERE w.phoneNumber = :phoneNumber")
    String findBankCodeByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT w.clientCode FROM Wallet w WHERE w.phoneNumber = :phoneNumber AND w.bankCode = :bankCode")
    String findClientCodeByPhoneNumberAndBankCode(@Param("phoneNumber") String phoneNumber, @Param("bankCode") String bankCode);

    @Query("SELECT w FROM Wallet w WHERE w.mobileNumber = :mobileNumber AND w.statusWallet = 'A'")
    Optional<Wallet> findByMobileNumber(@Param("mobileNumber") String mobileNumber);

    @Query("SELECT w FROM Wallet w WHERE w.walletNumber = :walletNumber AND w.bankCode = :bankCode")
    Optional<Wallet> findByWalletNumberAndBankCode(
            @Param("walletNumber") String walletNumber,
            @Param("bankCode") String bankCode);

    @Query("SELECT w FROM Wallet w WHERE w.clientCode = :customerId AND w.bankCode = :bankCode")
    Optional<Wallet> findByCustomerIdAndBankCode(
            @Param("customerId") String customerId,
            @Param("bankCode") String bankCode);

    /**
     * Trouver les portefeuilles avec PIN expiré
     */
    @Query("SELECT w FROM Wallet w WHERE w.pinExpiryDate < CURRENT_DATE AND w.statusWallet = 'A' AND w.pinChangeRequired = 'N'")
    List<Wallet> findWalletsWithExpiredPin();

    /**
     * Trouver les portefeuilles nécessitant une notification d'expiration (7 jours)
     */
    @Query("SELECT w FROM Wallet w WHERE w.pinExpiryDate BETWEEN CURRENT_DATE AND :sevenDaysFromNow " +
            "AND w.statusWallet = 'A' AND w.pinExpiryNotificationSent = 'N'")
    List<Wallet> findWalletsNeedingSevenDayNotification(@Param("sevenDaysFromNow") Date sevenDaysFromNow);

    /**
     * Trouver les portefeuilles nécessitant une notification d'expiration (3 jours)
     */
    @Query("SELECT w FROM Wallet w WHERE w.pinExpiryDate BETWEEN CURRENT_DATE AND :threeDaysFromNow " +
            "AND w.statusWallet = 'A' AND w.pinExpiryNotificationSent IN ('N', '7')")
    List<Wallet> findWalletsNeedingThreeDayNotification(@Param("threeDaysFromNow") Date threeDaysFromNow);

    /**
     * Trouver les portefeuilles nécessitant une notification d'expiration (1 jour)
     */
    @Query("SELECT w FROM Wallet w WHERE w.pinExpiryDate BETWEEN CURRENT_DATE AND :oneDayFromNow " +
            "AND w.statusWallet = 'A' AND w.pinExpiryNotificationSent IN ('N', '7', '3')")
    List<Wallet> findWalletsNeedingOneDayNotification(@Param("oneDayFromNow") Date oneDayFromNow);

    /**
     * Compter les portefeuilles avec PIN expirant aujourd'hui
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.pinExpiryDate = CURRENT_DATE AND w.statusWallet = 'A'")
    Long countWalletsWithPinExpiringToday();

    // Dans WalletRepository.java
    Optional<Wallet> findByClientCodeAndBankCode(String clientCode, String bankCode);
}
