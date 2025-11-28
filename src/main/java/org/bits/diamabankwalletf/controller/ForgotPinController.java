package org.bits.diamabankwalletf.controller;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.service.ForgotPinService;
import org.bits.diamabankwalletf.utils.IpAddressUtils;
import org.bits.diamabankwalletf.utils.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth/forgot-pin")
@RequiredArgsConstructor
public class ForgotPinController {

    private final ForgotPinService forgotPinService;
    private final IpAddressUtils ipAddressUtils;
    private final WalletRepository walletRepository;

    @PostMapping("/initiate")
    public ResponseEntity<ForgotPinResponse> initiateForgotPin(
              @RequestBody ForgotPinInitiateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipAddressUtils.getClientIp(httpRequest);
        log.info("Forgot PIN initiation request from IP: {}", ipAddress);

        ForgotPinResponse response = forgotPinService.initiateForgotPin(request, ipAddress);

        HttpStatus status = determineHttpStatus(response);
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ForgotPinResponse> verifyBackendOtp(@RequestBody OtpVerificationRequest request,
                                                              HttpServletRequest httpRequest) {
        String ipAddress = ipAddressUtils.getClientIp(httpRequest);
        log.info("Forwarding OTP verification to bank backend for phoneNumber={}, ip={}",
                SecurityUtils.maskPhoneNumber(request.getPhoneNumber()), ipAddress);

        ForgotPinResponse response = forgotPinService.verifyBackendOtp(request, ipAddress);

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }


    @PostMapping("/verify-question")
    public ResponseEntity<ForgotPinResponse> verifySecurityQuestion(
              @RequestBody SecurityQuestionRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipAddressUtils.getClientIp(httpRequest);
        log.info("Forgot PIN security question verification request from IP: {}", ipAddress);

        ForgotPinResponse response = forgotPinService.verifySecurityQuestion(request, ipAddress);

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<ForgotPinResponse> resetPin(
              @RequestBody PinResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ipAddressUtils.getClientIp(httpRequest);
        log.info("PIN reset request from IP: {}", ipAddress);

        ForgotPinResponse response = forgotPinService.resetPin(request, ipAddress);

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    // Helper method to determine HTTP status based on response code
    private HttpStatus determineHttpStatus(ForgotPinResponse response) {
        if (response.isSuccess()) {
            return HttpStatus.OK;
        }

        return switch (response.getRespCode()) {
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "ACCOUNT_BLOCKED" -> HttpStatus.FORBIDDEN;
            case "INVALID_SESSION", "INVALID_VERIFICATION", "INVALID_RESET_TOKEN" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @PostMapping("/check-eligibility")
    public ResponseEntity<ForgotPinResponse> checkEligibility(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String phoneNumber = request.get("phoneNumber");
        String ipAddress = ipAddressUtils.getClientIp(httpRequest);

        log.info("Checking forgot PIN eligibility for phoneNumber=[{}], IP=[{}]",
                org.bits.diamabankwalletf.utils.SecurityUtils.maskPhoneNumber(phoneNumber), ipAddress);

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ForgotPinResponse.builder()
                            .success(false)
                            .message("Phone number is required")
                            .respCode("VALIDATION_ERROR")
                            .build());
        }

        // Check if wallet exists
        Optional<Wallet> walletOpt = walletRepository.findByMobileNumber(phoneNumber);
        if (walletOpt.isEmpty()) {
            // Don't reveal that wallet doesn't exist - security measure
            return ResponseEntity.ok(ForgotPinResponse.builder()
                    .success(true)
                    .message("If this phone number is registered, you can proceed with forgot PIN")
                    .respCode("ELIGIBLE")
                    .build());
        }

        Wallet wallet = walletOpt.get();

        // Check if wallet is blocked
        if (wallet.getBlockAction() != null && wallet.getBlockAction() == 'Y') {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ForgotPinResponse.builder()
                            .success(false)
                            .message("Account is temporarily blocked. Please contact customer support")
                            .respCode("ACCOUNT_BLOCKED")
                            .build());
        }

        // Check client type (same logic as your AuthController)
        if (wallet.getClientType() == null || !wallet.getClientType().toString().equals("P")) {
            return ResponseEntity.badRequest()
                    .body(ForgotPinResponse.builder()
                            .success(false)
                            .message("This app is for personal wallet users only")
                            .respCode("INVALID_CLIENT_TYPE")
                            .build());
        }

        return ResponseEntity.ok(ForgotPinResponse.builder()
                .success(true)
                .message("You can proceed with forgot PIN process")
                .respCode("ELIGIBLE")
                .build());
    }

}
