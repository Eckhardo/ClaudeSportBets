package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CompetitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
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

@WebMvcTest(CompetitionController.class)
class CompetitionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CompetitionService competitionService;

    @Test
    void should_returnCompetitionDtos_when_getCompetitions() {
        // Arrange
        given(competitionService.findAll()).willReturn(List.of(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .competitionFamilyId(7L)
                .build()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competitions").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 1, "name": "Bundesliga 2026/27", "competitionFamilyId": 7}]
                        """);
    }

    @Test
    void should_returnCompetitionDto_when_getByExistingId() {
        // Arrange
        given(competitionService.findById(1L)).willReturn(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .competitionFamilyId(7L)
                .build());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competitions/{id}", 1L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 1, "name": "Bundesliga 2026/27", "competitionFamilyId": 7}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(competitionService.findById(42L)).willThrow(new ResourceNotFoundException("Competition", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competitions/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return400_when_getByNonNumericId() {
        // Arrange
        String nonNumericId = "abc";

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competitions/{id}", nonNumericId).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(competitionService.create(any(CompetitionDto.class))).willReturn(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Bundesliga 2026/27", "winMultiplicator": 3, "remisMultiplicator": 1, "competitionFamilyId": 7}
                        """)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/competitions/1")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 1, "name": "Bundesliga 2026/27", "competitionFamilyId": 7}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "winMultiplicator": -1}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "winMultiplicator", "remisMultiplicator", "competitionFamilyId");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(competitionService.update(eq(1L), any(CompetitionDto.class))).willReturn(CompetitionDto.builder()
                .id(1L)
                .name("New name")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competitions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "New name", "winMultiplicator": 3, "remisMultiplicator": 1, "competitionFamilyId": 7}
                        """)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 1, "name": "New name", "competitionFamilyId": 7}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_updateWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": "", "winMultiplicator": 3, "remisMultiplicator": 1}
                """;

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competitions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "competitionFamilyId");
    }

    @Test
    void should_return404_when_updateUnknownCompetition() {
        // Arrange
        given(competitionService.update(eq(42L), any(CompetitionDto.class)))
                .willThrow(new ResourceNotFoundException("Competition", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competitions/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "New name", "winMultiplicator": 3, "remisMultiplicator": 1, "competitionFamilyId": 7}
                        """)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingCompetition() {
        // Arrange
        Long id = 1L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competitions/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownCompetition() {
        // Arrange
        willThrow(new ResourceNotFoundException("Competition", 42L)).given(competitionService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competitions/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return422WithMessage_when_deleteCompetitionWithDependents() {
        // Arrange
        willThrow(new BusinessRuleException("Competition with id 1 cannot be deleted, it still has rounds"))
                .given(competitionService).delete(1L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competitions/{id}", 1L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Competition with id 1 cannot be deleted, it still has rounds");
    }

    @Test
    void should_return409_when_deleteCompetitionStillReferenced() {
        // Arrange
        willThrow(new DataIntegrityViolationException("FK_ROUND_TO_COMP")).given(competitionService).delete(1L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competitions/{id}", 1L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void should_return404_when_createWithUnknownFamily() {
        // Arrange
        given(competitionService.create(any(CompetitionDto.class)))
                .willThrow(new ResourceNotFoundException("CompetitionFamily", 7L));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Bundesliga 2026/27", "winMultiplicator": 3, "remisMultiplicator": 1, "competitionFamilyId": 7}
                        """)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
