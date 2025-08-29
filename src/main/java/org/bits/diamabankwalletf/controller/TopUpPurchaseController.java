package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestTopUpPurchase;
import org.bits.diamabankwalletf.dto.ResponseService;
import org.bits.diamabankwalletf.service.TopUpPurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TopUpPurchaseController {
    private final TopUpPurchaseService topUpPurchaseService;

    @PostMapping("/top-up")
    public ResponseEntity<ResponseService> deactivateWallet(@RequestBody RequestTopUpPurchase request) {
        log.info("Received topUp Purchase request: {}", request.getRequestId());

        ResponseService response = topUpPurchaseService.topUpPurchase(request);

        log.info("topUp Purchase request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }
}
