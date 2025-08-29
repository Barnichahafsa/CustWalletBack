package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, String> {
    @Query("SELECT a FROM Advertisement a WHERE a.bankCode = :bankCode")
    List<Advertisement> findByBankCode(@Param("bankCode") String bankCode);
}
