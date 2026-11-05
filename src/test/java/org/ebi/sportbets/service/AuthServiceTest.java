package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.exception.AuthenticationFailedException;
import org.ebi.sportbets.repository.TipperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TipperRepository tipperRepository;

    @Mock
    private TipperService tipperService;

    @InjectMocks
    private AuthService authService;

    private static Tipper erika() {
        return Tipper.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .passwort("geheim")
                .build();
    }

    private static LoginRequestDto login(String username, String passwort) {
        return LoginRequestDto.builder().username(username).passwort(passwort).build();
    }

    @Test
    void should_returnTipperDto_when_credentialsAreValid() {
        // Arrange
        Tipper tipper = erika();
        TipperDto expected = TipperDto.builder().id(3L).username("erika").build();
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(tipperService.toDto(tipper)).willReturn(expected);

        // Act
        TipperDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void should_throwAuthenticationFailed_when_passwortIsWrong() {
        // Arrange
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(erika()));

        // Act
        Throwable thrown = catchThrowable(() -> authService.login(login("erika", "falsch")));

        // Assert
        assertThat(thrown)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or passwort");
    }

    @Test
    void should_throwAuthenticationFailed_when_usernameIsUnknown() {
        // Arrange
        given(tipperRepository.findByUsername("niemand")).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> authService.login(login("niemand", "geheim")));

        // Assert
        assertThat(thrown)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or passwort");
    }
}
