package org.ebi.sportbets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Encodes new passworts with BCrypt, stored with an id prefix such as {@code {bcrypt}$2a$10$...}.
     * Legacy passworts without a prefix are plain text; they are still accepted for matching, and
     * {@link PasswordEncoder#upgradeEncoding} reports them, so they can be re-encoded after a successful login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder =
                (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        encoder.setDefaultPasswordEncoderForMatches(new LegacyPlainTextPasswordEncoder());
        return encoder;
    }

    // Only used for matching legacy values; compares in constant time to avoid timing leaks
    private static final class LegacyPlainTextPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException("Plain text passworts must not be stored");
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword != null && MessageDigest.isEqual(
                    rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                    encodedPassword.getBytes(StandardCharsets.UTF_8));
        }
    }
}
