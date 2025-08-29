package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.JResponseService;
import org.bits.diamabankwalletf.dto.RequestCheckPin;
import org.bits.diamabankwalletf.dto.ResponseServiceJson;
import org.bits.diamabankwalletf.model.Bank;
import org.bits.diamabankwalletf.model.RequestListBanks;
import org.bits.diamabankwalletf.repository.BankRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    private final BankRepository bankRepository;
    private final WalletCreationService walletCreationService;
    private final WebClient webClient;


    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.list-banks}")
    private String listBanksEndpoint;

    public String getBankWording(String bankCode) {
        log.info("Getting bank wording for bankCode=[{}]", bankCode);
        return bankRepository.findById(bankCode)
                .map(Bank::getWording)
                .orElse("Unknown Bank");
    }

    public boolean isBankCertified(String bankCode) {
        log.info("Checking if bank is certified for bankCode=[{}]", bankCode);
        return bankRepository.findById(bankCode)
                .map(bank -> "Y".equals(bank.getGipCertified()))
                .orElse(false);
    }

    public boolean getDebitPullOtp(String bankCode) {
        log.info("Checking if debit pull OTP is enabled for bankCode=[{}]", bankCode);
        return bankRepository.findById(bankCode)
                .map(bank -> "Y".equals(bank.getDebitPullOtp()))
                .orElse(false);
    }

    public String getGhBankCode(String bankCode) {
        log.info("Getting GH bank code for bankCode=[{}]", bankCode);
        return bankRepository.findById(bankCode)
                .map(Bank::getGhBankCode)
                .orElse(null);
    }

    public Optional<Bank> getBankDetails(String bankCode) {
        log.info("Getting bank details for bankCode=[{}]", bankCode);
        return bankRepository.findById(bankCode);
    }


    public JResponseService listbanks(RequestListBanks request) {
        try {
            String url = walletBackendUrl + listBanksEndpoint;
            log.info("Calling list banks endpoint at: {}", url);

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


            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }


            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for PIN check");
                JResponseService errorResponse = new JResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }



            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling list banks API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling list banks API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("list banks response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling list banks API", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling list banks API: " + e.getMessage());
            return errorResponse;
        }
    }
}
