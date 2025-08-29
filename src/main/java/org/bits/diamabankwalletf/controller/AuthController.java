package org.bits.diamabankwalletf.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.bits.diamabankwalletf.auth.security.JwtService;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.exception.AccountLockedException;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.model.WalletDetails;
import org.bits.diamabankwalletf.repository.DeviceRepository;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.service.*;
import org.bits.diamabankwalletf.utils.IpAddressUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WalletRepository walletRepository;
    private final DeviceRepository deviceRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final IpAddressUtils ipAddressUtils;
    private final AuthService authService;
    private final DeviceService deviceService;
    private final BranchService branchService;
    private final NotificationService notificationService;
    private final ProviderService providerService;
    private final AdvertisementService advertisementService;
    private final EpsProfileService profileService;
    private final WalletService walletService;
    private final ProcessingService processingService;
    private final BankService bankService;
    private final WalletAuthService walletAuthService;
    private final PinExpiryService pinExpiryService;

    // Constants for better maintainability
    private static final String PERSONAL_CLIENT_TYPE = "P";
    private static final String WALLET_USER_TYPE = "C";
    private static final String SUCCESS_RESP_CODE = "000";
    private static final String OTP_REQUIRED_RESP_CODE = "831";
    private static final String BLOCKED_ACCOUNT_CODE = "805";
    private static final char BLOCKED_ACTION = 'Y';

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest, HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        final String ipAddress = ipAddressUtils.getClientIp(request);
        final String phoneNumber = authRequest.getPhoneNumber();

        log.info("Login attempt - phoneNumber=[{}], userAgent=[{}], ipAddress=[{}]",
                phoneNumber, userAgent, ipAddress);

        // Extract and clear PIN for security
        final String pin = authRequest.getPin();
        authRequest.setPin(null);

        // Version check
        ResponseEntity<AuthResponse> versionCheckResponse = authService.checkAppVersion(userAgent);
        if (versionCheckResponse != null) {
            log.info("Login failed: version check failed for phoneNumber=[{}]", phoneNumber);
            return versionCheckResponse;
        }

        // Find and validate wallet
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.warn("Login failed: wallet not found for phoneNumber=[{}]", phoneNumber);
            return createUnauthorizedResponse("User not found", null);
        }

        Wallet wallet = walletOpt.get();

        // Validate client type
        if (!isValidClientType(wallet)) {
            log.warn("Login failed: invalid client type for phoneNumber=[{}], clientType=[{}]",
                    phoneNumber, wallet.getClientType());
            return createUnauthorizedResponse(
                    "This app is for personal wallet users only",
                    "INVALID_CLIENT_TYPE"
            );
        }

        log.info("Wallet found for phoneNumber=[{}], bankCode=[{}]", phoneNumber, wallet.getBankCode());

        // Check if wallet is blocked
        if (isWalletBlocked(wallet)) {
            log.warn("Login failed: wallet blocked for phoneNumber=[{}]", phoneNumber);
            throw new AccountLockedException(BLOCKED_ACCOUNT_CODE);
        }

        // Verify PIN
        if (!walletAuthService.verifyPin(wallet, pin)) {
            return handleFailedPinVerification(wallet, phoneNumber);
        }

        log.info("PIN verification successful for phoneNumber=[{}]", phoneNumber);

        // Check PIN expiry and requirements
        ResponseEntity<?> pinCheckResponse = checkPinStatus(wallet, phoneNumber);
        if (pinCheckResponse != null) {
            return pinCheckResponse;
        }

        log.info("All PIN checks passed for phoneNumber=[{}], proceeding with normal login", phoneNumber);

        // Reset failed attempts on successful login
        walletAuthService.resetFailedAttempts(wallet);

        // Handle device verification
        return handleDeviceVerification(authRequest, wallet, ipAddress);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request, HttpServletRequest httpRequest) {
        final String phoneNumber = request.getPhoneNumber();
        log.info("OTP verification attempt for phoneNumber=[{}]", phoneNumber);

        if (!otpService.canCheckOtp(phoneNumber)) {
            log.warn("Too many OTP verification attempts for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthResponse(false, null, "Too many verification attempts, please try again later"));
        }

        if (!otpService.verifyOtp(phoneNumber, request.getOtp())) {
            log.warn("Invalid OTP for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse(false, null, "Invalid OTP"));
        }

        // Generate device ID if not provided
        String deviceId = Optional.ofNullable(request.getDeviceId())
                .orElseGet(deviceService::generateDeviceId);

        // Register the device
        boolean registered = deviceService.updateDeviceId(phoneNumber, deviceId);
        log.info("Device registration result for phoneNumber=[{}]: registered=[{}]", phoneNumber, registered);

        // Continue with login flow
        log.info("OTP verification successful, continuing with login flow for phoneNumber=[{}]", phoneNumber);
        String ipAddress = ipAddressUtils.getClientIp(httpRequest);
        return processLogin(phoneNumber, deviceId, ipAddress);
    }

    @PostMapping("/init-pin")
    public ResponseEntity<?> initPin(@RequestBody AuthRequest request) {
        final String phoneNumber = request.getPhoneNumber();
        Optional<Wallet> walletOpt = walletRepository.findByMobileNumber(phoneNumber);

        if (walletOpt.isEmpty()) {
            log.warn("Init PIN failed: wallet not found for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse(false, null, "User not found"));
        }

        Wallet wallet = walletOpt.get();

        // Check if PIN is already set
        if (wallet.getWalletPin() != null && !wallet.getWalletPin().isEmpty()) {
            log.warn("Init PIN failed: PIN already set for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse(false, null, "PIN already set"));
        }

        // Set PIN for first-time user
        boolean success = walletAuthService.updatePin(wallet, request.getPin());
        if (!success) {
            log.error("Init PIN failed: error setting PIN for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, null, "Error setting PIN"));
        }

        // Generate JWT token
        String token = jwtService.generateWalletToken(wallet, request.getDeviceId(), null);
        log.info("PIN initialized successfully for phoneNumber=[{}]", phoneNumber);

        return ResponseEntity.ok(new AuthResponse(true, token, "PIN set successfully"));
    }

    // Private helper methods

    private boolean isValidClientType(Wallet wallet) {
        return wallet.getClientType() != null && wallet.getClientType().toString().equals(PERSONAL_CLIENT_TYPE);
    }

    private boolean isWalletBlocked(Wallet wallet) {
        return wallet.getBlockAction() != null && wallet.getBlockAction() == BLOCKED_ACTION;
    }

    private ResponseEntity<?> handleFailedPinVerification(Wallet wallet, String phoneNumber) {
        boolean shouldBlock = walletAuthService.handleFailedLogin(wallet);
        log.warn("Login failed: invalid PIN for phoneNumber=[{}], blockRequired=[{}]", phoneNumber, shouldBlock);
        return createUnauthorizedResponse("Invalid credentials", null);
    }

    private ResponseEntity<?> checkPinStatus(Wallet wallet, String phoneNumber) {
        log.info("Checking PIN status for phoneNumber=[{}]: expiryDate=[{}], lastChanged=[{}], changeRequired=[{}], notificationSent=[{}]",
                phoneNumber, wallet.getPinExpiryDate(), wallet.getPinLastChangedDate(),
                wallet.getPinChangeRequired(), wallet.getPinExpiryNotificationSent());

        // Check PIN expiry
        boolean isPinExpired = pinExpiryService.isPinExpired(wallet);
        log.info("PIN expiry check for phoneNumber=[{}]: isPinExpired=[{}]", phoneNumber, isPinExpired);

        if (isPinExpired) {
            log.warn("Login failed: PIN expired for phoneNumber=[{}], expiryDate=[{}]",
                    phoneNumber, wallet.getPinExpiryDate());

            // Force PIN change if not already required
            if (!pinExpiryService.isPinChangeRequired(wallet)) {
                pinExpiryService.forcePinChange(wallet);
                log.info("PIN change forced for wallet: {}", wallet.getWalletNumber());
            }

            return createUnauthorizedResponse(
                    "Votre code PIN a expiré. Vous devez le modifier pour continuer.",
                    "PIN_EXPIRED",
                    true
            );
        }

        // Check if PIN change is required
        boolean isPinChangeRequired = pinExpiryService.isPinChangeRequired(wallet);
        log.info("PIN change required check for phoneNumber=[{}]: isPinChangeRequired=[{}]",
                phoneNumber, isPinChangeRequired);

        if (isPinChangeRequired) {
            log.warn("Login failed: PIN change required for phoneNumber=[{}]", phoneNumber);
            return createUnauthorizedResponse(
                    "Vous devez modifier votre code PIN avant de continuer.",
                    "PIN_CHANGE_REQUIRED",
                    true
            );
        }

        return null; // No PIN issues
    }

    private ResponseEntity<?> handleDeviceVerification(AuthRequest authRequest, Wallet wallet, String ipAddress) {
        final String phoneNumber = authRequest.getPhoneNumber();
        final String deviceId = authRequest.getDeviceId();
        final String storedDeviceId = deviceService.getDeviceId(phoneNumber);

        log.info("Device check: phoneNumber=[{}], requestDeviceId=[{}], storedDeviceId=[{}]",
                phoneNumber, deviceId, storedDeviceId);

        // Check if device verification is needed
        if (storedDeviceId == null || !storedDeviceId.equals(deviceId)) {
            return handleNewDevice(phoneNumber);
        }

        // Generate JWT token for authenticated wallet
        log.info("Generating wallet JWT token for phoneNumber=[{}]", phoneNumber);
        String token = jwtService.generateWalletToken(wallet, deviceId, ipAddress);
        log.info("JWT token generated successfully for phoneNumber=[{}]", phoneNumber);

        return buildWalletResponse(wallet, token, SUCCESS_RESP_CODE);
    }

    private ResponseEntity<?> handleNewDevice(String phoneNumber) {
        if (otpService.canSendOtp(phoneNumber)) {
            String otp = otpService.generateOtp(phoneNumber);

            log.info("OTP generated for new device, phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("OTP verification required")
                            .respCode(OTP_REQUIRED_RESP_CODE)
                            .userType(WALLET_USER_TYPE)
                            .build());
        } else {
            log.warn("Too many OTP requests for phoneNumber=[{}]", phoneNumber);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("Too many OTP requests, please try again later")
                            .respCode("TOO_MANY_REQUESTS")
                            .build());
        }
    }

    private ResponseEntity<?> processLogin(String phoneNumber, String deviceId, String ipAddress) {
        // Find wallet by phone number
        Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            log.warn("processLogin: wallet not found for phoneNumber=[{}]", phoneNumber);
            return createUnauthorizedResponse("User not found", null);
        }

        Wallet wallet = walletOpt.get();
        log.info("processLogin: wallet found for phoneNumber=[{}], bankCode=[{}]",
                phoneNumber, wallet.getBankCode());

        // Check if wallet is blocked
        if (isWalletBlocked(wallet)) {
            log.warn("processLogin: wallet blocked for phoneNumber=[{}]", phoneNumber);
            throw new AccountLockedException(BLOCKED_ACCOUNT_CODE);
        }

        // Generate JWT token
        log.info("processLogin: generating wallet JWT token for phoneNumber=[{}]", phoneNumber);
        String token = jwtService.generateWalletToken(wallet, deviceId, ipAddress);
        log.info("processLogin: wallet JWT token generated successfully for phoneNumber=[{}]", phoneNumber);

        return buildWalletResponse(wallet, token, SUCCESS_RESP_CODE);
    }

    private ResponseEntity<AuthResponse> buildWalletResponse(Wallet wallet, String token, String respCode) {
        final String phoneNumber = wallet.getMobileNumber();
        final String bankCode = wallet.getBankCode();
        final String walletNumber = wallet.getWalletNumber();

        log.info("Building response for wallet phoneNumber=[{}], walletNumber=[{}], bankCode=[{}]",
                phoneNumber, walletNumber, bankCode);

        // Get wallet details
        WalletDetails walletDetails = new WalletDetails(wallet);

        // Fetch all required data efficiently
        WalletResponseData responseData = fetchWalletResponseData(wallet, bankCode);

        log.info("Wallet login successful for phoneNumber=[{}], walletNumber=[{}]", phoneNumber, walletNumber);

        logResponseDataSizes(phoneNumber, responseData);

        return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .token(token)
                .message("Login successful")
                .respCode(respCode)
                .userCode(wallet.getClientCode())
                .phoneNumber(phoneNumber)
                .name(walletDetails.getFullName())
                .email(walletDetails.getEmail())
                .type("Wallet")
                .userType(WALLET_USER_TYPE)
                .walletnumber(walletNumber)
                .bankCode(bankCode)
                .bankWording(responseData.getBankWording())
                .branchCode(wallet.getBranchCode())
                .tokenTimeout(jwtService.getExpirationTime())
                .notificationList(responseData.getNotificationList())
                .QuestionsList(responseData.getWalletQuestionsList())
                .providerList(responseData.getProviderList())
                .airtimeProviderList(responseData.getAirtimeProviderList())
                .reasonList(responseData.getReasonList())
                .branchList(responseData.getBranchList())
                .processingCodes(responseData.getProcessingCodes())
                .ads(responseData.getAds())
                .adsDelay(responseData.getAdsDelay())
                .build());
    }

    private WalletResponseData fetchWalletResponseData(Wallet wallet, String bankCode) {
        log.info("Fetching response data for wallet phoneNumber=[{}], walletNumber=[{}]",
                wallet.getMobileNumber(), wallet.getWalletNumber());

        WalletResponseData.WalletResponseDataBuilder builder = WalletResponseData.builder();

        // Bank wording
        builder.bankWording(bankService.getBankWording(bankCode));

        // Notifications - use client code if available
        List<?> notificationList = wallet.getClientCode() != null ?
                notificationService.getNotificationList(wallet.getClientCode()) : List.of();
        builder.notificationList(notificationList);

        // Secret questions
        List<JSONObject> walletQuestionsList = fetchSecretQuestions();
        builder.walletQuestionsList(walletQuestionsList);

        // Various service lists
        builder.reasonList(walletService.getReasonList(bankCode, wallet.getClientCode()))
                .providerList(providerService.getProviderList())
                .airtimeProviderList(providerService.getAirtimeProviderList())
                .processingCodes(processingService.getProcessingCodes())
                .branchList(branchService.getAllBranches(bankCode));

        // Advertisement data
        List<?> ads = advertisementService.getAdsForBank(bankCode);
        int adsDelay = Integer.parseInt(profileService.getEpsProfile("ADS_DELAY_WALLET"));
        builder.ads(ads).adsDelay(adsDelay);

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<JSONObject> fetchSecretQuestions() {
        try {
            JResponseService secretQResponse = walletService.getWalletQuestionsList();

            if (secretQResponse != null && SUCCESS_RESP_CODE.equals(secretQResponse.getRespCode())) {
                List<JSONObject> questions = (List<JSONObject>) secretQResponse.getResult();
                log.info("Successfully fetched {} secret questions for wallet",
                        questions != null ? questions.size() : 0);
                return questions != null ? questions : List.of();
            } else {
                log.warn("Failed to fetch secret questions: {}",
                        secretQResponse != null ? secretQResponse.getMessage() : "Unknown error");
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error fetching secret questions for wallet: {}", e.getMessage());
            return List.of();
        }
    }

    private void logResponseDataSizes(String phoneNumber, WalletResponseData data) {
        log.debug("Response data sizes for phoneNumber=[{}]: notifications=[{}], questions=[{}], " +
                        "reasons=[{}], providers=[{}], airtimeProviders=[{}], processingCodes=[{}], " +
                        "branches=[{}], ads=[{}]",
                phoneNumber,
                safeSize(data.getNotificationList()),
                safeSize(data.getWalletQuestionsList()),
                safeSize(data.getReasonList()),
                safeSize(data.getProviderList()),
                safeSize(data.getAirtimeProviderList()),
                safeSize(data.getProcessingCodes()),
                safeSize(data.getBranchList()),
                safeSize(data.getAds()));
    }

    private int safeSize(List<?> list) {
        return list != null ? list.size() : 0;
    }

    private ResponseEntity<AuthResponse> createUnauthorizedResponse(String message, String respCode) {
        return createUnauthorizedResponse(message, respCode, false);
    }

    private ResponseEntity<AuthResponse> createUnauthorizedResponse(String message, String respCode, boolean requiresPinChange) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.builder()
                        .success(false)
                        .message(message)
                        .respCode(respCode)
                        .requiresPinChange(requiresPinChange)
                        .build());
    }

    // Inner class to hold response data
    @lombok.Data
    @lombok.Builder
    private static class WalletResponseData {
        private String bankWording;
        private List<?> notificationList;
        private List<JSONObject> walletQuestionsList;
        private List<?> reasonList;
        private List<?> providerList;
        private List<?> airtimeProviderList;
        private List<?> processingCodes;
        private List<Map<String, Object>> branchList;
        private List<?> ads;
        private int adsDelay;
    }
}
