package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Notification;
import org.bits.diamabankwalletf.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Map<String, Object>> getNotificationList(String customerId) {
        log.info("Getting notifications for customerId=[{}]", customerId);

        List<Notification> notifications = notificationRepository.findAllNotifications();

        return notifications.stream()
                .map(notification -> {
                    Map<String, Object> notificationMap = new HashMap<>();
                    notificationMap.put("id", notification.getId());
                    notificationMap.put("title", notification.getTitle());
                    notificationMap.put("body", notification.getBody());
                    return notificationMap;
                })
                .collect(Collectors.toList());
    }
}
