package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.AuthorizationFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AuthorizationFeeRepository extends JpaRepository<AuthorizationFee, Long> {


    @Query("SELECT af FROM AuthorizationFee af WHERE " +
            "af.processingCode = :processingCode AND " +
            "af.currencyCode = :currencyCode AND " +
            "af.walletProductCode = :walletProductCode AND " +
            "af.walletType = :walletType AND " +
            "af.actionCode = :actionCode AND " +
            "af.bankCode = :bankCode AND " +
            "af.originTransaction = :originTransaction AND " +
            "(af.messageType = :messageType OR af.messageType IS NULL) AND " +
            "(:amount >= af.minimum OR af.minimum IS NULL) AND " +
            "(:amount <= af.maximum OR af.maximum IS NULL) " +
            "ORDER BY af.minimum DESC, af.maximum ASC")
    List<AuthorizationFee> findApplicableFees(
            @Param("processingCode") String processingCode,
            @Param("currencyCode") String currencyCode,
            @Param("walletProductCode") String walletProductCode,
            @Param("walletType") String walletType,
            @Param("actionCode") String actionCode,
            @Param("bankCode") String bankCode,
            @Param("originTransaction") String originTransaction,
            @Param("messageType") String messageType,
            @Param("amount") BigDecimal amount
    );
}
