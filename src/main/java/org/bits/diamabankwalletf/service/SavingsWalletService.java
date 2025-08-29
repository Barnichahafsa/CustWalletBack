package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.SavingAccount;
import org.bits.diamabankwalletf.repository.SavingAccountRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsWalletService {
    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.create-savings-wallet}")
    private String createSavingsWalletEndpoint;

    @Value("${wallet.backend.endpoints.balance-savings-wallet}")
    private String balanceSavingsWalletEndpoint;

    @Value("${wallet.backend.endpoints.credit-savings-wallet}")
    private String creditSavingsWalletEndpoint;

    @Value("${wallet.backend.endpoints.debit-savings-wallet}")
    private String debitSavingsWalletEndpoint;

    @Value("${wallet.backend.endpoints.last5-savings-wallet}")
    private String last5SavingsWalletEndpoint;

    @Value("${wallet.backend.endpoints.e-statement-savings-wallet}")
    private String savingsWalletEStatementEndpoint;

    private final SavingAccountRepository savingAccountRepository;

    public ResponseService savingEStatement(RequestEStatement request) {
        try {
            String url = walletBackendUrl + savingsWalletEStatementEndpoint;
            log.info("Calling savings wallet statement endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }
            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            if (request.getBank() == null ) {

                request.setBank("00100");
            }

            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet e-statement");
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}, bank={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId(), request.getBank());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet e-statement API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling savings wallet e-statement API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info(" savings wallet statement response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling savings wallet statement API", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling savings wallet statement API: " + e.getMessage());
            return errorResponse;
        }
    }


    public JResponseService last5SavingsWallet(RequestEStatement request) {
        try {
            String url = walletBackendUrl + last5SavingsWalletEndpoint;
            log.info("Calling savings wallet last5 endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }
            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet credit");
                JResponseService errorResponse = new JResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet last5 API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling savings wallet last5 API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info(" savings wallet last5 response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling savings wallet last5 API", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling savings wallet last5 API: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService creditSavingsWallet(RequestCreditSaving request) {
        try {
            String url = walletBackendUrl + creditSavingsWalletEndpoint;
            log.info("Calling savings wallet credit endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }


            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet credit");
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet credit API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling credit savings wallet  API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("credit savings wallet credit response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling credit savings wallet API", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling credit savings wallet  API: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService debitSavingsWallet(RequestDebitSaving request) {
        try {
            String url = walletBackendUrl + debitSavingsWalletEndpoint;
            log.info("Calling savings wallet debit endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }


            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet debit");
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet debit API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling credit savings wallet  API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("debit savings wallet response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling debit savings wallet API", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling debit savings wallet  API: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson savingsWalletBalance(RequestSavingBalance request) {
        try {
            String url = walletBackendUrl + balanceSavingsWalletEndpoint;
            log.info("Calling savings wallet balance endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }
            

            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet balance");
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet balance API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling savings wallet balancew API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("Savings wallet balance response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling Savings wallet balance API", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling Savings wallet balance  API: " + e.getMessage());
            return errorResponse;
        }
    }

    public List<SavingAccount> getByWalletNumber(String walletNumber) {
        return savingAccountRepository.findByWalletNumber(walletNumber);
    }

    public ResponseServiceJson createSavingsWallet(RequestCheckPin request) {
        try {
            String url = walletBackendUrl + createSavingsWalletEndpoint;
            log.info("Calling savings wallet creation endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }

            String bankCode = "00100";
            if (request.getWalletNumber() != null && request.getWalletNumber().length() >= 5) {

            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), bankCode);
                log.info("PIN encrypted for bank code: {}", bankCode);
                request.setPin(encryptedPin);
            }

            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for saving wallet creation");
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> {
                        log.error("Error calling savings wallet creation API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling savings wallet creation API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("Savings wallet creation response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling Savings wallet creation  API", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling Savings wallet creation  API: " + e.getMessage());
            return errorResponse;
        }
    }


}
