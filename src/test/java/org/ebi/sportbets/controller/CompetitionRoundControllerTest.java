package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CompetitionRoundDto;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CompetitionRoundService;
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

@WebMvcTest(CompetitionRoundController.class)
class   CompetitionRoundControllerTest {

    private static final String VALID_REQUEST = """
            {"name": "Hinrunde", "roundNumber": 1, "firstMatchday": 1, "matchdaysSize": 17,
             "teamsSize": 18, "hasGroups": false, "competitionId": 3}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CompetitionRoundService competitionRoundService;

    private static CompetitionRoundDto hinrunde() {
        return CompetitionRoundDto.builder()
                .id(5L)
                .name("Hinrunde")
                .roundNumber(1)
                .competitionId(3L)
                .build();
    }

    @Test
    void should_returnAllRoundDtos_when_getRoundsWithoutFilter() {
        // Arrange
        given(competitionRoundService.findAll()).willReturn(List.of(hinrunde()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-rounds").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 5, "name": "Hinrunde", "roundNumber": 1, "competitionId": 3}]
                        """);
    }

    @Test
    void should_returnRoundDtosOfCompetition_when_getRoundsWithCompetitionId() {
        // Arrange
        given(competitionRoundService.findByCompetitionId(3L)).willReturn(List.of(hinrunde()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-rounds?competitionId={id}", 3L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 5, "competitionId": 3}]
                        """);
    }

    @Test
    void should_returnRoundDto_when_getByExistingId() {
        // Arrange
        given(competitionRoundService.findById(5L)).willReturn(hinrunde());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-rounds/{id}", 5L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 5, "name": "Hinrunde"}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(competitionRoundService.findById(42L)).willThrow(new ResourceNotFoundException("CompetitionRound", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-rounds/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(competitionRoundService.create(any(CompetitionRoundDto.class))).willReturn(hinrunde());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-rounds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/competition-rounds/5")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 5, "name": "Hinrunde", "competitionId": 3}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "roundNumber": 0, "firstMatchday": 1, "matchdaysSize": 17, "teamsSize": 18}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-rounds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "roundNumber", "hasGroups", "competitionId");
    }

    @Test
    void should_return404_when_createForUnknownCompetition() {
        // Arrange
        given(competitionRoundService.create(any(CompetitionRoundDto.class)))
                .willThrow(new ResourceNotFoundException("Competition", 3L));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-rounds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return409_when_createWithTakenRoundNumber() {
        // Arrange
        given(competitionRoundService.create(any(CompetitionRoundDto.class)))
                .willThrow(new DuplicateResourceException("CompetitionRound with round number 1 already exists for Competition 3"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-rounds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("CompetitionRound with round number 1 already exists for Competition 3");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(competitionRoundService.update(eq(5L), any(CompetitionRoundDto.class))).willReturn(hinrunde());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-rounds/{id}", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 5, "name": "Hinrunde", "competitionId": 3}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_updateWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": "Hinrunde", "roundNumber": 1, "firstMatchday": -1, "hasGroups": false, "competitionId": 3}
                """;

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-rounds/{id}", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("firstMatchday", "matchdaysSize", "teamsSize");
    }

    @Test
    void should_return404_when_updateUnknownRound() {
        // Arrange
        given(competitionRoundService.update(eq(42L), any(CompetitionRoundDto.class)))
                .willThrow(new ResourceNotFoundException("CompetitionRound", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-rounds/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingRound() {
        // Arrange
        Long id = 5L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-rounds/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownRound() {
        // Arrange
        willThrow(new ResourceNotFoundException("CompetitionRound", 42L)).given(competitionRoundService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-rounds/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
