package org.bits.diamabankwalletf.utils;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpAddressUtils {

    /**
     * Gets the client IP address by checking various HTTP headers and falling back to remoteAddr
     * @param request The HTTP request
     * @return The client IP address
     */
    public String getClientIp(HttpServletRequest request) {
        log.debug("Determining client IP address");

        // Check header: Client-IP
        String ipAddress = request.getHeader("Client-IP");
        log.trace("Client-IP header: {}", ipAddress);

        // Check header: X-Forwarded-For
        if (ipAddress == null) {
            ipAddress = request.getHeader("X-Forwarded-For");
            log.trace("X-Forwarded-For header: {}", ipAddress);
        }

        // Check header: Remote_Addr
        if (ipAddress == null) {
            ipAddress = request.getHeader("Remote_Addr");
            log.trace("Remote_Addr header: {}", ipAddress);
        }

        // Check header: X-FORWARDED-FOR (uppercase variant)
        if (ipAddress == null) {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            log.trace("X-FORWARDED-FOR header: {}", ipAddress);
        }

        // Check header: x-forwarded-for (lowercase variant)
        if (ipAddress == null) {
            ipAddress = request.getHeader("x-forwarded-for");
            log.trace("x-forwarded-for header: {}", ipAddress);
        }

        // Check header: HTTP_X_FORWARDED_FOR
        if (ipAddress == null) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            log.trace("HTTP_X_FORWARDED_FOR header: {}", ipAddress);
        }

        // Fallback to remote address if all headers are null
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
            log.trace("Remote address: {}", ipAddress);
        }

        // Handle comma-separated values (common in X-Forwarded-For)
        if (ipAddress != null && ipAddress.contains(",")) {
            // Take the first IP in the list which is typically the original client
            ipAddress = ipAddress.split(",")[0].trim();
            log.trace("Extracted first IP from comma-separated list: {}", ipAddress);
        }

        log.debug("Final determined client IP: {}", ipAddress);
        return ipAddress;
    }
}
