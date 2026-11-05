package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.exception.AuthenticationFailedException;
import org.ebi.sportbets.repository.TipperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // Same message for unknown username and wrong passwort, so existing usernames cannot be probed
    private static final String INVALID_CREDENTIALS = "Invalid username or passwort";

    private final TipperRepository tipperRepository;
    private final TipperService tipperService;

    public TipperDto login(LoginRequestDto loginRequest) {
        return tipperRepository.findByUsername(loginRequest.getUsername())
                .filter(tipper -> passwortMatches(loginRequest.getPasswort(), tipper.getPasswort()))
                .map(tipperService::toDto)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
    }

    // The legacy schema stores the passwort as plain text; compare in constant time to avoid timing leaks
    private static boolean passwortMatches(String given, String stored) {
        return stored != null && MessageDigest.isEqual(
                given.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }
}
