package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @Query("SELECT c.bankCode FROM Customer c WHERE c.phoneNumber = :phoneNumber")
    String findBankCodeByPhoneNumber(@Param("phoneNumber") String phoneNumber);


}
