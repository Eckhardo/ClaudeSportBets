package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.exception.AuthenticationFailedException;
import org.ebi.sportbets.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String VALID_REQUEST = """
            {"username": "erika", "passwort": "geheim"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AuthService authService;

    private static TipperDto erika() {
        return TipperDto.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .communityAdmin(false)
                .competitionAdmin(false)
                .build();
    }

    @Test
    void should_returnTipperDto_when_credentialsAreValid() {
        // Arrange
        given(authService.login(any(LoginRequestDto.class))).willReturn(erika());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 3, "username": "erika"}
                        """);
    }

    @Test
    void should_return401_when_credentialsAreInvalid() {
        // Arrange
        given(authService.login(any(LoginRequestDto.class)))
                .willThrow(new AuthenticationFailedException("Invalid username or passwort"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 401, "message": "Invalid username or passwort", "path": "/v1/auth/login"}
                        """);
    }

    @Test
    void should_return400WithoutEchoingPasswort_when_usernameIsBlank() {
        // Arrange
        String request = """
                {"username": "", "passwort": "geheim"}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyText()
                .contains("username")
                .doesNotContain("geheim");
    }
}
