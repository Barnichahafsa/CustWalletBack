package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTransferService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.wallet-to-wallet}")
    private String walletToWalletEndpoint;

    @Value("${wallet.backend.endpoints.wallet-to-account}")
    private String walletToAccountEndpoint;

    @Value("${wallet.backend.endpoints.wallet-to-account-ne}")
    private String walletToAccountNeEndpoint;

    @Value("${wallet.backend.endpoints.account-to-wallet}")
    private String accountToWalletEndpoint;

    @Value("${wallet.backend.endpoints.mobile-money-ne}")
    private String mobileMoneyNeEndpoint;


    @Value("${wallet.backend.endpoints.qrpayment}")
    private String qrTransferEndpoint;

    private final WalletRepository walletRepository;


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${wallet.backend.endpoints.wallet-limits}")
    private String walletLimitsEndpoint;

    @Value("${wallet.backend.endpoints.initiateDsd}")
    private String initiatepayment;

    public ResponseService initiateBillDsd(RequestInitiateDSD request) {
        try {
            String url = walletBackendUrl + initiatepayment;
            log.info("Calling billpayment endpoint at: {}", url);


            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Backend request body: {}", request);

            // Make the API call
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling bill payment API", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling bill payment API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Error calling bill payment API", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling bill payment API: " + e.getMessage());
            return errorResponse;
        }
    }


    public ResponseGetWalletLimits getWalletLimits(RequestGetWalletLimits request) {
        try {
            String url = walletBackendUrl + walletLimitsEndpoint;
            log.info("Calling wallet limits endpoint at: {}", url);

            // Normalize wallet request - lookup wallet by phone if needed
            String actualWalletNumber = request.getWalletNumber();

            if ((actualWalletNumber == null || actualWalletNumber.isEmpty()) &&
                    request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {

                log.info("Wallet number not provided, looking up by phone: {}", request.getPhoneNumber());
                actualWalletNumber = lookupActiveWalletByPhone(request.getPhoneNumber());

                if (actualWalletNumber == null) {
                    ResponseGetWalletLimits errorResponse = new ResponseGetWalletLimits();
                    errorResponse.setStatus("NOK");
                    errorResponse.setRespCode("404");
                    errorResponse.setMessage("No active wallet found for the provided phone number");
                    return errorResponse;
                }
            }

            // Validate we have a wallet number
            if (actualWalletNumber == null || actualWalletNumber.isEmpty()) {
                ResponseGetWalletLimits errorResponse = new ResponseGetWalletLimits();
                errorResponse.setStatus("NOK");
                errorResponse.setRespCode("400");
                errorResponse.setMessage("Wallet number or phone number is required");
                return errorResponse;
            }

            // Create request for backend API
            RequestGetWalletLimits backendRequest = new RequestGetWalletLimits();
            backendRequest.setBank("00100");
            backendRequest.setWalletNumber(actualWalletNumber);

            // Get authentication token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Backend request body: {}", backendRequest);

            // Make the API call
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(backendRequest)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseGetWalletLimits.class)
                    .doOnError(error -> log.error("Error calling wallet limits API", error))
                    .onErrorResume(error -> {
                        ResponseGetWalletLimits errorResponse = new ResponseGetWalletLimits();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet limits API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Error calling wallet limits API", e);
            ResponseGetWalletLimits errorResponse = new ResponseGetWalletLimits();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet limits API: " + e.getMessage());
            return errorResponse;
        }
    }
    public FeeCalculationResponse calculateFee(FeeCalculationRequest request) {
        FeeCalculationResponse response = new FeeCalculationResponse();
        response.setRequestId(request.getRequestId());

        try {
            String sql = "SELECT CALCULATE_TRANSACTION_FEE(?, ?, ?, ?, ?, ?, ?, ?, ?) as fee_amount FROM dual";

            Double feeAmount = jdbcTemplate.queryForObject(sql, Double.class,
                    request.getProcessingCode(),
                    request.getBankCode(),
                    request.getActionCode(),
                    request.getCurrencyCode(),
                    request.getTransactionAmount(),
                    request.getWalletProductCode(),
                    request.getWalletType(),
                    request.getOrigin(),
                    request.getMessageType()
            );

            if (feeAmount != null && feeAmount > 0) {
                response.setRespCode("000");
                response.setMessage("Fee calculated successfully");
                response.setFeeAmount(feeAmount);
            } else {
                response.setRespCode("001");
                response.setMessage("No fee applicable");
                response.setFeeAmount(0.0);
            }

        } catch (Exception e) {
            log.error("Error in simple fee calculation", e);
            response.setRespCode("999");
            response.setMessage("Calculation failed: " + e.getMessage());
            response.setFeeAmount(0.0);
        }

        return response;
    }

    private String lookupActiveWalletByPhone(String phoneNumber) {
        try {
            log.debug("Looking up wallet for phone number: {}", phoneNumber);
            Optional<Wallet> wallet = walletRepository.findByPhoneNumberAndBankCode(phoneNumber, "00100");
            if (wallet.isEmpty()) {
                wallet = walletRepository.findByMobileNumber(phoneNumber);
            }
            if (wallet.isPresent()) {
                Wallet foundWallet = wallet.get();
                if ("N".equals(foundWallet.getStatusWallet())) {
                    log.info("Found active wallet: {} for phone: {}",
                            foundWallet.getWalletNumber(), phoneNumber);
                    return foundWallet.getWalletNumber();
                } else {
                    log.warn("Found wallet {} for phone {} but status is: {}",
                            foundWallet.getWalletNumber(), phoneNumber, foundWallet.getStatusWallet());
                    return null;
                }
            }

            log.warn("No wallet found for phone number: {}", phoneNumber);
            return null;

        } catch (Exception e) {
            log.error("Error looking up wallet for phone number: {}", phoneNumber, e);
            return null;
        }
    }
    private String lookupActiveWalletByClientId(String clientId) {
        try {
            log.debug("Looking up wallet for client ID: {}", clientId);
            Optional<Wallet> wallet = walletRepository.findByClientCodeAndBankCode(clientId, "00100");

            if (wallet.isPresent()) {
                Wallet foundWallet = wallet.get();
                if ("N".equals(foundWallet.getStatusWallet())) {
                    log.info("Found active wallet: {} for client ID: {}",
                            foundWallet.getWalletNumber(), clientId);
                    return foundWallet.getWalletNumber();
                } else {
                    log.warn("Found wallet {} for client ID {} but status is: {}",
                            foundWallet.getWalletNumber(), clientId, foundWallet.getStatusWallet());
                    return null;
                }
            }

            log.warn("No wallet found for client ID: {}", clientId);
            return null;

        } catch (Exception e) {
            log.error("Error looking up wallet for client ID: {}", clientId, e);
            return null;
        }
    }
    private String lookupActiveWalletByPhoneOrClientId(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            log.warn("Empty identifier provided for wallet lookup");
            return null;
        }

        try {
            log.debug("Looking up wallet for identifier: {}", identifier);

            // First try by phone number
            String walletByPhone = lookupActiveWalletByPhone(identifier);
            if (walletByPhone != null) {
                log.info("Found wallet by phone number: {}", walletByPhone);
                return walletByPhone;
            }

            // If not found, try by client ID
            log.info("Wallet not found by phone, trying client ID lookup for: {}", identifier);
            String walletByClientId = lookupActiveWalletByClientId(identifier);
            if (walletByClientId != null) {
                log.info("Found wallet by client ID: {}", walletByClientId);
                return walletByClientId;
            }

            log.warn("No wallet found for identifier (tried phone and client ID): {}", identifier);
            return null;

        } catch (Exception e) {
            log.error("Error looking up wallet for identifier: {}", identifier, e);
            return null;
        }
    }

    private String[] normalizeWalletRequest(String originalSource, String walletNumber, String phoneNumber) {
        if ("P".equals(originalSource) &&
                (walletNumber == null || walletNumber.isEmpty()) &&
                phoneNumber != null && !phoneNumber.isEmpty()) {

            log.info("Source is P with phone number only, looking up wallet for: {}", phoneNumber);
            String foundWallet = lookupActiveWalletByPhoneOrClientId(phoneNumber); // ← CHANGED HERE

            if (foundWallet != null) {
                log.info("Normalized P request: identifier {} -> wallet {}", phoneNumber, foundWallet);
                return new String[]{foundWallet, "W"};
            } else {
                log.error("Failed to find active wallet for identifier: {}", phoneNumber);
                return null;
            }
        }

        if (walletNumber != null && !walletNumber.isEmpty()) {
            return new String[]{walletNumber, "W"};
        }

        log.error("Invalid request: no wallet number or phone number provided");
        return null;
    }

    public ResponseService qrtransferToWallet(RequestQrPayment request) {
        try {
            String url = walletBackendUrl + qrTransferEndpoint;
            log.info("Calling qr transfer endpoint at: {}", url);

            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Normalize source wallet
            String[] normalizedSource = normalizeWalletRequest(
                    request.getSource(),
                    request.getWalletNumber(),
                    request.getPhoneNumber()
            );

            if (normalizedSource == null) {
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided source identifier (phone or client ID)");
                return errorResponse;
            }

            request.setWalletNumber(normalizedSource[0]);
            request.setSource(normalizedSource[1]);

            log.debug("Normalized source request - Wallet: {}, Source: {}",
                    request.getWalletNumber(), request.getSource());


            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            request.setBank("00100");

            // Set destination bank if not already set
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
            }

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling qr transfer", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling qr transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling qr transfer", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling qr transfer: " + e.getMessage());
            return errorResponse;
        }
    }


    public ResponseServiceJson transferToWallet(RequestWalletToWallet request) {
        try {
            String url = walletBackendUrl + walletToWalletEndpoint;
            log.info("Calling wallet-to-wallet transfer endpoint at: {}", url);

            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Normalize source wallet
            String[] normalizedSource = normalizeWalletRequest(
                    request.getSource(),
                    request.getSrcWalletNumber(),
                    request.getSrcPhoneNumber()
            );

            if (normalizedSource == null) {
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided source identifier (phone or client ID)");
                return errorResponse;
            }

            request.setSrcWalletNumber(normalizedSource[0]);
            request.setSource(normalizedSource[1]);

            log.debug("Normalized source request - Wallet: {}, Source: {}",
                    request.getSrcWalletNumber(), request.getSource());

            // Normalize destination wallet if desPhoneNumber is provided and desWalletNumber is empty
            if ((request.getDesWalletNumber() == null || request.getDesWalletNumber().isEmpty()) &&
                    request.getDesPhoneNumber() != null && !request.getDesPhoneNumber().isEmpty()) {

                log.info("Destination wallet number not provided, looking up wallet for identifier: {}",
                        request.getDesPhoneNumber());

                String foundDestinationWallet = lookupActiveWalletByPhoneOrClientId(request.getDesPhoneNumber()); // ← CHANGED HERE
                if (foundDestinationWallet != null) {
                    request.setDesWalletNumber(foundDestinationWallet);
                    log.info("Found destination wallet: {} for identifier: {}",
                            foundDestinationWallet, request.getDesPhoneNumber());
                } else {
                    ResponseServiceJson errorResponse = new ResponseServiceJson();
                    errorResponse.setRespCode("404");
                    errorResponse.setMessage("No active wallet found for the provided destination identifier (phone or client ID)");
                    return errorResponse;
                }
            }

            // Validate that we have a destination wallet number
            if (request.getDesWalletNumber() == null || request.getDesWalletNumber().isEmpty()) {
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("400");
                errorResponse.setMessage("Destination wallet number or identifier (phone/client ID) is required");
                return errorResponse;
            }

            log.debug("Final destination wallet: {}", request.getDesWalletNumber());

            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            request.setSrcBank("00100");

            // Set destination bank if not already set
            if (request.getDesBank() == null || request.getDesBank().isEmpty()) {
                request.setDesBank("00100");
            }

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet-to-wallet transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet-to-wallet transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet-to-wallet transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet-to-wallet transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson transferToAccount(RequestWalletToBankAccount request) {
        try {
            String url = walletBackendUrl + walletToAccountEndpoint;
            log.info("Calling wallet-to-account transfer endpoint at: {}", url);

            // Always set the source bank
            request.setSrcBank("00100");
            log.debug("Set srcBank: {}", request.getSrcBank());

            if (request.getAuthCode() == null || request.getAuthCode().isEmpty()) {
                log.warn("No authCode provided - name enquiry should be performed first");
            }

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            String[] normalized = normalizeWalletRequest(
                    request.getSource(),
                    request.getWalletNumber(),
                    request.getPhoneNumber()
            );

            if (normalized == null) {
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided phone number");
                return errorResponse;
            }

            request.setWalletNumber(normalized[0]);
            request.setSource(normalized[1]);

            log.debug("Normalized request - Wallet: {}, Source: {}",
                    request.getWalletNumber(), request.getSource());
            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body before PIN encryption: {}", request);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            request.setBankDes("00100");
            log.debug("Final request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet-to-account transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet-to-account transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet-to-account transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet-to-account transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson accountNameEnquiry(RequestWalletToBankAccountNE request) {
        try {
            String url = walletBackendUrl + walletToAccountNeEndpoint;
            log.info("Calling account name enquiry endpoint at: {}", url);

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }


            String[] normalized = normalizeWalletRequest(
                    request.getSource(),
                    request.getWalletNumber(),
                    request.getPhoneNumber()
            );

            if (normalized == null) {
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided phone number");
                return errorResponse;
            }

            request.setWalletNumber(normalized[0]);
            request.setSource(normalized[1]);

            log.debug("Normalized request - Wallet: {}, Source: {}",
                    request.getWalletNumber(), request.getSource());

            // Set default values for srcBank if needed
            if (request.getSrcBank() == null || request.getSrcBank().isEmpty()) {
                request.setSrcBank("00100"); // Default bank code if not available
            }
            request.setDesBank("00100");

            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars) : {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body : {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling account name enquiry", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling account name enquiry: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling account name enquiry", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling account name enquiry: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson accountToWallet(AccountToWalletRequest request) {
        try {
            String url = walletBackendUrl + accountToWalletEndpoint;
            log.info("Calling account-to-wallet transfer endpoint at: {}", url);

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId: {}", request.getRequestId());
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate: {}", request.getRequestDate());
            }


            String[] normalized = normalizeWalletRequest(
                    request.getSource(),
                    request.getWalletNumber(),
                    request.getPhoneNumber()
            );

            if (normalized == null) {
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided phone number");
                return errorResponse;
            }

            request.setWalletNumber(normalized[0]);
            request.setSource(normalized[1]);

            log.debug("Normalized request - Wallet: {}, Source: {}",
                    request.getWalletNumber(), request.getSource());
            // Set default banks if not provided
            if (request.getSrcBank() == null || request.getSrcBank().isEmpty()) {
                request.setSrcBank("00100"); // Default source bank
            }

            if (request.getDesBank() == null || request.getDesBank().isEmpty()) {
                request.setDesBank("00100"); // Default destination bank
            }


            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }
            // Get authentication token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Final request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling account-to-wallet transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling account-to-wallet transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling account-to-wallet transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling account-to-wallet transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService transferToMobileMoney(RequestWalletToMB request) {
        try {
            String url = walletBackendUrl + "/WalletToMobileMoney";
            log.info("Calling mobile money transfer endpoint at: {}", url);

            // Add requestId and requestDate if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId: {}", request.getRequestId());
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate: {}", request.getRequestDate());
            }

            String[] normalized = normalizeWalletRequest(
                    request.getSource(),
                    request.getWalletNumber(),
                    request.getPhoneNumber()
            );

            if (normalized == null) {
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("404");
                errorResponse.setMessage("No active wallet found for the provided phone number");
                return errorResponse;
            }

            request.setWalletNumber(normalized[0]);
            request.setSource(normalized[1]);

            log.debug("Normalized request - Wallet: {}, Source: {}",
                    request.getWalletNumber(), request.getSource());
            // Get token from token service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request details: {}", request);


            // Make the API call
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for mobile money transfer", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for mobile money transfer", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson mobileMoneyNameEnquiry(RequestWalletToMobileMoneyNE request) {
        try {
            // Generate necessary fields if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId: {}", request.getRequestId());
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate: {}", request.getRequestDate());
            }

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
            }

            String url = walletBackendUrl + mobileMoneyNeEndpoint;
            log.info("Calling mobile money name enquiry endpoint at: {}", url);

            // Get service account token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body: {}", request);

            // Make the API call
            ResponseServiceJson response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .block();

            log.info("Mobile money name enquiry completed with response code: {}",
                    response != null ? response.getRespCode() : "null");

            return response;
        } catch (Exception e) {
            log.error("Error calling wallet backend", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }




}
