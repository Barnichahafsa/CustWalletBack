package org.bits.diamabankwalletf.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Getter
public class WalletDetails implements UserDetails {

    private final Wallet wallet;

    public WalletDetails(Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = wallet.getWalletLevel() != null ?
                wallet.getWalletLevel().toString() : "WALLET";
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return wallet.getWalletPin();
    }

    @Override
    public String getUsername() {
        return wallet.getMobileNumber();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return wallet.getBlockAction() == null || wallet.getBlockAction() != 'Y';
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "N".equals(wallet.getStatusWallet());
    }


    public String getWalletNumber() {
        return wallet.getWalletNumber();
    }

    public String getBankCode() {
        return wallet.getBankCode();
    }

    public String getClientCode() {
        return wallet.getClientCode();
    }

    public String getPhoneNumber() {
        return wallet.getMobileNumber();
    }

    public String getFullName() {
        String firstName = wallet.getFirstName();
        String lastName = wallet.getFamilyName();

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Wallet User";
        }
    }

    public String getEmail() {
        return wallet.getEmailAddress();
    }
}
