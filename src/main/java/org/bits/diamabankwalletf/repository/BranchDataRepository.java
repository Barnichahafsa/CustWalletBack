package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.BranchData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchDataRepository extends JpaRepository<BranchData, String> {
    List<BranchData> findByBranch(String branchCode);

    @Query("SELECT bd FROM BranchData bd WHERE bd.latitude IS NOT NULL AND bd.longitude IS NOT NULL")
    List<BranchData> findAllWithCoordinates();
}
