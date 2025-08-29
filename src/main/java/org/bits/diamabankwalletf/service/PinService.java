package org.bits.diamabankwalletf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestChangePin;
import org.bits.diamabankwalletf.dto.RequestResetPin;
import org.bits.diamabankwalletf.dto.RequestResetPinQ;
import org.bits.diamabankwalletf.dto.ResponseService;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinService {

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.change-pin}")
    private String changePinUrl;

    @Value("${wallet.backend.endpoints.reset-pin}")
    private String resetPinUrl;

    @Value("${wallet.backend.endpoints.reset-pin-q}")
    private String resetPinQUrl;

    private final WalletCreationService walletCreationService;
    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletRepository walletRepository;
    private final PinExpiryService pinExpiryService;

    public ResponseService changePin(RequestChangePin request) {
        try {
            String url = walletBackendUrl + changePinUrl;
            log.info("[CHANGE PIN] Calling changePin endpoint at: {}", url);

            // Print all fields of the request to identify what's missing
            log.info("[CHANGE PIN] Request details:");
            log.info("[CHANGE PIN] - walletNumber: {}", request.getWalletNumber());
            log.info("[CHANGE PIN] - phoneNumber: {}", request.getPhoneNumber());
            log.info("[CHANGE PIN] - source: {}", request.getSource());
            log.info("[CHANGE PIN] - bank: {}", request.getBank());
            log.info("[CHANGE PIN] - oldPinCode: {}", request.getOldPinCode() != null ? "PRESENT" : "MISSING");
            log.info("[CHANGE PIN] - newPinCode: {}", request.getNewPinCode() != null ? "PRESENT" : "MISSING");
            log.info("[CHANGE PIN] - confPinCode: {}", request.getConfPinCode() != null ? "PRESENT" : "MISSING");
            log.info("[CHANGE PIN] - requestId: {}", request.getRequestId());
            log.info("[CHANGE PIN] - requestDate: {}", request.getRequestDate());
            log.info("[CHANGE PIN] - entityId: {}", request.getEntityId());

            // Add missing required fields
            boolean addedMissingFields = false;

            if (request.getSource() == null) {
                request.setSource("W");  // W for wallet-based request
                log.info("[CHANGE PIN] Added missing source field: W");
                addedMissingFields = true;
            }

            if (request.getEntityId() == null) {
                // Use a default entity ID or get it from configuration
                request.setEntityId("0000000001");  // Example default value
                log.info("[CHANGE PIN] Added missing entityId field: {}", request.getEntityId());
                addedMissingFields = true;
            }

            // Add requestId and requestDate if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.info("[CHANGE PIN] Generated requestId: {}", request.getRequestId());
                addedMissingFields = true;
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.info("[CHANGE PIN] Generated requestDate: {}", request.getRequestDate());
                addedMissingFields = true;
            }

            request.setOldPinCode(pinEncryptionUtil.encryptPin(request.getOldPinCode(),request.getBank()));
            request.setNewPinCode(pinEncryptionUtil.encryptPin(request.getNewPinCode(),request.getBank()));
            request.setConfPinCode(pinEncryptionUtil.encryptPin(request.getConfPinCode(),request.getBank()));

            // Validate the request again after adding missing fields
            if (addedMissingFields) {
                log.info("[CHANGE PIN] Request after adding missing fields:");
                log.info("[CHANGE PIN] - walletNumber: {}", request.getWalletNumber());
                log.info("[CHANGE PIN] - phoneNumber: {}", request.getPhoneNumber());
                log.info("[CHANGE PIN] - source: {}", request.getSource());
                log.info("[CHANGE PIN] - bank: {}", request.getBank());
                log.info("[CHANGE PIN] - oldPinCode: {}", request.getOldPinCode() != null ? "PRESENT" : "MISSING");
                log.info("[CHANGE PIN] - newPinCode: {}", request.getNewPinCode() != null ? "PRESENT" : "MISSING");
                log.info("[CHANGE PIN] - confPinCode: {}", request.getConfPinCode() != null ? "PRESENT" : "MISSING");
                log.info("[CHANGE PIN] - requestId: {}", request.getRequestId());
                log.info("[CHANGE PIN] - requestDate: {}", request.getRequestDate());
                log.info("[CHANGE PIN] - entityId: {}", request.getEntityId());
            }

            // Get token from token service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("[CHANGE PIN] Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Make the API call
            log.info("[CHANGE PIN] Sending request to backend API...");
            ResponseService response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("[CHANGE PIN] Error calling wallet backend for ChangePin", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

            log.info("[CHANGE PIN] Response received: {}", response);
            if (response != null && "000".equals(response.getRespCode())) {
                try {
                    log.info("[CHANGE PIN] PIN change successful in external system, updating local database");
                    log.info("[CHANGE PIN] Looking for wallet: walletNumber={}, bankCode={}",
                            request.getWalletNumber(), request.getBank());

                    // Trouver le wallet dans VOTRE base de données locale
                    Optional<Wallet> walletOpt = walletRepository.findByWalletNumberAndBankCode(
                            request.getWalletNumber(), request.getBank());

                    if (walletOpt.isPresent()) {
                        Wallet wallet = walletOpt.get();

                        log.info("[CHANGE PIN] Wallet found in local DB: {}", wallet.getWalletNumber());
                        log.info("[CHANGE PIN] BEFORE update - PIN expiry: {}, change required: {}",
                                wallet.getPinExpiryDate(), wallet.getPinChangeRequired());

                        // MISE À JOUR LOCALE OBLIGATOIRE
                        pinExpiryService.initializePinExpiry(wallet);

                        // Vérification après mise à jour
                        Wallet updatedWallet = walletRepository.findByWalletNumberAndBankCode(
                                request.getWalletNumber(), request.getBank()).orElse(null);

                        if (updatedWallet != null) {
                            log.info("[CHANGE PIN] AFTER update - PIN expiry: {}, change required: {}",
                                    updatedWallet.getPinExpiryDate(), updatedWallet.getPinChangeRequired());
                        } else {
                            log.error("[CHANGE PIN] Could not retrieve updated wallet");
                        }

                        log.info("[CHANGE PIN] Local database updated successfully for wallet: {}", wallet.getWalletNumber());
                    } else {
                        log.error("[CHANGE PIN] CRITICAL: Wallet not found in local database for update: walletNumber={}, bankCode={}",
                                request.getWalletNumber(), request.getBank());

                        // DIAGNOSTIC : Lister tous les wallets pour ce numéro de téléphone
                        if (request.getPhoneNumber() != null) {
                            Optional<Wallet> walletByPhone = walletRepository.findByPhoneNumber(request.getPhoneNumber());
                            if (walletByPhone.isPresent()) {
                                log.info("[CHANGE PIN] Found wallet by phone: walletNumber={}, bankCode={}",
                                        walletByPhone.get().getWalletNumber(), walletByPhone.get().getBankCode());

                                // Utiliser ce wallet à la place
                                pinExpiryService.initializePinExpiry(walletByPhone.get());
                                log.info("[CHANGE PIN] Updated wallet found by phone number instead");
                            } else {
                                log.error("[CHANGE PIN] No wallet found by phone number either: {}", request.getPhoneNumber());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[CHANGE PIN] CRITICAL ERROR updating local PIN expiry after successful PIN change", e);
                    // Ne pas faire échouer la réponse, mais log l'erreur
                }
            } else {
                log.warn("[CHANGE PIN] PIN change failed in external system, not updating local database. Response code: {}",
                        response != null ? response.getRespCode() : "null");
            }
            return response;
        } catch (Exception e) {
            log.error("[CHANGE PIN] Error calling wallet backend for ChangePin", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService resetPin(RequestResetPin request) {
        try {
            String url = walletBackendUrl + resetPinUrl;
            log.info("[RESET PIN] Calling resetPin endpoint at: {}", url);

            // Add requestId and requestDate if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.info("[RESET PIN] Generated requestId: {}", request.getRequestId());
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.info("[RESET PIN] Generated requestDate: {}", request.getRequestDate());
            }

            request.setNewPinCode(pinEncryptionUtil.encryptPin(request.getNewPinCode(),request.getBank()));
            request.setConfPinCode(pinEncryptionUtil.encryptPin(request.getConfPinCode(),request.getBank()));

            // Get token from token service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("[RESET PIN] Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Make the API call
            log.info("[RESET PIN] Sending reset request to backend API...");
            ResponseService response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("[RESET PIN] Error calling wallet backend for resetPin", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

            log.info("[RESET PIN] Response received: {}", response);
            if (response != null && "000".equals(response.getRespCode())) {
                try {
                    log.info("[RESET PIN] PIN reset successful in external system, updating local database");
                    log.info("[RESET PIN] Looking for wallet: walletNumber={}, bankCode={}",
                            request.getWalletNumber(), request.getBank());

                    // Trouver le wallet dans VOTRE base de données locale
                    Optional<Wallet> walletOpt = walletRepository.findByWalletNumberAndBankCode(
                            request.getWalletNumber(), request.getBank());

                    if (walletOpt.isPresent()) {
                        Wallet wallet = walletOpt.get();

                        log.info("[RESET PIN] Wallet found in local DB: {}", wallet.getWalletNumber());
                        log.info("[RESET PIN] BEFORE update - PIN expiry: {}, change required: {}",
                                wallet.getPinExpiryDate(), wallet.getPinChangeRequired());

                        // MISE À JOUR LOCALE OBLIGATOIRE
                        pinExpiryService.initializePinExpiry(wallet);

                        // Vérification après mise à jour
                        Wallet updatedWallet = walletRepository.findByWalletNumberAndBankCode(
                                request.getWalletNumber(), request.getBank()).orElse(null);

                        if (updatedWallet != null) {
                            log.info("[RESET PIN] AFTER update - PIN expiry: {}, change required: {}",
                                    updatedWallet.getPinExpiryDate(), updatedWallet.getPinChangeRequired());
                        } else {
                            log.error("[RESET PIN] Could not retrieve updated wallet");
                        }

                        log.info("[RESET PIN] Local database updated successfully for wallet: {}", wallet.getWalletNumber());
                    } else {
                        log.error("[RESET PIN] CRITICAL: Wallet not found in local database for update: walletNumber={}, bankCode={}",
                                request.getWalletNumber(), request.getBank());

                        // DIAGNOSTIC : Lister tous les wallets pour ce numéro de téléphone
                        if (request.getPhoneNumber() != null) {
                            Optional<Wallet> walletByPhone = walletRepository.findByPhoneNumber(request.getPhoneNumber());
                            if (walletByPhone.isPresent()) {
                                log.info("[RESET PIN] Found wallet by phone: walletNumber={}, bankCode={}",
                                        walletByPhone.get().getWalletNumber(), walletByPhone.get().getBankCode());

                                // Utiliser ce wallet à la place
                                pinExpiryService.initializePinExpiry(walletByPhone.get());
                                log.info("[RESET PIN] Updated wallet found by phone number instead");
                            } else {
                                log.error("[RESET PIN] No wallet found by phone number either: {}", request.getPhoneNumber());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[RESET PIN] CRITICAL ERROR updating local PIN expiry after successful PIN reset", e);
                    // Ne pas faire échouer la réponse, mais log l'erreur
                }
            } else {
                log.warn("[RESET PIN] PIN reset failed in external system, not updating local database. Response code: {}",
                        response != null ? response.getRespCode() : "null");
            }
            return response;
        } catch (Exception e) {
            log.error("[RESET PIN] Error calling wallet backend for resetPin", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService resetPinQ(RequestResetPinQ request) {
        try {
            String url = walletBackendUrl + resetPinQUrl;
            log.info("[RESET PIN Q] Calling resetPinQ endpoint at: {}", url);

            // Add requestId and requestDate if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.info("[RESET PIN Q] Generated requestId: {}", request.getRequestId());
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.info("[RESET PIN Q] Generated requestDate: {}", request.getRequestDate());
            }

            request.setNewPinCode(pinEncryptionUtil.encryptPin(request.getNewPinCode(),request.getBank()));
            request.setConfPinCode(pinEncryptionUtil.encryptPin(request.getConfPinCode(),request.getBank()));

            // Get token from token service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("[RESET PIN Q] Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Make the API call
            log.info("[RESET PIN Q] Sending resetPinQ request to backend API...");
            ResponseService response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("[RESET PIN Q] Error calling wallet backend for resetPinQ", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

            log.info("[RESET PIN Q] Response received: {}", response);
            if (response != null && "000".equals(response.getRespCode())) {
                try {
                    log.info("[RESET PIN Q] PIN reset successful in external system, updating local database");
                    log.info("[RESET PIN Q] Looking for wallet: walletNumber={}, bankCode={}",
                            request.getWalletNumber(), request.getBank());

                    // Trouver le wallet dans VOTRE base de données locale
                    Optional<Wallet> walletOpt = walletRepository.findByWalletNumberAndBankCode(
                            request.getWalletNumber(), request.getBank());

                    if (walletOpt.isPresent()) {
                        Wallet wallet = walletOpt.get();

                        log.info("[RESET PIN Q] Wallet found in local DB: {}", wallet.getWalletNumber());
                        log.info("[RESET PIN Q] BEFORE update - PIN expiry: {}, change required: {}",
                                wallet.getPinExpiryDate(), wallet.getPinChangeRequired());

                        // MISE À JOUR LOCALE OBLIGATOIRE
                        pinExpiryService.initializePinExpiry(wallet);

                        // Vérification après mise à jour
                        Wallet updatedWallet = walletRepository.findByWalletNumberAndBankCode(
                                request.getWalletNumber(), request.getBank()).orElse(null);

                        if (updatedWallet != null) {
                            log.info("[RESET PIN Q] AFTER update - PIN expiry: {}, change required: {}",
                                    updatedWallet.getPinExpiryDate(), updatedWallet.getPinChangeRequired());
                        } else {
                            log.error("[RESET PIN Q] Could not retrieve updated wallet");
                        }

                        log.info("[RESET PIN Q] Local database updated successfully for wallet: {}", wallet.getWalletNumber());
                    } else {
                        log.error("[RESET PIN Q] CRITICAL: Wallet not found in local database for update: walletNumber={}, bankCode={}",
                                request.getWalletNumber(), request.getBank());

                        // DIAGNOSTIC : Lister tous les wallets pour ce numéro de téléphone
                        if (request.getPhoneNumber() != null) {
                            Optional<Wallet> walletByPhone = walletRepository.findByPhoneNumber(request.getPhoneNumber());
                            if (walletByPhone.isPresent()) {
                                log.info("[RESET PIN Q] Found wallet by phone: walletNumber={}, bankCode={}",
                                        walletByPhone.get().getWalletNumber(), walletByPhone.get().getBankCode());

                                // Utiliser ce wallet à la place
                                pinExpiryService.initializePinExpiry(walletByPhone.get());
                                log.info("[RESET PIN Q] Updated wallet found by phone number instead");
                            } else {
                                log.error("[RESET PIN Q] No wallet found by phone number either: {}", request.getPhoneNumber());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[RESET PIN Q] CRITICAL ERROR updating local PIN expiry after successful PIN reset", e);
                    // Ne pas faire échouer la réponse, mais log l'erreur
                }
            } else {
                log.warn("[RESET PIN Q] PIN resetQ failed in external system, not updating local database. Response code: {}",
                        response != null ? response.getRespCode() : "null");
            }
            return response;
        } catch (Exception e) {
            log.error("[RESET PIN Q] Error calling wallet backend for resetPinQ", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }
}
