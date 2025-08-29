package org.bits.diamabankwalletf.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PasswordEncryption {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public static void main(String[] args) {
        PasswordEncryption util = new PasswordEncryption();

        String rawPassword = "password123";
        String encrypted = util.encryptPassword(rawPassword);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Match: " + util.matchPassword(rawPassword, encrypted));

        String testPassword = "password123";
        String testEncrypted = util.encryptPassword(testPassword);
        System.out.println("\nTest password encrypted: " + testEncrypted);
    }
}
