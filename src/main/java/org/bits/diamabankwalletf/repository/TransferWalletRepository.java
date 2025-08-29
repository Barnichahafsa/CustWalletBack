package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.TransferWalletToWallet;
import org.bits.diamabankwalletf.utils.TransferWalletPK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferWalletRepository extends JpaRepository<TransferWalletToWallet, TransferWalletPK> {

    Optional<TransferWalletToWallet> findByInternalReferenceNumber(String internalReferenceNumber);

    Optional<TransferWalletToWallet> findByToken(String token);

    @Query("SELECT t FROM TransferWalletToWallet t WHERE t.walletNumber = :walletNumber " +
            "ORDER BY t.transactionDate DESC")
    Page<TransferWalletToWallet> findByWalletNumberOrderByTransactionDateDesc(
            @Param("walletNumber") String walletNumber, Pageable pageable);

    @Query("SELECT t FROM TransferWalletToWallet t WHERE t.walletNumber = :walletNumber " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<TransferWalletToWallet> findByWalletAndDateRange(
            @Param("walletNumber") String walletNumber,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT t FROM TransferWalletToWallet t WHERE t.qrData = :qrData")
    Optional<TransferWalletToWallet> findByQrData(@Param("qrData") String qrData);
}
