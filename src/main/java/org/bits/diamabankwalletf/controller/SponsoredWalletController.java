package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.service.SponsoredWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@RestController
@RequestMapping("/api/sponsor")
@RequiredArgsConstructor
@Slf4j
public class SponsoredWalletController {

    private final SponsoredWalletService sponsoredWalletService;

    @PostMapping("/daily-limits")
    public ResponseEntity<ResponseService> sponsoredWalletDailyLimits(@RequestBody RequestGetDailyLimitSponsorWallet request) {

        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("RequestId généré: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("RequestDate générée: {}", request.getRequestDate());
        }

        if (request.getSource() == null || request.getSource().isEmpty()) {
            request.setSource("W");
        }

        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("CUSTOMER");
        }

        ResponseService response = sponsoredWalletService.sponsorWalletDailyLimits(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sponsor-wallet")
    public ResponseEntity<ResponseServiceJson> sponsorWallet(@RequestBody SponsoredWalletDto request) {
        log.info("Demande reçue pour lier un portefeuille sponsorisé: {}", request.getWalletNumberSponsored());

        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("RequestId généré: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("RequestDate générée: {}", request.getRequestDate());
        }

        if (request.getSource() == null || request.getSource().isEmpty()) {
            request.setSource("W");
        }

        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("CUSTOMER");
        }

        ResponseServiceJson response = sponsoredWalletService.sponsorWallet(request);

        log.info("Opération d'ajout de portefeuille sponsorisé terminée avec le code de réponse: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sponsored-wallets")
    public ResponseEntity<JResponseService> getSponsoredWallets(@RequestBody RequestListSponsoredWallets request) {
        log.info("Received sponsored wallets request for source: {}, identifier: {}",
                request.getSource(),
                "W".equals(request.getSource()) ? request.getWalletNumber() : request.getPhoneNumber());

        validateAndPrepareRequest(request);

        JResponseService response = sponsoredWalletService.getSponsoredWallets(request);

        log.info("Sponsored wallets request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/list-sponsoring-wallets")
    public ResponseEntity<JResponseService> listSponsoringWallets(@RequestBody RequestListSponsoringWallets request) {
        String identifier = "W".equals(request.getSource()) ?
                "wallet: " + request.getWalletNumber() :
                "phone: " + request.getPhoneNumber();

        log.info("Received list sponsoring wallets request for {}", identifier);

        // Compléter les champs manquants dans la requête
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

        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
            log.debug("Set default bank code: {}", request.getBank());
        }

        // Définir entityId par défaut si non fourni
        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("CUSTOMER");
            log.debug("Set default entityId: {}", request.getEntityId());
        }

        // Appel du service
        JResponseService response = sponsoredWalletService.getListSponsoringWallets(request);

        log.info("List sponsoring wallets completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/unlink")
    public ResponseEntity<ResponseServiceJson> unlinkSponsoredWallet(@RequestBody SponsoredWalletDto request) {
        log.info("Received unlink sponsored wallet request for wallet: {}", request.getWalletNumber());

        if (request.getBankCode() == null || request.getBankCode().isEmpty()) {
            request.setBankCode("00100");
        }
        ResponseServiceJson response = sponsoredWalletService.unlinkSponsoredWallet(request);

        log.info("Unlink sponsored wallet completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseServiceJson> updateSponsoredWallet(@RequestBody SponsoredWalletDto request) {
        log.info("Received update sponsored wallet request for wallet: {}", request.getWalletNumber());

        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("CUSTOMER"); // Default entity ID
            log.debug("Set default entityId: {}", request.getEntityId());
        }
        ResponseServiceJson response = sponsoredWalletService.updateSponsoredWallet(request);

        log.info("Update sponsored wallet completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    private void validateAndPrepareRequest(RequestListSponsoredWallets request) {
        // Validate source
        if (request.getSource() == null || request.getSource().isEmpty()) {
            request.setSource("W"); // Default to wallet if not specified
            log.debug("Set default source to W (wallet)");
        }

        // Validate based on source type
        if ("W".equals(request.getSource()) && (request.getWalletNumber() == null || request.getWalletNumber().isEmpty())) {
            throw new IllegalArgumentException("Wallet number is required when source is W");
        }

        if ("P".equals(request.getSource()) && (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())) {
            throw new IllegalArgumentException("Phone number is required when source is P");
        }

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

        // Set default bank if not provided
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100"); // Default bank code
            log.debug("Set default bank code: {}", request.getBank());
        }

        // Set default entityId if not provided
        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("MOBILE_APP"); // Default entity ID
            log.debug("Set default entityId: {}", request.getEntityId());
        }
    }
}
