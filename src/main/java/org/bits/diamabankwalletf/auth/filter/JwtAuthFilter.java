package org.bits.diamabankwalletf.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.auth.config.JwtConfig;
import org.bits.diamabankwalletf.auth.security.JwtService;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.IpAddressUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final WalletRepository walletRepository;
    private final JwtConfig jwtConfig;
    private final IpAddressUtils ipAddressUtils;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String servletPath = request.getServletPath();
        String method = request.getMethod();

        log.debug("=== JWT FILTER ===");
        log.debug("Processing request: {} {}", method, requestURI);
        log.debug("Servlet path: {}", servletPath);

        // Skip JWT validation for endpoints that should be permitted
        if (isPermittedEndpoint(servletPath)) {
            log.debug("Permitted endpoint detected, skipping JWT validation: {}", servletPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Log ALL headers for debugging
        log.debug("=== ALL REQUEST HEADERS ===");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            log.debug("Header: {} = {}", headerName, headerValue);
        }
        log.debug("=============================");

        // Try to extract JWT token from multiple sources
        String jwt = null;
        String tokenSource = null;

        // Method 1: Standard Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            tokenSource = "Authorization header";
            log.debug("Token found in Authorization header");
        }

        // Method 2: Custom X-Auth-Token header
        if (jwt == null) {
            String xAuthToken = request.getHeader("X-Auth-Token");
            if (xAuthToken != null) {
                jwt = xAuthToken;
                tokenSource = "X-Auth-Token header";
                log.debug("Token found in X-Auth-Token header");
            }
        }

        // Method 3: Custom X-Bearer-Token header
        if (jwt == null) {
            String xBearerToken = request.getHeader("X-Bearer-Token");
            if (xBearerToken != null && xBearerToken.startsWith("Bearer ")) {
                jwt = xBearerToken.substring(7);
                tokenSource = "X-Bearer-Token header";
                log.debug("Token found in X-Bearer-Token header");
            }
        }

        // Method 4: Custom X-JWT header
        if (jwt == null) {
            String xJwtToken = request.getHeader("X-JWT");
            if (xJwtToken != null) {
                jwt = xJwtToken;
                tokenSource = "X-JWT header";
                log.debug("Token found in X-JWT header");
            }
        }

        // Method 5: Query parameter (last resort)
        if (jwt == null) {
            String queryToken = request.getParameter("access_token");
            if (queryToken != null) {
                jwt = queryToken;
                tokenSource = "access_token query parameter";
                log.debug("Token found in query parameter");
            }
        }

        if (jwt == null) {
            log.warn("No JWT token found in any location for protected endpoint: {}", requestURI);
            log.warn("Checked: Authorization header, X-Auth-Token, X-Bearer-Token, X-JWT, access_token parameter");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"No authentication token found\"}");
            return;
        }

        log.debug("JWT token found via: {}, length: {}", tokenSource, jwt.length());

        try {
            // First, validate the token structure and expiration
            if (!jwtService.isTokenValid(jwt)) {
                log.warn("Invalid JWT token from {} for request: {}", tokenSource, requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                return;
            }

            // Extract mobile number from token
            String mobileNumber = jwtService.extractUsername(jwt);
            log.debug("Extracted mobile number from token: {}", mobileNumber);

            if (mobileNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Find wallet by mobile number
                Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(mobileNumber);

                if (walletOpt.isEmpty()) {
                    log.warn("Wallet not found for mobile number: {}", mobileNumber);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"User not found\"}");
                    return;
                }

                Wallet wallet = walletOpt.get();
                log.debug("Wallet found: {}", wallet.getWalletNumber());

                // Check if wallet is blocked
                if (wallet.getBlockAction() != null && wallet.getBlockAction() == 'Y') {
                    log.warn("Wallet is blocked: {}", wallet.getWalletNumber());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Account is blocked\"}");
                    return;
                }

                // Create UserDetails from wallet
                UserDetails userDetails = createUserDetailsFromWallet(wallet);

                // Validate token with user details
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    log.warn("Token validation failed for user: {}", mobileNumber);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token validation failed\"}");
                    return;
                }

                // Set authentication in security context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authentication set in SecurityContext for user: {} via {}", mobileNumber, tokenSource);

                // Store wallet information in request attributes
                request.setAttribute("walletNumber", wallet.getWalletNumber());
                request.setAttribute("bankCode", wallet.getBankCode());
                request.setAttribute("clientCode", wallet.getClientCode());
            }
        } catch (Exception e) {
            log.error("JWT validation error for request {}: {}", requestURI, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token validation failed: " + e.getMessage() + "\"}");
            return;
        }

        log.debug("JWT validation completed successfully, proceeding with request");
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the endpoint should be permitted without JWT validation
     * This matches the patterns in SecurityConfig
     */
    private boolean isPermittedEndpoint(String servletPath) {
        return servletPath.startsWith("/api/auth/") ||
                servletPath.equals("/api/wallet/list-secretQ") ||
                servletPath.equals("/api/otp/verify") ||
                servletPath.equals("/api/customer/register");
    }
    /**
     * Create a UserDetails object from a Wallet entity
     */
    private UserDetails createUserDetailsFromWallet(Wallet wallet) {
        return new User(
                wallet.getMobileNumber(),
                wallet.getWalletPin() != null ? wallet.getWalletPin() : "", // PIN as password
                wallet.getBlockAction() == null || wallet.getBlockAction() != 'Y', // isEnabled (not blocked)
                true, // accountNonExpired
                true, // credentialsNonExpired
                wallet.getBlockAction() == null || wallet.getBlockAction() != 'Y', // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_WALLET"))
        );
    }
}
