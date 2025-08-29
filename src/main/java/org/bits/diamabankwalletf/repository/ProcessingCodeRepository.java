package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.ProcessingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ProcessingCodeRepository extends JpaRepository<ProcessingCode, String> {
    @Query("SELECT NEW map(p.code as id, p.wording as label) FROM ProcessingCode p")
    List<Map<String, Object>> findAllAsMap();

    ProcessingCode findByCode(String code);
}
