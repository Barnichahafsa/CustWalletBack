package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.repository.MobileMoneyOperatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {

    private final MobileMoneyOperatorRepository mobileMoneyOperatorRepository;

    public List<Map<String, Object>> getProviderList() {
        log.info("Getting provider list");
        return mobileMoneyOperatorRepository.findAllProvidersAsMap();
    }

    public List<Map<String, Object>> getAirtimeProviderList() {
        log.info("Getting airtime provider list");
        return mobileMoneyOperatorRepository.findAllAirtimeProvidersAsMap();
    }
}
