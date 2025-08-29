package org.bits.diamabankwalletf.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.auth.config.JwtConfig;
import org.bits.diamabankwalletf.model.Wallet;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    // Constants for claim names
    private static final String DEVICE_ID_CLAIM = "deviceId";
    private static final String IP_ADDRESS_CLAIM = "ipAddress";
    private static final String WALLET_NUMBER_CLAIM = "walletNumber";
    private static final String CLIENT_CODE_CLAIM = "clientCode";
    private static final String BANK_CODE_CLAIM = "bankCode";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get(DEVICE_ID_CLAIM, String.class));
    }

    public String extractIpAddress(String token) {
        return extractClaim(token, claims -> claims.get(IP_ADDRESS_CLAIM, String.class));
    }

    public String extractWalletNumber(String token) {
        return extractClaim(token, claims -> claims.get(WALLET_NUMBER_CLAIM, String.class));
    }

    public String extractBankCode(String token) {
        return extractClaim(token, claims -> claims.get(BANK_CODE_CLAIM, String.class));
    }

    public String extractClientCode(String token) {
        return extractClaim(token, claims -> claims.get(CLIENT_CODE_CLAIM, String.class));
    }

    // Generate token for wallet authentication
    public String generateWalletToken(Wallet wallet, String deviceId, String ipAddress) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(DEVICE_ID_CLAIM, deviceId);
        claims.put(IP_ADDRESS_CLAIM, ipAddress);
        claims.put(WALLET_NUMBER_CLAIM, wallet.getWalletNumber());
        claims.put(CLIENT_CODE_CLAIM, wallet.getClientCode());
        claims.put(BANK_CODE_CLAIM, wallet.getBankCode());

        String token = Jwts
                .builder()
                .setClaims(claims)
                .setSubject(wallet.getMobileNumber())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated token for wallet: {}, length: {}", wallet.getWalletNumber(), token.length());
        return token;
    }


    public boolean isTokenValid(String token) {
        try {
            log.debug("=== SIMPLE TOKEN VALIDATION ===");
            log.debug("Validating token, length: {}", token.length());

            Claims claims = extractAllClaims(token);
            log.debug("Claims extracted successfully");

            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());

            log.debug("Token expiration: {}", expiration);
            log.debug("Current time: {}", new Date());
            log.debug("Token expired: {}", isExpired);

            if (isExpired) {
                log.warn("Token is expired");
                return false;
            }

            String phoneNumber = claims.getSubject();
            log.debug("Token subject (phone): {}", phoneNumber);

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("Token has no subject (phone number)");
                return false;
            }

            log.debug("Simple token validation successful");
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Malformed token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        } finally {
            log.debug("===============================");
        }
    }

    // Token validation with UserDetails
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            log.debug("=== TOKEN VALIDATION WITH USERDETAILS ===");
            final String username = extractUsername(token);
            boolean usernameMatches = username.equals(userDetails.getUsername());
            boolean notExpired = !isTokenExpired(token);

            log.debug("Username from token: {}", username);
            log.debug("Username from UserDetails: {}", userDetails.getUsername());
            log.debug("Username matches: {}", usernameMatches);
            log.debug("Token not expired: {}", notExpired);

            boolean isValid = usernameMatches && notExpired;
            log.debug("Token valid with UserDetails: {}", isValid);

            return isValid;
        } catch (Exception e) {
            log.error("Token validation with UserDetails error: {}", e.getMessage());
            return false;
        } finally {
            log.debug("=========================================");
        }
    }


    public boolean isTokenValid(String token, UserDetails userDetails, String deviceId) {
        if (!isTokenValid(token, userDetails)) {
            return false;
        }

        log.debug("=== ENHANCED TOKEN VALIDATION (WITH DEVICE) ===");
        // Optional device ID verification
        if (deviceId != null) {
            String tokenDeviceId = extractDeviceId(token);
            log.debug("Device ID from token: {}", tokenDeviceId);
            log.debug("Device ID from request: {}", deviceId);

            if (tokenDeviceId != null && !tokenDeviceId.equals(deviceId)) {
                log.warn("Device ID mismatch - token: {}, request: {}", tokenDeviceId, deviceId);
                return false;
            }
        }

        log.debug("Enhanced token validation successful");
        log.debug("==============================================");
        return true;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationTime() {
        return jwtConfig.getExpiration();
    }

    // Add this method to debug token creation
    public void debugTokenCreation(String token) {
        try {
            log.debug("=== DEBUG TOKEN CREATION ===");
            Claims claims = extractAllClaims(token);
            log.debug("Subject: {}", claims.getSubject());
            log.debug("Issued at: {}", claims.getIssuedAt());
            log.debug("Expiration: {}", claims.getExpiration());
            log.debug("Device ID: {}", claims.get(DEVICE_ID_CLAIM));
            log.debug("Wallet Number: {}", claims.get(WALLET_NUMBER_CLAIM));
            log.debug("Bank Code: {}", claims.get(BANK_CODE_CLAIM));
            log.debug("Client Code: {}", claims.get(CLIENT_CODE_CLAIM));
            log.debug("Token length: {}", token.length());
            log.debug("============================");
        } catch (Exception e) {
            log.error("Error debugging token: {}", e.getMessage());
        }
    }
}
