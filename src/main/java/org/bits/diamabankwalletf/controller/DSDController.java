package org.bits.diamabankwalletf.controller;

import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.bits.diamabankwalletf.dto.DSDResponse;
import org.bits.diamabankwalletf.dto.RequestInitiateDSD;
import org.bits.diamabankwalletf.dto.ResponseService;
import org.bits.diamabankwalletf.dto.StatusQueryResponse;
import org.bits.diamabankwalletf.service.DSDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/dsd")
public class DSDController {

    @Autowired
    private DSDService dsdService;

    private static final Logger logger = LoggerFactory.getLogger(DSDController.class);

    @GetMapping("/payments/pending/{phoneNumber}")
    public ResponseEntity<ResponseService> getPendingPayments(
            @PathVariable String phoneNumber) {

        log.info(" GET PENDING OPERATIONS | phone={}", phoneNumber);


        try {
            ResponseService response = dsdService.getPendingOperations(phoneNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("ANNULE");
            errorResponse.setRespCode("500");
            errorResponse.setMessage("Internal error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/payments/initiate")
    public ResponseEntity<DSDResponse> initiatePayment(
            @RequestParam String phoneNumber,
            @RequestParam String chassisNumber,
            @RequestParam String category,
            @RequestParam String amount) {



        try {
            DSDResponse response = dsdService.initiateDSDPayment(
                    phoneNumber, chassisNumber, category, amount
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            DSDResponse errorResponse = new DSDResponse();
            errorResponse.setStatus("ANNULE");
            errorResponse.setRespCode("500");
            errorResponse.setMessage("Internal error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }




    @GetMapping("/payments/status")
    public ResponseEntity<StatusQueryResponse> queryPaymentStatus(@RequestBody RequestInitiateDSD request) {

        log.info("QUERY PAYMENT STATUS | phone={}, category={}, chassis={}, amount={}",
                request.getNumeroTelephone(),
                request.getCategVehicule(),
                request.getNumeroChassis(),
                request.getAmount());

        try {
            // Call backend service and get ResponseService
            ResponseService backendResponse = dsdService.queryPaymentStatus(request);

            // Transform to StatusQueryResponse
            StatusQueryResponse response = transformStatusResponse(backendResponse, request);

            if ("000".equals(backendResponse.getRespCode()) || "00".equals(backendResponse.getRespCode())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Error querying payment status: ", e);

            StatusQueryResponse errorResponse = new StatusQueryResponse();
            errorResponse.setCode("500");
            errorResponse.setMessage("Internal error: " + e.getMessage());

            StatusQueryResponse.StatusQueryData errorData = new StatusQueryResponse.StatusQueryData();
            errorData.setStatus("KO");
            errorData.setNumeroTelephone(request.getNumeroTelephone());
            errorData.setCategVehicule(request.getCategVehicule());
            errorData.setNumeroChassis(request.getNumeroChassis());
            errorData.setAmount(request.getAmount());
            errorData.setResult("Error");

            errorResponse.setData(errorData);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private StatusQueryResponse transformStatusResponse(ResponseService backendResponse, RequestInitiateDSD request) {
        StatusQueryResponse response = new StatusQueryResponse();

        // Map respCode to code
        response.setCode(backendResponse.getRespCode() != null ? backendResponse.getRespCode() : "999");
        response.setMessage(backendResponse.getMessage());

        // Build data object
        StatusQueryResponse.StatusQueryData data = new StatusQueryResponse.StatusQueryData();

        // Map status: "OK" if respCode is "000" or "00", otherwise "KO"
        if ("000".equals(backendResponse.getRespCode()) || "00".equals(backendResponse.getRespCode())) {
            data.setStatus("OK");

            // Parse the result JSON string to extract status details
            if (backendResponse.getResult() != null && !backendResponse.getResult().isEmpty()) {
                try {
                    JSONObject resultJson = JSONObject.fromObject(backendResponse.getResult());

                    // Set result based on DSD status from backend
                    String dsdStatus = resultJson.optString("status", "EN_ATTENTE");
                    data.setResult(dsdStatus);

                    // Optional: populate additional details
                    data.setOperationId(resultJson.optString("operationId"));
                    data.setTransactionId(resultJson.optString("transactionId"));
                    data.setMotif(resultJson.optString("motif"));
                    data.setCreatedAt(resultJson.optString("createdAt"));
                    data.setValidatedAt(resultJson.optString("validatedAt"));

                } catch (Exception e) {
                    log.error("Error parsing result JSON: {}", e.getMessage());
                    data.setResult("EN_ATTENTE");
                }
            } else {
                data.setResult("EN_ATTENTE");
            }
        } else {
            data.setStatus("KO");
            data.setResult("Error");
        }

        // Echo back request data
        data.setNumeroTelephone(request.getNumeroTelephone());
        data.setCategVehicule(request.getCategVehicule());
        data.setNumeroChassis(request.getNumeroChassis());
        data.setAmount(request.getAmount());

        response.setData(data);
        return response;
    }

    @PostMapping("/payments/validate")
    public ResponseEntity<DSDResponse> validatePayment(
            @RequestParam String operationId,
            @RequestParam String phoneNumber,
            @RequestParam String pin) {


        try {
            DSDResponse response = dsdService.validatePayment(operationId, phoneNumber, pin);


            return ResponseEntity.ok(response);

        } catch (Exception e) {

            DSDResponse errorResponse = new DSDResponse();
            errorResponse.setStatus("ANNULE");
            errorResponse.setRespCode("500");
            errorResponse.setMessage("Internal error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    @PostMapping("/payments/reject")
    public ResponseEntity<DSDResponse> rejectPayment(
            @RequestParam String operationId,
            @RequestParam String phoneNumber) {



        try {
            DSDResponse response = dsdService.rejectPayment(operationId, phoneNumber);


            return ResponseEntity.ok(response);

        } catch (Exception e) {

            DSDResponse errorResponse = new DSDResponse();
            errorResponse.setStatus("ANNULE");
            errorResponse.setRespCode("500");
            errorResponse.setMessage("Internal error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }




}
