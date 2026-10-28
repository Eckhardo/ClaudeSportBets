package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.SpieltagService;
import org.junit.jupiter.api.Test;
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
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(SpieltagController.class)
class SpieltagControllerTest {

    private static final String VALID_REQUEST = """
            {"spieltagNumber": 1, "startDate": "2026-08-21T20:30:00", "competitionRoundId": 5}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SpieltagService spieltagService;

    private static SpieltagDto spieltag1() {
        return SpieltagDto.builder().id(8L).spieltagNumber(1).competitionRoundId(5L).build();
    }

    @Test
    void should_returnAllSpieltagDtos_when_getSpieltageWithoutFilter() {
        // Arrange
        given(spieltagService.findAll()).willReturn(List.of(spieltag1()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spieltage").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 8, "spieltagNumber": 1, "competitionRoundId": 5}]
                        """);
    }

    @Test
    void should_returnSpieltagDtosOfRound_when_getSpieltageWithCompetitionRoundId() {
        // Arrange
        given(spieltagService.findByCompetitionRoundId(5L)).willReturn(List.of(spieltag1()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spieltage?competitionRoundId={id}", 5L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 8, "competitionRoundId": 5}]
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(spieltagService.findById(42L)).willThrow(new ResourceNotFoundException("Spieltag", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spieltage/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(spieltagService.create(any(SpieltagDto.class))).willReturn(spieltag1());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spieltage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/spieltage/8")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 8, "spieltagNumber": 1}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"spieltagNumber": 0}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spieltage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("spieltagNumber", "competitionRoundId");
    }

    @Test
    void should_return409_when_createWithTakenNumber() {
        // Arrange
        given(spieltagService.create(any(SpieltagDto.class)))
                .willThrow(new DuplicateResourceException("Spieltag with number 1 already exists for CompetitionRound 5"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spieltage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(spieltagService.update(eq(8L), any(SpieltagDto.class))).willReturn(spieltag1());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spieltage/{id}", 8L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 8, "spieltagNumber": 1}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownSpieltag() {
        // Arrange
        given(spieltagService.update(eq(42L), any(SpieltagDto.class)))
                .willThrow(new ResourceNotFoundException("Spieltag", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spieltage/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingSpieltag() {
        // Arrange
        Long id = 8L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spieltage/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownSpieltag() {
        // Arrange
        willThrow(new ResourceNotFoundException("Spieltag", 42L)).given(spieltagService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spieltage/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
