package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CompetitionFamilyDto;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CompetitionFamilyService;
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

@WebMvcTest(CompetitionFamilyController.class)
class CompetitionFamilyControllerTest {

    private static final String VALID_REQUEST = """
            {"name": "Bundesliga", "country": "Deutschland", "hasClubs": true, "hasLigaModus": true}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CompetitionFamilyService competitionFamilyService;

    @Test
    void should_returnFamilyDtos_when_getFamilies() {
        // Arrange
        given(competitionFamilyService.findAll()).willReturn(List.of(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-families").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 7, "name": "Bundesliga", "hasClubs": true, "hasLigaModus": true}]
                        """);
    }

    @Test
    void should_returnFamilyDto_when_getByExistingId() {
        // Arrange
        given(competitionFamilyService.findById(7L)).willReturn(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .build());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-families/{id}", 7L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "name": "Bundesliga"}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(competitionFamilyService.findById(42L)).willThrow(new ResourceNotFoundException("CompetitionFamily", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-families/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(competitionFamilyService.create(any(CompetitionFamilyDto.class))).willReturn(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/competition-families/7")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "name": "Bundesliga", "country": "Deutschland"}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " "}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "hasClubs", "hasLigaModus");
    }

    @Test
    void should_return409_when_createWithExistingName() {
        // Arrange
        given(competitionFamilyService.create(any(CompetitionFamilyDto.class)))
                .willThrow(new DuplicateResourceException("CompetitionFamily with name Bundesliga already exists"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("CompetitionFamily with name Bundesliga already exists");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(competitionFamilyService.update(eq(7L), any(CompetitionFamilyDto.class))).willReturn(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-families/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "name": "Bundesliga", "country": "Deutschland"}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_updateWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": "", "hasClubs": true}
                """;

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-families/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "hasLigaModus");
    }

    @Test
    void should_return404_when_updateUnknownFamily() {
        // Arrange
        given(competitionFamilyService.update(eq(42L), any(CompetitionFamilyDto.class)))
                .willThrow(new ResourceNotFoundException("CompetitionFamily", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-families/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingFamily() {
        // Arrange
        Long id = 7L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-families/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownFamily() {
        // Arrange
        willThrow(new ResourceNotFoundException("CompetitionFamily", 42L)).given(competitionFamilyService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-families/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
