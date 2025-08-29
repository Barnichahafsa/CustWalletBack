package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.SavingAccount;
import org.bits.diamabankwalletf.service.SavingsWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-wallet")
@RequiredArgsConstructor
@Slf4j
public class SavingsWalletController {
    private final SavingsWalletService savingsWalletService;

    @PostMapping("/create")
    public ResponseEntity<ResponseServiceJson> createSavingsWallet(@RequestBody RequestCheckPin request) {
        log.info("Received Savings wallet creation request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseServiceJson response = savingsWalletService.createSavingsWallet(request);

        log.info("Returning Savings wallet creation response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/balance")
    public ResponseEntity<ResponseServiceJson> savingsWalletBalance(@RequestBody RequestSavingBalance request) {
        log.info("Received Savings wallet balance request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseServiceJson response = savingsWalletService.savingsWalletBalance(request);

        log.info("Returning Savings wallet balance response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    public ResponseEntity<ResponseService> creditSavingsWallet(@RequestBody RequestCreditSaving request) {
        log.info("Received Savings wallet credit request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseService response = savingsWalletService.creditSavingsWallet(request);

        log.info("Returning Savings wallet credit response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<ResponseService> debitSavingsWallet(@RequestBody RequestDebitSaving request) {
        log.info("Received Savings wallet debit request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseService response = savingsWalletService.debitSavingsWallet(request);

        log.info("Returning Savings wallet debit response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/last5")
    public ResponseEntity<JResponseService> savingsLast5(@RequestBody RequestEStatement request) {
        log.info("Received Savings wallet last5 request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        JResponseService response = savingsWalletService.last5SavingsWallet(request);

        log.info("Returning Savings wallet last5 response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/e-statement")
    public ResponseEntity<ResponseService> savingsEStatement(@RequestBody RequestEStatement request) {
        log.info("Received Savings wallet e-statement request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseService response = savingsWalletService.savingEStatement(request);

        log.info("Returning Savings wallet e-statement response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/by-wallet/{walletNumber}")
    public List<SavingAccount> getByWalletNumber(@PathVariable String walletNumber) {
        return savingsWalletService  .getByWalletNumber(walletNumber);
    }
}
