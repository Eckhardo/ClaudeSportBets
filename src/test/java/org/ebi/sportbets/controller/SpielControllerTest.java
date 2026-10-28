package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.SpielDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.SpielService;
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

@WebMvcTest(SpielController.class)
class SpielControllerTest {

    private static final String VALID_REQUEST = """
            {"spielNumber": 1, "heimTore": 2, "gastTore": 1, "stattgefunden": true,
             "heimTeamId": 1, "gastTeamId": 2, "spieltagId": 8}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SpielService spielService;

    private static SpielDto spiel1() {
        return SpielDto.builder()
                .id(11L)
                .spielNumber(1)
                .heimTore(2)
                .gastTore(1)
                .stattgefunden(true)
                .heimTeamId(1L)
                .gastTeamId(2L)
                .spieltagId(8L)
                .build();
    }

    @Test
    void should_returnAllSpielDtos_when_getSpieleWithoutFilter() {
        // Arrange
        given(spielService.findAll()).willReturn(List.of(spiel1()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiele").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 11, "heimTeamId": 1, "gastTeamId": 2, "spieltagId": 8}]
                        """);
    }

    @Test
    void should_returnSpielDtosOfSpieltag_when_getSpieleWithSpieltagId() {
        // Arrange
        given(spielService.findBySpieltagId(8L)).willReturn(List.of(spiel1()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiele?spieltagId={id}", 8L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 11, "spieltagId": 8}]
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(spielService.findById(42L)).willThrow(new ResourceNotFoundException("Spiel", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/spiele/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(spielService.create(any(SpielDto.class))).willReturn(spiel1());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiele")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/spiele/11")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 11, "heimTore": 2, "gastTore": 1}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"spielNumber": 0, "heimTore": -1}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiele")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("spielNumber", "heimTore", "gastTore", "stattgefunden");
    }

    @Test
    void should_return422_when_createWithSameHeimAndGastTeam() {
        // Arrange
        given(spielService.create(any(SpielDto.class)))
                .willThrow(new BusinessRuleException("Heim team and Gast team must be different"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/spiele")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Heim team and Gast team must be different");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(spielService.update(eq(11L), any(SpielDto.class))).willReturn(spiel1());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spiele/{id}", 11L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 11, "stattgefunden": true}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownSpiel() {
        // Arrange
        given(spielService.update(eq(42L), any(SpielDto.class))).willThrow(new ResourceNotFoundException("Spiel", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/spiele/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingSpiel() {
        // Arrange
        Long id = 11L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spiele/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownSpiel() {
        // Arrange
        willThrow(new ResourceNotFoundException("Spiel", 42L)).given(spielService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/spiele/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
