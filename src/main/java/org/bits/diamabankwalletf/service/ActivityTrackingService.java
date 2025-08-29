package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.CustomerActivity;
import org.bits.diamabankwalletf.repository.CustomerActivityRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTrackingService {

    private final CustomerActivityRepository activityRepository;

    public void saveUserActivityTracking(String userCode, String ip, String link, String desc,
                                         String userAgent, String deviceId) {
        try {
            CustomerActivity activity = new CustomerActivity();
            activity.setOpTimestamp(new Timestamp(System.currentTimeMillis()));
            activity.setUserCode(userCode);
            activity.setUserIp(ip);
            activity.setOpLink(link);
            activity.setOpDesc(desc);
            activity.setUserAgent(userAgent);
            activity.setDeviceId(deviceId);

            activityRepository.save(activity);
            log.debug("Activity tracking saved for user: {}", userCode);
        } catch (Exception e) {
            log.error("Failed to save activity tracking", e);
        }
    }
}
