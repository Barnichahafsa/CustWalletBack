package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.ForgotPinAttempt;
import org.bits.diamabankwalletf.model.SecretQuestion;
import org.bits.diamabankwalletf.model.SmsLog;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.ForgotPinAttemptRepository;
import org.bits.diamabankwalletf.repository.SecretQuestionRepository;
import org.bits.diamabankwalletf.repository.SmsLogRepository;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.bits.diamabankwalletf.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPinService {

    private final ForgotPinAttemptRepository forgotPinAttemptRepository;
    private final WalletRepository walletRepository;
    private final OtpService otpService;
    private final WalletAuthService walletAuthService;
    private final SecurityQuestionService securityQuestionService;
    private final SmsLogRepository smsLogRepository;
    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final SecretQuestionRepository secretQuestionRepository;

    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.endpoints.reset-pin:/ResetPin}")
    private String resetPinEndpoint;
    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    // Configuration constants
    private static final int MAX_DAILY_ATTEMPTS = 3;
    private static final int MAX_HOURLY_ATTEMPTS_PER_IP = 10;
    private static final int SESSION_TOKEN_VALIDITY_MINUTES = 10;
    private static final int VERIFICATION_TOKEN_VALIDITY_MINUTES = 15;
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 10;

    @Transactional
    public ForgotPinResponse initiateForgotPin(ForgotPinInitiateRequest request, String ipAddress) {
        final String phoneNumber = request.getPhoneNumber();

        log.info("Initiating forgot PIN for phoneNumber=[{}], ipAddress=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber), ipAddress);

        // Rate limiting checks
        if (!canInitiateForgotPin(phoneNumber, ipAddress)) {
            log.warn("Rate limit exceeded for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return buildRateLimitResponse();
        }

        // Find and validate wallet
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.warn("Forgot PIN attempted for non-existent phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("If this phone number is registered, you will receive an OTP shortly")
                    .respCode("PROCESSING")
                    .build();
        }

        Wallet wallet = walletOpt.get();

        // Check if wallet is blocked
        if (wallet.getBlockAction() != null && wallet.getBlockAction() == 'Y') {
            log.warn("Forgot PIN attempted for blocked wallet - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Account is temporarily blocked. Please contact customer support")
                    .respCode("ACCOUNT_BLOCKED")
                    .build();
        }

        // Generate session token and create attempt record
        String sessionToken = SecurityUtils.generateSecureToken();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(SESSION_TOKEN_VALIDITY_MINUTES);

        ForgotPinAttempt attempt = ForgotPinAttempt.builder()
                .walletNumber(wallet.getWalletNumber())
                .phoneNumber(phoneNumber)
                .attemptTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .status(ForgotPinAttempt.ForgotPinStatus.INITIATED)
                .sessionToken(sessionToken)
                .tokenExpiry(expiry)
                .build();

        forgotPinAttemptRepository.save(attempt);

        // Fetch the user's security question
        Map<String, Object> questionMap = null;
        try {


            if (wallet != null && wallet.getSecretQuestion() != null) {
                SecretQuestion securityQuestion = secretQuestionRepository
                        .findById(wallet.getSecretQuestion())
                        .orElse(null);

                if (securityQuestion != null) {
                    questionMap = new HashMap<>();
                    questionMap.put("id", securityQuestion.getId());
                    questionMap.put("question", securityQuestion.getQuestion());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching security question for phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber), e);
        }

        return ForgotPinResponse.builder()
                .success(true)
                .message("Please answer your security question to proceed")
                .respCode("000")  // Changed from PROCEED_TO_BACKEND_OTP
                .sessionToken(sessionToken)
                .question(questionMap)  // Add the security question here
                .build();
    }
    @Transactional
    public ForgotPinResponse verifyOtpForForgotPin(ForgotPinVerifyRequest request, String ipAddress) {
        final String phoneNumber = request.getPhoneNumber();

        log.info("Verifying OTP for forgot PIN - phoneNumber=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber));

        // Find active attempt
        Optional<ForgotPinAttempt> attemptOpt = forgotPinAttemptRepository
                .findBySessionTokenAndPhoneNumber(request.getSessionToken(), phoneNumber);

        if (attemptOpt.isEmpty() || isTokenExpired(attemptOpt.get().getTokenExpiry())) {
            log.warn("Invalid or expired session token for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Invalid or expired session. Please start over")
                    .respCode("INVALID_SESSION")
                    .build();
        }

        ForgotPinAttempt attempt = attemptOpt.get();

        // Verify OTP
        if (!otpService.verifyOtp(phoneNumber, request.getOtp())) {
            attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.FAILED);
            attempt.setFailureReason("INVALID_OTP");
            forgotPinAttemptRepository.save(attempt);

            log.warn("Invalid OTP for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Invalid OTP. Please try again")
                    .respCode("INVALID_OTP")
                    .build();
        }

        // Find wallet and get security question
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.error("Wallet not found after OTP verification - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("System error. Please try again later")
                    .respCode("SYSTEM_ERROR")
                    .build();
        }

        Wallet wallet = walletOpt.get();

        // Check if wallet has security question
        if (!securityQuestionService.hasSecurityQuestion(wallet)) {
            log.warn("Wallet has no security question set - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("No security question set for this account. Please contact customer support")
                    .respCode("NO_SECURITY_QUESTION")
                    .build();
        }

        // Generate verification token for next step
        String verificationToken = SecurityUtils.generateSecureToken();
        LocalDateTime verificationExpiry = LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_VALIDITY_MINUTES);

        attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.OTP_VERIFIED);
        attempt.setVerificationToken(verificationToken);
        attempt.setTokenExpiry(verificationExpiry);
        forgotPinAttemptRepository.save(attempt);

        // Get security question
        Object question = securityQuestionService.getSecurityQuestion(wallet);

        log.info("OTP verified successfully for forgot PIN - phoneNumber=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber));

        return ForgotPinResponse.builder()
                .success(true)
                .message("OTP verified. Please answer your security question")
                .respCode("OTP_VERIFIED")
                .verificationToken(verificationToken)
                .question(question)
                .build();
    }

    @Transactional
    public ForgotPinResponse verifySecurityQuestion(SecurityQuestionRequest request, String ipAddress) {
        final String phoneNumber = request.getPhoneNumber();

        log.info("Verifying security question for forgot PIN - phoneNumber=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber));

        // Find active attempt
        Optional<ForgotPinAttempt> attemptOpt = forgotPinAttemptRepository
                .findByVerificationTokenAndPhoneNumber(request.getVerificationToken(), phoneNumber);

        if (attemptOpt.isEmpty() || isTokenExpired(attemptOpt.get().getTokenExpiry())) {
            log.warn("Invalid or expired verification token for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Invalid or expired verification token. Please start over")
                    .respCode("INVALID_VERIFICATION")
                    .build();
        }

        ForgotPinAttempt attempt = attemptOpt.get();

        // Find wallet
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.error("Wallet not found for security question verification - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("System error. Please try again later")
                    .respCode("SYSTEM_ERROR")
                    .build();
        }

        Wallet wallet = walletOpt.get();

        // Verify security question answer
        boolean answerValid = securityQuestionService.verifySecurityAnswer(wallet, request.getAnswer());

        if (!answerValid) {
            attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.FAILED);
            attempt.setFailureReason("INVALID_SECURITY_ANSWER");
            forgotPinAttemptRepository.save(attempt);

            log.warn("Invalid security answer for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Security answer is incorrect. Please try again")
                    .respCode("INVALID_ANSWER")
                    .build();
        }

        // Generate reset token
        String resetToken = SecurityUtils.generateSecureToken();
        LocalDateTime resetExpiry = LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES);

        attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.QUESTIONS_VERIFIED);
        attempt.setResetToken(resetToken);
        attempt.setTokenExpiry(resetExpiry);
        forgotPinAttemptRepository.save(attempt);

        log.info("Security question verified successfully for forgot PIN - phoneNumber=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber));

        return ForgotPinResponse.builder()
                .success(true)
                .message("Verification successful. You can now set a new PIN")
                .respCode("VERIFICATION_COMPLETE")
                .resetToken(resetToken)
                .build();
    }


    @Transactional
    public ForgotPinResponse resetPin(PinResetRequest request, String ipAddress) {
        final String phoneNumber = request.getPhoneNumber();

        log.info("Calling backend ResetPin API for phoneNumber=[{}]",
                SecurityUtils.maskPhoneNumber(phoneNumber));

        // Validate PIN match
        if (!request.getNewPin().equals(request.getConfirmPin())) {
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("PIN confirmation does not match")
                    .respCode("PIN_MISMATCH")
                    .build();
        }

        // Validate PIN strength
        if (!SecurityUtils.isValidPin(request.getNewPin())) {
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("PIN is too weak. Avoid sequential or repeated digits")
                    .respCode("WEAK_PIN")
                    .build();
        }

        // Find active attempt
        Optional<ForgotPinAttempt> attemptOpt = forgotPinAttemptRepository
                .findByResetTokenAndPhoneNumber(request.getResetToken(), phoneNumber);

        if (attemptOpt.isEmpty() || isTokenExpired(attemptOpt.get().getTokenExpiry())) {
            log.warn("Invalid or expired reset token for forgot PIN - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("Invalid or expired reset token. Please start over")
                    .respCode("INVALID_RESET_TOKEN")
                    .build();
        }

        ForgotPinAttempt attempt = attemptOpt.get();

        // Find wallet
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.error("Wallet not found for PIN reset - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber));
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("System error. Please try again later")
                    .respCode("SYSTEM_ERROR")
                    .build();
        }

        Wallet wallet = walletOpt.get();

        try {
            // Call backend ResetPin API which will send OTP
            ResponseService backendResponse = callBackendResetPinAPI(wallet, request.getNewPin());

            if (backendResponse != null && "000".equals(backendResponse.getRespCode())) {
                // Backend sent OTP successfully
                log.info("Backend ResetPin API called successfully, OTP sent for phoneNumber=[{}]",
                        SecurityUtils.maskPhoneNumber(phoneNumber));

                // Update our attempt status to show we're waiting for backend OTP
                attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.COMPLETED);
                attempt.setFailureReason("BACKEND_OTP_SENT"); // Use failure reason field to track this
                forgotPinAttemptRepository.save(attempt);

                return ForgotPinResponse.builder()
                        .success(true)
                        .message("PIN reset initiated. Please verify the OTP sent to your phone to complete the process")
                        .respCode("OTP_SENT_BY_BACKEND")
                        .authCode(backendResponse.getAuthCode())
                        .build();

            } else {
                log.error("Backend ResetPin API failed - phoneNumber=[{}], response=[{}]",
                        SecurityUtils.maskPhoneNumber(phoneNumber), backendResponse);

                attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.FAILED);
                attempt.setFailureReason("BACKEND_API_FAILED");
                forgotPinAttemptRepository.save(attempt);

                return ForgotPinResponse.builder()
                        .success(false)
                        .message("Failed to reset PIN. Please try again later")
                        .respCode("PIN_UPDATE_FAILED")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error calling backend ResetPin API - phoneNumber=[{}]",
                    SecurityUtils.maskPhoneNumber(phoneNumber), e);

            attempt.setStatus(ForgotPinAttempt.ForgotPinStatus.FAILED);
            attempt.setFailureReason("API_CALL_EXCEPTION");
            forgotPinAttemptRepository.save(attempt);

            return ForgotPinResponse.builder()
                    .success(false)
                    .message("System error. Please try again later")
                    .respCode("SYSTEM_ERROR")
                    .build();
        }
    }


    private ResponseService callBackendResetPinAPI(Wallet wallet, String newPin) {
        try {
            // Create RequestResetPin object1
            RequestResetPin resetPinRequest = new RequestResetPin();
            resetPinRequest.setWalletNumber(wallet.getWalletNumber());
            resetPinRequest.setPhoneNumber(wallet.getMobileNumber());
            resetPinRequest.setBank(wallet.getBankCode());

            // Encrypt the new PIN
            String encryptedPin = pinEncryptionUtil.encryptPin(newPin, wallet.getBankCode());
            resetPinRequest.setNewPinCode(encryptedPin);
            resetPinRequest.setConfPinCode(encryptedPin);

            // Set document details from wallet
            resetPinRequest.setDocCode(wallet.getDocumentCode() != null ? wallet.getDocumentCode() : "NI");
            resetPinRequest.setDocId(wallet.getDocumentId());

            // Generate requestId and requestDate
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new java.util.Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            resetPinRequest.setRequestId(datePrefix + randomDigits);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            resetPinRequest.setRequestDate(dateFormat.format(new java.util.Date()));

            resetPinRequest.setEntityId("CUSTOMER");

            // Call backend API using WalletService pattern
            String url = walletBackendUrl + resetPinEndpoint;
            String token = walletCreationService.getServiceAccountToken();

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(resetPinRequest)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling backend ResetPin API", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling backend ResetPin API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Exception in callBackendResetPinAPI", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Exception calling backend API: " + e.getMessage());
            return errorResponse;
        }
    }

    private boolean canInitiateForgotPin(String phoneNumber, String ipAddress) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long dailyAttempts = forgotPinAttemptRepository.countAttemptsSince(phoneNumber, oneDayAgo);
        long hourlyIpAttempts = forgotPinAttemptRepository.countAttemptsByIpSince(ipAddress, oneHourAgo);

        return dailyAttempts < MAX_DAILY_ATTEMPTS && hourlyIpAttempts < MAX_HOURLY_ATTEMPTS_PER_IP;
    }

    private ForgotPinResponse buildRateLimitResponse() {
        LocalDateTime tomorrow = LocalDateTime.now().plusHours(24);
        long retryAfterMs = java.time.Duration.between(LocalDateTime.now(), tomorrow).toMillis();

        return ForgotPinResponse.builder()
                .success(false)
                .message("Too many attempts today. Please try again tomorrow")
                .respCode("RATE_LIMIT_EXCEEDED")
                .retryAfter(retryAfterMs)
                .build();
    }

    private boolean isTokenExpired(LocalDateTime expiry) {
        return expiry != null && LocalDateTime.now().isAfter(expiry);
    }



    @Transactional
    public ForgotPinResponse verifyBackendOtp(OtpVerificationRequest request, String ipAddress) {
        try {
            RequestCheckOtp backendReq = new RequestCheckOtp();
            backendReq.setPhoneNumber(request.getPhoneNumber());
            backendReq.setOtp(request.getOtp());
            backendReq.setAuthCode(request.getDeviceId());   // comes from ResetPin response
            backendReq.setRequestId(SecurityUtils.generateRequestId());
            backendReq.setRequestDate(SecurityUtils.getCurrentTimestamp());
            backendReq.setEntityId("CUSTOMER");

            String url = walletBackendUrl + "/CheckOtp";
            String token = walletCreationService.getServiceAccountToken();

            ResponseServiceJson backendResp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(backendReq)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .block();

            if (backendResp != null && "000".equals(backendResp.getRespCode())) {
                log.info("OTP verified successfully for {}", SecurityUtils.maskPhoneNumber(request.getPhoneNumber()));
                return ForgotPinResponse.builder()
                        .success(true)
                        .message("OTP verified successfully")
                        .respCode("OTP_VERIFIED")
                        .build();
            } else {
                return ForgotPinResponse.builder()
                        .success(false)
                        .message("Invalid OTP")
                        .respCode("INVALID_OTP")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error verifying OTP with backend", e);
            return ForgotPinResponse.builder()
                    .success(false)
                    .message("System error. Please try again later")
                    .respCode("SYSTEM_ERROR")
                    .build();
        }
    }


}
