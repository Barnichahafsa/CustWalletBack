package org.bits.diamabankwalletf.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.service.ActivityTrackingService;
import org.bits.diamabankwalletf.utils.IpAddressUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private final ActivityTrackingService activityService;
    private final IpAddressUtils ipAddressUtils;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Skip tracking for non-controller endpoints
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        try {
            // Get controller and method name as the link
            String link = ((HandlerMethod) handler).getMethod().getDeclaringClass().getSimpleName() +
                    "." + ((HandlerMethod) handler).getMethod().getName();

            // Get IP address using our utility
            String ipAddress = ipAddressUtils.getClientIp(request);

            // Get user agent
            String userAgent = request.getHeader("User-Agent");

            // Get device ID from header or attribute
            String deviceId = request.getHeader("Device-ID");
            if (deviceId == null) {
                deviceId = (String) request.getAttribute("deviceId");
            }

            // Get user code from security context
            String userCode = getUserCodeFromContext();

            // Description based on status code
            String desc = String.format("%s request to %s completed with status %d",
                    request.getMethod(), request.getRequestURI(), response.getStatus());

            // Call your service
            activityService.saveUserActivityTracking(userCode, ipAddress, link, desc, userAgent, deviceId);
        } catch (Exception e) {
            log.error("Error in activity tracking interceptor", e);
        }
    }

    private String getUserCodeFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return "anonymous";
    }
}
