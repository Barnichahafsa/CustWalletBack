package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Device;
import org.bits.diamabankwalletf.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    public String getDeviceId(String phoneNumber) {
        try {
            log.info("getDeviceId() - >> phoneNumber=[{}]", phoneNumber);

            // Find all devices for this phone number
            List<Device> devices = deviceRepository.findAllByPhoneNumber(phoneNumber);

            // Return the most recent device ID if any exist
            if (!devices.isEmpty()) {
                // Using the first device for now - you might want to sort by creation date if available
                String deviceId = devices.get(0).getDeviceId();
                log.info("getDeviceId() - >> found {} devices, using ID: {}", devices.size(), deviceId);
                return deviceId;
            } else {
                log.info("getDeviceId() - >> not found");
                return null;
            }

        } catch (Exception e) {
            log.error("Error retrieving device ID", e);
            return null;
        }
    }

    public boolean insertDeviceId(String phoneNumber, String deviceId) {
        try {
            log.info("insertDeviceId() - >> phoneNumber=[{}]", phoneNumber);

            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setPhoneNumber(phoneNumber);

            deviceRepository.save(device);
            return true;

        } catch (Exception e) {
            log.error("Error inserting device ID", e);
            return false;
        }
    }

    public boolean updateDeviceId(String phoneNumber, String deviceId) {
        try {
            log.info("updateDeviceId() - >> phoneNumber=[{}]", phoneNumber);

            // Find all existing devices for this phone number
            List<Device> devices = deviceRepository.findAllByPhoneNumber(phoneNumber);

            if (!devices.isEmpty()) {
                // Update the first device found
                Device device = devices.get(0);
                device.setDeviceId(deviceId);
                deviceRepository.save(device);

                // Delete any other devices for this phone number
                if (devices.size() > 1) {
                    log.info("Found {} devices for phone number, cleaning up extras", devices.size());
                    for (int i = 1; i < devices.size(); i++) {
                        deviceRepository.delete(devices.get(i));
                    }
                }

                return true;
            } else {
                return insertDeviceId(phoneNumber, deviceId);
            }
        } catch (Exception e) {
            log.error("Error updating device ID", e);
            return false;
        }
    }
}
