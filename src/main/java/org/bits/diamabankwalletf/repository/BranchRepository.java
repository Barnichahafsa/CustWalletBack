package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findByBankCode(String bankCode);
    Optional<Branch> findByBankCodeAndBranchCode(String bankCode, String branchCode);
}
