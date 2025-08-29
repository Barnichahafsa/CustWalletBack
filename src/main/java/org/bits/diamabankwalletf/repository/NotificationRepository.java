package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query("SELECT n FROM Notification n")
    List<Notification> findAllNotifications();
}
