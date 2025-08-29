package org.bits.diamabankwalletf.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.UserActivityTracking;
import org.bits.diamabankwalletf.repository.UserActivityTrackingRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
@Service
public class UserActivityTrackingService {

    private final UserActivityTrackingRepository repository;
    private final TaskExecutor taskExecutor;

    public UserActivityTrackingService(
            UserActivityTrackingRepository repository,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.repository = repository;
        this.taskExecutor = taskExecutor;
    }


    public void trackActivity(HttpServletRequest request, String description) {
        final String userCode = getCurrentUserCode();
        final String clientIp = getClientIp(request);
        final String link = request.getRequestURI() +
                (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        final String deviceId = request.getHeader("X-Device-ID");
        final String userAgent = request.getHeader("User-Agent");

        taskExecutor.execute(() -> {
            try {
                UserActivityTracking tracking = new UserActivityTracking();
                tracking.setOpTimestamp(Timestamp.from(Instant.now()));
                tracking.setUserCode(userCode);
                tracking.setUserIp(clientIp);
                tracking.setOpLink(link);
                tracking.setOpDesc(description);
                tracking.setDeviceId(deviceId);
                tracking.setUserAgent(userAgent);

                repository.save(tracking);
            } catch (Exception e) {
                log.error("Failed to save user activity", e);
            }
        });
    }

    private String getCurrentUserCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
