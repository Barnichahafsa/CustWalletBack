package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface BankRepository extends JpaRepository<Bank, String> {
}
