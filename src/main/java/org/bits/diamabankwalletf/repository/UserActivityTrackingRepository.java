package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.UserActivityTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityTrackingRepository extends JpaRepository<UserActivityTracking, UserActivityTracking.UserActivityTrackingId> {
}
