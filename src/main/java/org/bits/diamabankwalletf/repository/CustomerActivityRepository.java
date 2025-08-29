package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.CustomerActivity;
import org.bits.diamabankwalletf.utils.CustomerActivityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerActivityRepository extends JpaRepository<CustomerActivity, CustomerActivityId> {
    List<CustomerActivity> findByUserCode(String userCode);
    List<CustomerActivity> findByUserIp(String userIp);
}
