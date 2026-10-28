package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.TeamDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.TeamService;
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

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    private static final String VALID_REQUEST = """
            {"name": "FC Bayern München", "acronym": "FCB", "hasClub": true}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TeamService teamService;

    private static TeamDto bayern() {
        return TeamDto.builder().id(4L).name("FC Bayern München").acronym("FCB").hasClub(true).build();
    }

    @Test
    void should_returnTeamDtos_when_getTeams() {
        // Arrange
        given(teamService.findAll()).willReturn(List.of(bayern()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/teams").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 4, "name": "FC Bayern München", "acronym": "FCB"}]
                        """);
    }

    @Test
    void should_returnTeamDto_when_getByExistingId() {
        // Arrange
        given(teamService.findById(4L)).willReturn(bayern());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/teams/{id}", 4L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 4, "acronym": "FCB"}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(teamService.findById(42L)).willThrow(new ResourceNotFoundException("Team", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/teams/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(teamService.create(any(TeamDto.class))).willReturn(bayern());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/teams/4")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 4, "name": "FC Bayern München"}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " "}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "acronym", "hasClub");
    }

    @Test
    void should_return409_when_createWithExistingName() {
        // Arrange
        given(teamService.create(any(TeamDto.class)))
                .willThrow(new DuplicateResourceException("Team with name FC Bayern München already exists"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Team with name FC Bayern München already exists");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(teamService.update(eq(4L), any(TeamDto.class))).willReturn(bayern());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/teams/{id}", 4L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 4, "name": "FC Bayern München"}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownTeam() {
        // Arrange
        given(teamService.update(eq(42L), any(TeamDto.class))).willThrow(new ResourceNotFoundException("Team", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/teams/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingTeam() {
        // Arrange
        Long id = 4L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/teams/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return422_when_deleteTeamInUse() {
        // Arrange
        willThrow(new BusinessRuleException("Team with id 4 is still used by a Spiel or Competition"))
                .given(teamService).delete(4L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/teams/{id}", 4L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Team with id 4 is still used by a Spiel or Competition");
    }

    @Test
    void should_return404_when_deleteUnknownTeam() {
        // Arrange
        willThrow(new ResourceNotFoundException("Team", 42L)).given(teamService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/teams/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
