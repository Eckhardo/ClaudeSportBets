package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.SpielFormulaDto;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.SpielFormulaService;
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

@WebMvcTest(SpielFormulaController.class)
class SpielFormulaControllerTest {

    private static final String VALID_REQUEST = """
            {"spielId": 11, "teamName": "FC Bayern München", "teamNameAcronym": "FCB", "heimTeam": true,
             "heimTore": 2, "gastTore": 1, "diffTore": 1, "won": 1, "remis": 0, "lost": 0, "points": 3}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SpielFormulaService spielFormulaService;

    private static SpielFormulaDto bayernFormula() {
        return SpielFormulaDto.builder()
                .id(30L)
                .spielId(11L)
                .teamName("FC Bayern München")
                .heimTeam(true)
                .points(3)
                .build();
    }

    @Test
    void should_returnAllSpielFormulaDtos_when_getWithoutFilter() {
        // Arrange
        given(spielFormulaService.findAll()).willReturn(List.of(bayernFormula()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiel-formulas").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 30, "spielId": 11, "teamName": "FC Bayern München"}]
                        """);
    }

    @Test
    void should_returnSpielFormulaDtosOfSpiel_when_getWithSpielId() {
        // Arrange
        given(spielFormulaService.findBySpielId(11L)).willReturn(List.of(bayernFormula()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiel-formulas?spielId={id}", 11L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 30, "spielId": 11}]
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(spielFormulaService.findById(42L)).willThrow(new ResourceNotFoundException("SpielFormula", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiel-formulas/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(spielFormulaService.create(any(SpielFormulaDto.class))).willReturn(bayernFormula());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiel-formulas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/spiel-formulas/30")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 30, "points": 3}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"teamName": " ", "heimTeam": true, "heimTore": -1, "gastTore": 0, "diffTore": -1,
                 "won": 0, "remis": 0, "lost": 1, "points": 0}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiel-formulas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("spielId", "teamName", "heimTore");
    }

    @Test
    void should_return404_when_createForUnknownSpiel() {
        // Arrange
        given(spielFormulaService.create(any(SpielFormulaDto.class))).willThrow(new ResourceNotFoundException("Spiel", 11L));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiel-formulas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(spielFormulaService.update(eq(30L), any(SpielFormulaDto.class))).willReturn(bayernFormula());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spiel-formulas/{id}", 30L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 30, "teamName": "FC Bayern München"}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownSpielFormula() {
        // Arrange
        given(spielFormulaService.update(eq(42L), any(SpielFormulaDto.class)))
                .willThrow(new ResourceNotFoundException("SpielFormula", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spiel-formulas/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingSpielFormula() {
        // Arrange
        Long id = 30L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spiel-formulas/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownSpielFormula() {
        // Arrange
        willThrow(new ResourceNotFoundException("SpielFormula", 42L)).given(spielFormulaService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spiel-formulas/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
