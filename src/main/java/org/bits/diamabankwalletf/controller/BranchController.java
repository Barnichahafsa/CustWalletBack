package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Branch;
import org.bits.diamabankwalletf.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    /**
     * Retrieve all branches for a specific bank
     * @param bankCode The code of the bank
     * @return List of branches
     */
    @GetMapping("/{bankCode}")
    public ResponseEntity<List<Map<String, Object>>> getBranchesByBankCode(@PathVariable String bankCode) {
        log.info("Fetching branches for bank code: {}", bankCode);

        List<Map<String, Object>> branches = branchService.getAllBranches(bankCode);

        if (branches.isEmpty()) {
            log.warn("No branches found for bank code: {}", bankCode);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(branches);
    }

    /**
     * Retrieve detailed information for a specific branch
     * @param bankCode The code of the bank
     * @param branchCode The code of the specific branch
     * @return Detailed branch information
     */
    @GetMapping("/{bankCode}/{branchCode}")
    public ResponseEntity<Map<String, Object>> getBranchDetails(
            @PathVariable String bankCode,
            @PathVariable String branchCode) {
        log.info("Fetching details for branch: {} in bank: {}", branchCode, bankCode);

        try {
            Map<String, Object> branchDetails = branchService.getBranchDetails(bankCode, branchCode);
            return ResponseEntity.ok(branchDetails);
        } catch (RuntimeException e) {
            log.error("Branch not found: {} in bank: {}", branchCode, bankCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Find nearby branches based on coordinates
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param bankCode Bank code to filter branches
     * @param maxDistance Maximum distance in kilometers (optional)
     * @return List of nearby branches
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<Map<String, Object>>> getNearbyBranches(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String bankCode,
            @RequestParam(required = false) Double maxDistance) {

        log.info("Finding nearby branches at [{}, {}] for bank: {}",
                latitude, longitude, bankCode);

        List<Map<String, Object>> nearbyBranches = branchService.getNearbyBranches(
                latitude, longitude, bankCode, maxDistance);

        if (nearbyBranches.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(nearbyBranches);
    }
}
