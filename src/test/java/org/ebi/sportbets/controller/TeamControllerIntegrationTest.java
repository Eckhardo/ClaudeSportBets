package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.TeamDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionTeam;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionTeamRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class TeamControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SpielRepository spielRepository;

    @Autowired
    private CompetitionTeamRepository competitionTeamRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionFamilyRepository competitionFamilyRepository;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/teams")
                .build();
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Teams are not cascaded from anything, so the rows referencing them are removed first.
    @AfterEach
    void tearDown() {
        spielRepository.deleteAll();
        competitionFamilyRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        TeamDto request = teamRequest("FC Bayern München", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        TeamDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/teams/\\d+")
                .expectBody(TeamDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("FC Bayern München");
        assertThat(created.getAcronym()).isEqualTo("FCB");
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(teamRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " "}
                """;

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.length()").isEqualTo(3)
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/teams");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithExistingName() {
        // Arrange
        saveTeam("FC Bayern München", "FCB");
        TeamDto request = teamRequest("FC Bayern München", "BAY");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Team with name FC Bayern München already exists");
        assertThat(teamRepository.count()).isEqualTo(1);
    }

    @Test
    void should_return409AndPersistNothing_when_createWithExistingAcronym() {
        // Arrange
        saveTeam("FC Bayern München", "FCB");
        TeamDto request = teamRequest("FC Barcelona", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Team with acronym FCB already exists");
        assertThat(teamRepository.count()).isEqualTo(1);
    }

    @Test
    void should_returnAllTeams_when_getAll() {
        // Arrange
        saveTeam("FC Bayern München", "FCB");
        saveTeam("Borussia Dortmund", "BVB");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].acronym").value(acronyms -> assertThat(acronyms)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("FCB", "BVB"));
    }

    @Test
    void should_returnTeam_when_getByExistingId() {
        // Arrange
        Team team = saveTeam("FC Bayern München", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", team.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(team.getId())
                .jsonPath("$.name").isEqualTo("FC Bayern München")
                .jsonPath("$.hasClub").isEqualTo(true);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Team with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Team team = saveTeam("Bayern", "BAY");
        TeamDto request = TeamDto.builder()
                .name("FC Bayern München")
                .acronym("FCB")
                .hasClub(false)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", team.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(team.getId())
                .jsonPath("$.acronym").isEqualTo("FCB");
        assertThat(teamRepository.findById(team.getId()))
                .get()
                .extracting(Team::getName, Team::getAcronym, Team::isHasClub)
                .containsExactly("FC Bayern München", "FCB", false);
    }

    @Test
    void should_return200_when_updateKeepsOwnNameAndAcronym() {
        // Arrange
        Team team = saveTeam("FC Bayern München", "FCB");
        TeamDto request = teamRequest("FC Bayern München", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", team.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    void should_return409AndKeepAcronym_when_updateToAcronymOfOtherTeam() {
        // Arrange
        saveTeam("Borussia Dortmund", "BVB");
        Team team = saveTeam("FC Bayern München", "FCB");
        TeamDto request = teamRequest("FC Bayern München", "BVB");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", team.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(teamRepository.findById(team.getId()))
                .get()
                .extracting(Team::getAcronym)
                .isEqualTo("FCB");
    }

    @Test
    void should_return404_when_updateUnknownTeam() {
        // Arrange
        TeamDto request = teamRequest("FC Bayern München", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndRemove_when_deleteUnusedTeam() {
        // Arrange
        Team team = saveTeam("FC Bayern München", "FCB");

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", team.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(teamRepository.existsById(team.getId())).isFalse();
    }

    @Test
    void should_return422AndKeepTeam_when_deleteTeamUsedAsGastTeamOfSpiel() {
        // Arrange
        Team team = saveTeam("FC Bayern München", "FCB");
        spielRepository.save(Spiel.builder()
                .spielNumber(1)
                .gastTeam(team)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", team.getId()).exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Team with id " + team.getId() + " is still used by a Spiel or Competition");
        assertThat(teamRepository.existsById(team.getId())).isTrue();
    }

    @Test
    void should_return422AndKeepTeam_when_deleteTeamAssignedToCompetition() {
        // Arrange
        Team team = saveTeam("FC Bayern München", "FCB");
        CompetitionFamily family = competitionFamilyRepository.save(CompetitionFamily.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());
        Competition competition = competitionRepository.save(Competition.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(family)
                .build());
        competitionTeamRepository.save(CompetitionTeam.builder()
                .competition(competition)
                .team(team)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", team.getId()).exchange();

        // Assert
        response.expectStatus().isEqualTo(422);
        assertThat(teamRepository.existsById(team.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownTeam() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private Team saveTeam(String name, String acronym) {
        return teamRepository.save(Team.builder()
                .name(name)
                .acronym(acronym)
                .hasClub(true)
                .build());
    }

    private static TeamDto teamRequest(String name, String acronym) {
        return TeamDto.builder()
                .name(name)
                .acronym(acronym)
                .hasClub(true)
                .build();
    }
}
