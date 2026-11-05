package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.UmsInfoDto;
import org.ebi.sportbets.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
class AuthController {

    private final AuthService authService;

    // Credentials go in the body, never in the URL, so they do not end up in access logs
    @PostMapping("/login")
    public ResponseEntity<UmsInfoDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        UmsInfoDto umsInfo = authService.login(loginRequest);
        log.info("logged in tipper {}", umsInfo.getUserId());
        return ResponseEntity.ok(umsInfo);
    }
}
