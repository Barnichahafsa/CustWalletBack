package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByDeviceIdAndPhoneNumber(String deviceId, String phoneNumber);

    Optional<Device> findByPhoneNumber(String phoneNumber);

    List<Device> findAllByPhoneNumber(String phoneNumber);
}
