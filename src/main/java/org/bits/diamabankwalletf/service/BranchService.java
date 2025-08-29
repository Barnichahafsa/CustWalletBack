package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Branch;
import org.bits.diamabankwalletf.model.BranchData;
import org.bits.diamabankwalletf.repository.BranchDataRepository;
import org.bits.diamabankwalletf.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchDataRepository branchDataRepository;

    /**
     * Get all branches for the bank
     * @param bankCode The bank code
     * @return List of branches with basic information
     */
    public List<Map<String, Object>> getAllBranches(String bankCode) {
        log.info("Getting all branches for bankCode=[{}]", bankCode);

        return branchRepository.findByBankCode(bankCode).stream()
                .map(branch -> {
                    Map<String, Object> branchMap = new HashMap<>();
                    branchMap.put("id", branch.getBranchCode());
                    branchMap.put("name", branch.getWording());
                    branchMap.put("address", branch.getAddress());
                    branchMap.put("location", branch.getBankLocation());
                    branchMap.put("zipCode", branch.getZipCode());
                    return branchMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get detailed branch information including ATMs
     * @param bankCode The bank code
     * @param branchCode The branch code
     * @return Detailed branch information
     */
    public Map<String, Object> getBranchDetails(String bankCode, String branchCode) {
        log.info("Getting branch details for bankCode=[{}], branchCode=[{}]", bankCode, branchCode);

        Branch branch = branchRepository.findByBankCodeAndBranchCode(bankCode, branchCode)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        List<BranchData> atmData = branchDataRepository.findByBranch(branchCode);

        Map<String, Object> branchDetails = new HashMap<>();
        branchDetails.put("branchCode", branch.getBranchCode());
        branchDetails.put("name", branch.getWording());
        branchDetails.put("address", branch.getAddress());
        branchDetails.put("location", branch.getBankLocation());
        branchDetails.put("zipCode", branch.getZipCode());
        branchDetails.put("city", branch.getCityCode());
        branchDetails.put("country", branch.getCountryCode());

        List<Map<String, Object>> atms = atmData.stream()
                .map(atm -> {
                    Map<String, Object> atmMap = new HashMap<>();
                    atmMap.put("id", atm.getAtmId());
                    atmMap.put("name", atm.getAtmName());
                    atmMap.put("address", atm.getAddress());
                    atmMap.put("location", atm.getLocation());
                    atmMap.put("zone", atm.getZone());
                    atmMap.put("latitude", atm.getLatitude());
                    atmMap.put("longitude", atm.getLongitude());
                    return atmMap;
                })
                .collect(Collectors.toList());

        branchDetails.put("atms", atms);

        return branchDetails;
    }

    /**
     * Get list of nearby branches based on coordinates
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param bankCode The bank code
     * @param maxDistance Maximum distance in kilometers (optional)
     * @return List of nearby branches with distance information
     */
    public List<Map<String, Object>> getNearbyBranches(
            Double latitude,
            Double longitude,
            String bankCode,
            Double maxDistance) {

        log.info("Finding nearby branches at coordinates [{}, {}] for bankCode=[{}]",
                latitude, longitude, bankCode);

        // Get branches with location data
        List<BranchData> branchesWithLocation = branchDataRepository.findAllWithCoordinates();

        // Calculate distances and filter
        return branchesWithLocation.stream()
                .map(branchData -> {
                    try {
                        double branchLat = Double.parseDouble(branchData.getLatitude());
                        double branchLng = Double.parseDouble(branchData.getLongitude());

                        // Calculate distance using Haversine formula
                        double distance = calculateDistance(
                                latitude, longitude, branchLat, branchLng);

                        if (maxDistance == null || distance <= maxDistance) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("id", branchData.getAtmId());
                            result.put("name", branchData.getAtmName());
                            result.put("address", branchData.getAddress());
                            result.put("distance", Math.round(distance * 100.0) / 100.0); // Round to 2 decimals
                            result.put("latitude", branchData.getLatitude());
                            result.put("longitude", branchData.getLongitude());
                            return result;
                        }
                        return null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(map -> map != null)
                .sorted((m1, m2) -> Double.compare(
                        (Double) m1.get("distance"),
                        (Double) m2.get("distance")))
                .collect(Collectors.toList());
    }

    // Haversine formula to calculate distance between two coordinates in kilometers
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
