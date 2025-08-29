package org.bits.diamabankwalletf.repository;
import org.bits.diamabankwalletf.model.MobileMoneyOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface MobileMoneyOperatorRepository extends JpaRepository<MobileMoneyOperator, String> {
    @Query("SELECT NEW map(m.providerCode as id, m.wording as label) FROM MobileMoneyOperator m ORDER BY m.providerCode")
    List<Map<String, Object>> findAllProvidersAsMap();

    @Query("SELECT NEW map(m.providerCode as id, m.wordingAirtime as label) FROM MobileMoneyOperator m ORDER BY m.providerCode")
    List<Map<String, Object>> findAllAirtimeProvidersAsMap();
}
