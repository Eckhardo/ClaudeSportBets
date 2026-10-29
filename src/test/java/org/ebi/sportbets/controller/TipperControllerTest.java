package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.TipperService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(TipperController.class)
class TipperControllerTest {

    private static final String VALID_REQUEST = """
            {"username": "erika", "firstname": "Erika", "lastname": "Mustermann", "email": "erika@example.org",
             "passwort": "geheim", "communityAdmin": false, "competitionAdmin": false}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TipperService tipperService;

    // passwort is set on purpose, to show that it is never serialized
    private static TipperDto erika() {
        return TipperDto.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .passwort("geheim")
                .communityAdmin(false)
                .competitionAdmin(false)
                .build();
    }

    @Test
    void should_returnTipperDtos_when_getTippers() {
        // Arrange
        given(tipperService.findAll()).willReturn(List.of(erika()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tippers").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 3, "username": "erika"}]
                        """);
    }

    @Test
    void should_notExposePasswort_when_getByExistingId() {
        // Arrange
        given(tipperService.findById(3L)).willReturn(erika());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tippers/{id}", 3L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .doesNotHavePath("$.passwort");
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(tipperService.findById(42L)).willThrow(new ResourceNotFoundException("Tipper", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tippers/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(tipperService.create(any(TipperDto.class))).willReturn(erika());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tippers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/tippers/3")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 3, "username": "erika"}
                        """);
    }

    @Test
    void should_passPasswortToService_when_createWithValidRequest() {
        // Arrange
        given(tipperService.create(any(TipperDto.class))).willReturn(erika());
        ArgumentCaptor<TipperDto> request = ArgumentCaptor.forClass(TipperDto.class);

        // Act
        mvc.post().uri("/v1/tippers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        then(tipperService).should().create(request.capture());
        assertThat(request.getValue().getPasswort()).isEqualTo("geheim");
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"username": " ", "email": "no-email"}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tippers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("username", "firstname", "lastname", "email", "passwort",
                        "communityAdmin", "competitionAdmin");
    }

    @Test
    void should_reportPasswortErrorWithoutRejectedValue_when_createWithTooLongPasswort() {
        // Arrange
        String invalidRequest = """
                {"username": "erika", "passwort": "%s"}
                """.formatted("geheim".repeat(50));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tippers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[?(@.field == 'passwort')].rejectedValue")
                .asArray()
                .containsExactly((Object) null);
    }

    @Test
    void should_return409_when_createWithExistingUsername() {
        // Arrange
        given(tipperService.create(any(TipperDto.class)))
                .willThrow(new DuplicateResourceException("Tipper with username erika already exists"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tippers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Tipper with username erika already exists");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(tipperService.update(eq(3L), any(TipperDto.class))).willReturn(erika());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tippers/{id}", 3L)
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
    void should_return404_when_updateUnknownTipper() {
        // Arrange
        given(tipperService.update(eq(42L), any(TipperDto.class)))
                .willThrow(new ResourceNotFoundException("Tipper", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tippers/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingTipper() {
        // Arrange
        Long id = 3L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tippers/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownTipper() {
        // Arrange
        willThrow(new ResourceNotFoundException("Tipper", 42L)).given(tipperService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tippers/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
