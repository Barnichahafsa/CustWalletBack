package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.SmsLog;
import org.bits.diamabankwalletf.repository.SmsLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final Random random = new Random();

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final SmsLogRepository smsLog;

    // OTP generation attempts
    private final Map<String, Integer> otpGenerationAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastOtpRequest = new ConcurrentHashMap<>();

    // OTP verification attempts
    private final Map<String, Integer> otpVerificationAttempts = new ConcurrentHashMap<>();

    // Configuration properties
    @Value("${otp.throttling.max-requests-per-hour}")
    private int maxOtpRequestsPerHour;

    @Value("${otp.throttling.min-seconds-between-requests}")
    private int minSecondsBetweenRequests;

    @Value("${otp.throttling.max-verification-attempts}")
    private int maxVerificationAttempts;

    @Value("${otp.throttling.expiry-minutes}")
    private int otpExpiryMinutes;


    public boolean canSendOtp(String phoneNumber) {
        log.info("Checking if OTP can be sent to: {}", phoneNumber);

        // One request per minute
        LocalDateTime lastRequest = lastOtpRequest.getOrDefault(phoneNumber, LocalDateTime.MIN);
        if (lastRequest.plusSeconds(minSecondsBetweenRequests).isAfter(LocalDateTime.now())) {
            log.warn("OTP request rejected - too frequent for: {}", phoneNumber);
            return false;
        }

        // Hourly limit
        int attempts = otpGenerationAttempts.getOrDefault(phoneNumber, 0);

        // COUNTEEER RESET if it's been over an hour since first request
        if (lastRequest.plusHours(1).isBefore(LocalDateTime.now())) {
            log.info("Resetting OTP request counter for: {}", phoneNumber);
            attempts = 0;
        }

        if (attempts >= maxOtpRequestsPerHour) {
            log.warn("OTP request rejected - hourly limit exceeded for: {}", phoneNumber);
            return false;
        }

        return true;
    }


    public boolean canCheckOtp(String phoneNumber) {
        log.info("Checking if OTP can be verified for: {}", phoneNumber);
        int attempts = otpVerificationAttempts.getOrDefault(phoneNumber, 0);

        if (attempts >= maxVerificationAttempts) {
            log.warn("OTP verification rejected - max attempts exceeded for: {}", phoneNumber);
            return false;
        }

        return true;
    }


    public String generateOtp(String phoneNumber) {
        if (!canSendOtp(phoneNumber)) {
            return null;
        }

        int attempts = otpGenerationAttempts.getOrDefault(phoneNumber, 0);
        otpGenerationAttempts.put(phoneNumber, attempts + 1);
        lastOtpRequest.put(phoneNumber, LocalDateTime.now());

        // 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));
        log.info("Generated OTP for {}: {}", phoneNumber, otp);

        otpStore.put(phoneNumber, otp);

        String smsMessage = String.format(
                "Code de verification : %s\n" +
                        "Utilisez ce code pour confirmer la connexion depuis votre nouveau appareil.\n" +
                        "Ne le partagez avec personne.",
                otp
        );

        SmsLog log = new SmsLog();
        log.setMsisdn(phoneNumber);
        log.setSenderModule("WB");
        log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSmsStatus('N');
        log.setSmsText(smsMessage);
        smsLog.save(log);

        return otp;
    }


    public boolean verifyOtp(String phoneNumber, String otp) {
        if (!canCheckOtp(phoneNumber)) {
            return false;
        }

        int attempts = otpVerificationAttempts.getOrDefault(phoneNumber, 0);
        otpVerificationAttempts.put(phoneNumber, attempts + 1);
        String storedOtp = otpStore.get(phoneNumber);
        if (storedOtp == null) {
            log.warn("No OTP found for: {}", phoneNumber);
            return false;
        }
        LocalDateTime requestTime = lastOtpRequest.get(phoneNumber);
        if (requestTime.plusMinutes(otpExpiryMinutes).isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for: {}", phoneNumber);
            otpStore.remove(phoneNumber);
            return false;
        }

        boolean isValid = storedOtp.equals(otp);
        log.info("OTP verification for {}: {}", phoneNumber, isValid ? "successful" : "failed");

        if (isValid) {
            removeOtpCounts(phoneNumber);
        }

        return isValid;
    }


    public void removeOtpCounts(String phoneNumber) {
        otpStore.remove(phoneNumber);
        otpVerificationAttempts.remove(phoneNumber);
        // NOO reset generation attempts so hourly limits are still enforced
    }
}
