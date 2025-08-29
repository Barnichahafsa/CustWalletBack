package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Timestamp> {
}
