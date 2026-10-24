package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CompetitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
    void should_return201WithBody_when_createWithValidRequest() {
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
