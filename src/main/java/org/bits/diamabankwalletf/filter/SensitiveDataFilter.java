package org.bits.diamabankwalletf.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SensitiveDataFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Apply only to auth endpoints
        if (request.getRequestURI().contains("/api/auth/")) {
            SensitiveDataRequestWrapper requestWrapper = new SensitiveDataRequestWrapper(request);

            // Log the request without sensitive data
            if (log.isDebugEnabled()) {
                log.debug("Request URI: {}, Masked body: {}",
                        request.getRequestURI(), requestWrapper.getMaskedBody());
            }

            // Continue with the wrapped request
            filterChain.doFilter(requestWrapper, response);
        } else {
            // For non-auth requests, continue normally
            filterChain.doFilter(request, response);
        }
    }
}
