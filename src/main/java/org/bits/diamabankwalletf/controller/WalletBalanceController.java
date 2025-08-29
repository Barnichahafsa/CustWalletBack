package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestBalanceEnquiry;
import org.bits.diamabankwalletf.dto.ResponseServiceJson;
import org.bits.diamabankwalletf.service.WalletBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletBalanceController {

    private final WalletBalanceService walletBalanceService;

    @PostMapping("/balance")
    public ResponseEntity<ResponseServiceJson> getWalletBalance(@RequestBody RequestBalanceEnquiry request) {
        log.info("Received wallet balance request for wallet/phone: {}/{}",
                request.getWalletNumber(), request.getPhoneNumber());

        ResponseServiceJson response = walletBalanceService.getWalletBalance(request);

        log.info("Returning balance response: {}", response);
        return ResponseEntity.ok(response);
    }
}
