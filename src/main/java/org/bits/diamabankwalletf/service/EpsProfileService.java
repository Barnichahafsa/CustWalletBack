package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.EpsProfile;
import org.bits.diamabankwalletf.repository.EpsProfileRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpsProfileService {

    private final EpsProfileRepository epsProfileRepository;

    public String getEpsProfile(String code) {
        log.info("Getting EPS profile for code=[{}]", code);
        return epsProfileRepository.findByCode(code)
                .map(EpsProfile::getValue)
                .orElse("0");
    }
}
