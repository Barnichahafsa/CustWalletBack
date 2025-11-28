package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.SmsLog;
import org.bits.diamabankwalletf.repository.SmsLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final Random random = new Random();
    private final SmsLogRepository smsLog;

    // Thread-safe data structures
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OtpGenerationInfo> generationInfo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> verificationAttempts = new ConcurrentHashMap<>();

    // Per-phone-number locks for fine-grained concurrency
    private final ConcurrentHashMap<String, Lock> phoneLocks = new ConcurrentHashMap<>();

    @Value("${otp.throttling.max-requests-per-hour}")
    private int maxOtpRequestsPerHour;

    @Value("${otp.throttling.min-seconds-between-requests}")
    private int minSecondsBetweenRequests;

    @Value("${otp.throttling.max-verification-attempts}")
    private int maxVerificationAttempts;

    @Value("${otp.throttling.expiry-minutes}")
    private int otpExpiryMinutes;

    /**
     * Stores OTP with timestamp for expiry checking
     */
    private static class OtpData {
        String otp;
        LocalDateTime createdAt;

        OtpData(String otp) {
            this.otp = otp;
            this.createdAt = LocalDateTime.now();
        }

        boolean isExpired(int expiryMinutes) {
            return createdAt.plusMinutes(expiryMinutes).isBefore(LocalDateTime.now());
        }
    }

    /**
     * Tracks OTP generation attempts for rate limiting
     */
    private static class OtpGenerationInfo {
        int count;
        LocalDateTime windowStart;
        LocalDateTime lastRequest;

        OtpGenerationInfo() {
            this.count = 0;
            this.windowStart = LocalDateTime.now();
            this.lastRequest = LocalDateTime.now();
        }

        void incrementCount() {
            this.count++;
            this.lastRequest = LocalDateTime.now();
        }

        void reset() {
            this.count = 0;
            this.windowStart = LocalDateTime.now();
            this.lastRequest = LocalDateTime.now();
        }

        boolean isWindowExpired() {
            return !windowStart.plusHours(1).isAfter(LocalDateTime.now());
        }
    }

    @PostConstruct
    public void init() {
        log.info("OTP Service initialized with configuration:");
        log.info("- Max requests per hour: {}", maxOtpRequestsPerHour);
        log.info("- Min seconds between requests: {}", minSecondsBetweenRequests);
        log.info("- Max verification attempts: {}", maxVerificationAttempts);
        log.info("- OTP expiry minutes: {}", otpExpiryMinutes);
    }

    /**
     * Get or create a lock for a specific phone number
     */
    private Lock getLock(String phoneNumber) {
        return phoneLocks.computeIfAbsent(phoneNumber, k -> new ReentrantLock());
    }

    /**
     * Check if OTP can be sent (public method for external checks)
     */
    public boolean canSendOtp(String phoneNumber) {
        Lock lock = getLock(phoneNumber);
        lock.lock();
        try {
            return canSendOtpLocked(phoneNumber);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if OTP can be verified (public method for external checks)
     */
    public boolean canCheckOtp(String phoneNumber) {
        log.info("Checking if OTP can be verified for: {}", phoneNumber);
        int attempts = verificationAttempts.getOrDefault(phoneNumber, 0);

        if (attempts >= maxVerificationAttempts) {
            log.warn("OTP verification rejected - max attempts exceeded for: {}", phoneNumber);
            return false;
        }

        return true;
    }

    /**
     * Generate OTP for a phone number with rate limiting
     */
    public String generateOtp(String phoneNumber) {
        Lock lock = getLock(phoneNumber);
        lock.lock();
        try {
            log.info("OTP generation requested for: {}", phoneNumber);

            // Check if OTP can be sent (rate limiting)
            if (!canSendOtpLocked(phoneNumber)) {
                log.warn("OTP generation rejected due to rate limiting for: {}", phoneNumber);
                return null;
            }

            // Update generation tracking
            OtpGenerationInfo info = generationInfo.computeIfAbsent(
                    phoneNumber,
                    k -> new OtpGenerationInfo()
            );
            info.incrementCount();

            // Generate 6-digit OTP
            String otp = String.format("%06d", random.nextInt(1000000));
            log.info("OTP generated for {}: {}", phoneNumber, otp);

            // Store OTP with timestamp
            otpStore.put(phoneNumber, new OtpData(otp));

            // Reset verification attempts for new OTP
            verificationAttempts.remove(phoneNumber);

            // Send SMS (async to avoid blocking)
            try {
                sendSms(phoneNumber, otp);
            } catch (Exception e) {
                log.error("Failed to send SMS for {}: {}", phoneNumber, e.getMessage());
                // OTP still valid even if SMS fails - user might retry
            }

            return otp;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if OTP can be sent (must be called within lock)
     */
    private boolean canSendOtpLocked(String phoneNumber) {
        log.info("Checking if OTP can be sent to: {}", phoneNumber);

        LocalDateTime now = LocalDateTime.now();
        OtpGenerationInfo info = generationInfo.get(phoneNumber);

        // First request - allow it
        if (info == null) {
            return true;
        }

        // Reset counter if hour window has expired
        if (info.isWindowExpired()) {
            log.info("Hour window expired, resetting OTP request counter for: {}", phoneNumber);
            info.reset();
            return true;
        }

        // Check minimum time between requests (rate limiting)
        long secondsSinceLastRequest = java.time.Duration.between(info.lastRequest, now).getSeconds();
        if (secondsSinceLastRequest < minSecondsBetweenRequests) {
            log.warn("OTP request rejected - too frequent for: {} - {} seconds since last request",
                    phoneNumber, secondsSinceLastRequest);
            return false;
        }

        // Check hourly limit
        if (info.count >= maxOtpRequestsPerHour) {
            log.warn("OTP request rejected - hourly limit ({}) exceeded for: {}",
                    maxOtpRequestsPerHour, phoneNumber);
            return false;
        }

        return true;
    }
    /**
     * Verify OTP for a phone number
     */
    public boolean verifyOtp(String phoneNumber, String otp) {
        log.info("OTP verification requested for: {}", phoneNumber);

        if (phoneNumber == null || otp == null) {
            log.warn("Invalid input - phoneNumber or OTP is null");
            return false;
        }

        // Atomically increment verification attempts
        int attempts = verificationAttempts.compute(
                phoneNumber,
                (key, val) -> (val == null) ? 1 : val + 1
        );

        // Check max attempts AFTER incrementing
        if (attempts > maxVerificationAttempts) {
            log.warn("OTP verification rejected - max attempts ({}) exceeded for: {}",
                    maxVerificationAttempts, phoneNumber);
            return false;
        }

        // Get stored OTP data
        OtpData otpData = otpStore.get(phoneNumber);
        if (otpData == null) {
            log.warn("No OTP found for: {}", phoneNumber);
            return false;
        }

        // Check if OTP has expired
        if (otpData.isExpired(otpExpiryMinutes)) {
            log.warn("OTP expired for: {} (created at: {})", phoneNumber, otpData.createdAt);
            otpStore.remove(phoneNumber);
            return false;
        }

        // Verify OTP
        boolean isValid = otpData.otp.equals(otp);

        if (isValid) {
            log.info("OTP verification successful for: {}", phoneNumber);
            removeOtpCounts(phoneNumber);
        } else {
            log.warn("OTP verification failed for: {} (attempt {}/{})",
                    phoneNumber, attempts, maxVerificationAttempts);
        }

        return isValid;
    }
    public void removeOtpCounts(String phoneNumber) {
        otpStore.remove(phoneNumber);
        verificationAttempts.remove(phoneNumber);
        // DON'T reset generation attempts so hourly limits are still enforced
    }

    /**
     * Send SMS with OTP
     */
    private void sendSms(String phoneNumber, String otp) {
        String smsMessage = String.format(
                "Bonjour, Code de verification : %s\n" +
                        "Utilisez ce code pour confirmer la connexion depuis votre nouveau appareil.\n" +
                        "Ne le partagez avec personne.",
                otp
        );

        SmsLog smsLogEntry = new SmsLog();
        smsLogEntry.setMsisdn(phoneNumber);
        smsLogEntry.setSenderModule("WB");
        smsLogEntry.setTimestamp(new Timestamp(System.currentTimeMillis()));
        smsLogEntry.setSmsStatus('N');
        smsLogEntry.setSmsText(smsMessage);

        try {
            smsLog.save(smsLogEntry);
            log.info("SMS queued successfully for: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to save SMS log for {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Scheduled cleanup of expired data (runs every 5 minutes)
     * Prevents memory leaks from abandoned OTP requests
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000) // 5 minutes
    public void cleanupExpiredData() {
        log.info("Starting scheduled cleanup of expired OTP data");

        LocalDateTime now = LocalDateTime.now();
        int removedOtps = 0;
        int removedGenInfo = 0;
        int removedVerifyAttempts = 0;
        int removedLocks = 0;

        try {
            // Clean expired OTPs
            for (Map.Entry<String, OtpData> entry : otpStore.entrySet()) {
                if (entry.getValue().isExpired(otpExpiryMinutes)) {
                    otpStore.remove(entry.getKey());
                    removedOtps++;
                }
            }

            // Clean old generation info (older than 2 hours)
            for (Map.Entry<String, OtpGenerationInfo> entry : generationInfo.entrySet()) {
                if (entry.getValue().windowStart.plusHours(2).isBefore(now)) {
                    generationInfo.remove(entry.getKey());
                    removedGenInfo++;
                }
            }

            // Clean old verification attempts (older than 1 hour)
            // This is safe because OTP expires after otpExpiryMinutes anyway
            for (String phoneNumber : verificationAttempts.keySet()) {
                OtpData otpData = otpStore.get(phoneNumber);
                if (otpData == null || otpData.createdAt.plusHours(1).isBefore(now)) {
                    verificationAttempts.remove(phoneNumber);
                    removedVerifyAttempts++;
                }
            }

            // Clean unused locks (no activity in 2 hours)
            for (String phoneNumber : phoneLocks.keySet()) {
                OtpGenerationInfo info = generationInfo.get(phoneNumber);
                if (info == null || info.lastRequest.plusHours(2).isBefore(now)) {
                    phoneLocks.remove(phoneNumber);
                    removedLocks++;
                }
            }

            log.info("Cleanup completed - Removed: {} OTPs, {} generation info, {} verification attempts, {} locks",
                    removedOtps, removedGenInfo, removedVerifyAttempts, removedLocks);

            // Log current memory usage
            logMemoryUsage();

        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Log current memory usage for monitoring
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 600000) // 10 minutes
    public void logMemoryUsage() {
        log.info("OTP Service Memory Usage - OTPs: {}, Generation Info: {}, Verification Attempts: {}, Locks: {}",
                otpStore.size(), generationInfo.size(), verificationAttempts.size(), phoneLocks.size());
    }

    /**
     * Get current statistics (for monitoring/debugging)
     */
    public Map<String, Integer> getStatistics() {
        return Map.of(
                "activeOtps", otpStore.size(),
                "generationTracking", generationInfo.size(),
                "verificationTracking", verificationAttempts.size(),
                "activeLocks", phoneLocks.size()
        );
    }

    /**
     * Manual cleanup for specific phone number (for admin use)
     */
    public void forceCleanup(String phoneNumber) {
        log.info("Force cleanup requested for: {}", phoneNumber);
        otpStore.remove(phoneNumber);
        generationInfo.remove(phoneNumber);
        verificationAttempts.remove(phoneNumber);
        phoneLocks.remove(phoneNumber);
        log.info("Force cleanup completed for: {}", phoneNumber);
    }
}
