package org.bits.diamabankwalletf.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BankAccountController {

    private final WebClient webClient;

    @Value("${diama.bank.api.base-url}")
    private String diamaBankBaseUrl;

    /**
     * Get account balance using account number (via /compte API)
     */
    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String clientId) {

        try {
            String requestId = generateRequestId();

            // Tailler clientId à 6 positions maximum
            String safeClientId;
            if (clientId == null || clientId.isBlank()) {
                safeClientId = "0";
            } else {
                // Prendre les 6 premiers caractères si la longueur dépasse 6
                safeClientId = clientId.length() > 6 ? clientId.substring(0, 6) : clientId;
            }

            String apiUrl = String.format("%s/compte/%s/%s/%s",
                    diamaBankBaseUrl, requestId, safeClientId, accountNumber);

            log.info("Calling DIAMA Bank API with WebClient: {} (clientId taillé: {})",
                    apiUrl, safeClientId);

            AccountBalanceResponse response = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(AccountBalanceResponse.class)
                    .block();

            log.info("DIAMA raw response: {}", response);

            if (response == null) {
                log.error("Null response from DIAMA Bank API");
                return ResponseEntity.internalServerError()
                        .body(new BalanceResponse("99", "API response is null", null));
            }

            if (!"00".equals(response.getCode())) {
                log.warn("DIAMA Bank API returned error code: {} - {}",
                        response.getCode(), response.getMessage());
                return ResponseEntity.badRequest()
                        .body(new BalanceResponse(response.getCode(), response.getMessage(), null));
            }

            AccountInfo account = response.getData();
            BalanceInfo balanceInfo = new BalanceInfo(
                    account.getAccountNbr(),
                    account.getAvailableBalance(),
                    account.getLedgerBalance(),
                    account.getCurrency(),
                    account.getValueDate()
            );

            log.info("Successfully retrieved balance for account {}: Available={}, Ledger={}",
                    accountNumber, account.getAvailableBalance(), account.getLedgerBalance());

            return ResponseEntity.ok(new BalanceResponse("00", "Success", balanceInfo));

        } catch (Exception e) {
            log.error("Error retrieving balance for account {}: {}", accountNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new BalanceResponse("96", "Internal error: " + e.getMessage(), null));
        }
    }
    /**
     * Get detailed client info + list of accounts (via /client API)
     */
    @GetMapping("/details/{accountNumber}")
    public ResponseEntity<ClientInfoResponse> getAccountDetails(
            @PathVariable String accountNumber,
            @RequestParam String clientId) {

        try {
            String requestId = generateRequestId();
            String apiUrl = String.format("%s/client/%s/%s",
                    diamaBankBaseUrl, requestId, clientId);

            log.info("Retrieving account details with WebClient: {}", apiUrl);

            ClientInfoResponse response = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(ClientInfoResponse.class)
                    .block();

            if (response == null || !"00".equals(response.getCode())) {
                return ResponseEntity.badRequest().body(response);
            }

            boolean accountExists = response.getData().getAccounts().stream()
                    .anyMatch(account -> accountNumber.equals(account.getAccountNbr()));

            if (!accountExists) {
                log.warn("Account {} not found for client {}", accountNumber, clientId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving account details: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateRequestId() {
        return "REQ" + System.currentTimeMillis() % 1000000000L;
    }

    // ✅ Response DTOs

    public static class BalanceResponse {
        private String code;
        private String message;
        private BalanceInfo data;
        // constructor/getters/setters
        public BalanceResponse(String code, String message, BalanceInfo data) {
            this.code = code; this.message = message; this.data = data;
        }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public BalanceInfo getData() { return data; }
    }

    public static class BalanceInfo {
        private String accountNumber;
        private String availableBalance;
        private String ledgerBalance;
        private String currency;
        private String valueDate;
        // constructor/getters/setters
        public BalanceInfo(String accountNumber, String availableBalance,
                           String ledgerBalance, String currency, String valueDate) {
            this.accountNumber = accountNumber;
            this.availableBalance = availableBalance;
            this.ledgerBalance = ledgerBalance;
            this.currency = currency;
            this.valueDate = valueDate;
        }
        public String getAccountNumber() { return accountNumber; }
        public String getAvailableBalance() { return availableBalance; }
        public String getLedgerBalance() { return ledgerBalance; }
        public String getCurrency() { return currency; }
        public String getValueDate() { return valueDate; }
    }

    // ✅ Used for /compte
    public static class AccountBalanceResponse {
        private String code;
        private String message;
        private AccountInfo data; // single account
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public AccountInfo getData() { return data; }
    }

    // ✅ Used for /client
    public static class ClientInfoResponse {
        private String code;
        private String message;
        private ClientData data;
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public ClientData getData() { return data; }
    }

    public static class ClientData {
        @JsonProperty("requestId") private String requestId;
        @JsonProperty("clientId") private String clientId;
        @JsonProperty("accounts") private List<AccountInfo> accounts;
        // getters
        public String getRequestId() { return requestId; }
        public String getClientId() { return clientId; }
        public List<AccountInfo> getAccounts() { return accounts; }
    }

    public static class AccountInfo {
        @JsonProperty("accountNbr") private String accountNbr;
        @JsonProperty("availableBalance") private String availableBalance;
        @JsonProperty("currency") private String currency;
        @JsonProperty("valueDate") private String valueDate;
        @JsonProperty("ledgerBalance") private String ledgerBalance;
        @JsonProperty("TypCompte") private String typCompte;
        @JsonProperty("requestId") private String requestId;
        // getters
        public String getAccountNbr() { return accountNbr; }
        public String getAvailableBalance() { return availableBalance; }
        public String getCurrency() { return currency; }
        public String getValueDate() { return valueDate; }
        public String getLedgerBalance() { return ledgerBalance; }
        public String getTypCompte() { return typCompte; }
        public String getRequestId() { return requestId; }
    }
}
