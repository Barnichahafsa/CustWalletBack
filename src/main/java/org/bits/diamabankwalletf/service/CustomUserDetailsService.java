package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.model.WalletDetails;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final WalletRepository walletRepository;

    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException {
        log.debug("Loading user details for mobile number: {}", mobileNumber);

        // Find wallet by mobile number
        Wallet wallet = walletRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> {
                    log.warn("Wallet not found for mobile number: {}", mobileNumber);
                    return new UsernameNotFoundException("Wallet not found with mobile number: " + mobileNumber);
                });

        log.debug("Found wallet for mobile number: {}, wallet number: {}", mobileNumber, wallet.getWalletNumber());

        // Convert Wallet to WalletDetails which implements UserDetails
        return new WalletDetails(wallet);
    }
}
