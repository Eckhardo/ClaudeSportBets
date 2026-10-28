package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.SpielDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionGroup;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionGroupRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class SpielControllerIntegrationTest {

    private static final LocalDateTime ANPFIFF = LocalDateTime.of(2026, 8, 21, 20, 30);

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private CompetitionFamilyRepository competitionFamilyRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionRoundRepository competitionRoundRepository;

    @Autowired
    private CompetitionGroupRepository competitionGroupRepository;

    @Autowired
    private SpieltagRepository spieltagRepository;

    @Autowired
    private SpielRepository spielRepository;

    @Autowired
    private TeamRepository teamRepository;

    private WebTestClient client;
    private Team bayern;
    private Team dortmund;
    private Spieltag spieltag;
    private CompetitionGroup group;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/spiele")
                .build();
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
        CompetitionRound round = competitionRoundRepository.save(CompetitionRound.builder()
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(true)
                .competition(competition)
                .build());
        spieltag = saveSpieltag(1, round);
        group = competitionGroupRepository.save(CompetitionGroup.builder()
                .name("Gruppe A")
                .groupNumber(1)
                .competitionRound(round)
                .build());
        bayern = saveTeam("FC Bayern München", "FCB");
        dortmund = saveTeam("Borussia Dortmund", "BVB");
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Spiele without a Spieltag and teams are not cascaded, so they are removed explicitly.
    @AfterEach
    void tearDown() {
        spielRepository.deleteAll();
        competitionFamilyRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersistReferences_when_createWithValidRequest() {
        // Arrange
        SpielDto request = spielRequest()
                .heimTeamId(bayern.getId())
                .gastTeamId(dortmund.getId())
                .spieltagId(spieltag.getId())
                .competitionGroupId(group.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        SpielDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/spiele/\\d+")
                .expectBody(SpielDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created)
                .extracting(SpielDto::getHeimTeamId, SpielDto::getGastTeamId, SpielDto::getSpieltagId,
                        SpielDto::getCompetitionGroupId, SpielDto::getAnpfiffDate)
                .containsExactly(bayern.getId(), dortmund.getId(), spieltag.getId(), group.getId(), ANPFIFF);
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(spielRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return201_when_createWithoutReferences() {
        // Arrange
        SpielDto request = spielRequest().build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.heimTeamId").isEmpty()
                .jsonPath("$.spieltagId").isEmpty();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"spielNumber": 0, "heimTore": -1}
                """;

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.length()").isEqualTo(4)
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/spiele");
    }

    @Test
    void should_return404AndPersistNothing_when_createWithUnknownTeam() {
        // Arrange
        long unknownId = dortmund.getId() + 1000;
        SpielDto request = spielRequest().heimTeamId(bayern.getId()).gastTeamId(unknownId).build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Team with id " + unknownId + " not found");
        assertThat(spielRepository.count()).isZero();
    }

    @Test
    void should_return422AndPersistNothing_when_createWithSameHeimAndGastTeam() {
        // Arrange
        SpielDto request = spielRequest().heimTeamId(bayern.getId()).gastTeamId(bayern.getId()).build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Heim team and Gast team must be different");
        assertThat(spielRepository.count()).isZero();
    }

    @Test
    void should_returnAllSpiele_when_getAll() {
        // Arrange
        saveSpiel(1, spieltag);
        saveSpiel(2, null);

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void should_returnOnlySpieleOfSpieltagOrderedByNumber_when_getWithSpieltagId() {
        // Arrange
        Spieltag other = saveSpieltag(2, spieltag.getCompetitionRound());
        saveSpiel(2, spieltag);
        saveSpiel(1, spieltag);
        saveSpiel(3, other);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("spieltagId", spieltag.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].spielNumber").isEqualTo(1)
                .jsonPath("$[1].spielNumber").isEqualTo(2);
    }

    @Test
    void should_returnSpiel_when_getByExistingId() {
        // Arrange
        Spiel spiel = saveSpiel(1, spieltag);

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", spiel.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(spiel.getId())
                .jsonPath("$.heimTeamId").isEqualTo(bayern.getId())
                .jsonPath("$.gastTeamId").isEqualTo(dortmund.getId())
                .jsonPath("$.spieltagId").isEqualTo(spieltag.getId());
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
                .jsonPath("$.message").isEqualTo("Spiel with id 999999 not found");
    }

    @Test
    void should_return200AndPersistResultAndSwappedTeams_when_updateWithValidRequest() {
        // Arrange
        Spiel spiel = saveSpiel(1, spieltag);
        SpielDto request = spielRequest()
                .heimTore(0)
                .gastTore(3)
                .heimTeamId(dortmund.getId())
                .gastTeamId(bayern.getId())
                .spieltagId(spieltag.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", spiel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(spiel.getId())
                .jsonPath("$.heimTeamId").isEqualTo(dortmund.getId())
                .jsonPath("$.gastTore").isEqualTo(3);
        assertThat(spielRepository.findById(spiel.getId()))
                .get()
                .extracting(Spiel::getHeimTore, Spiel::getGastTore, Spiel::isStattgefunden)
                .containsExactly(0, 3, true);
    }

    @Test
    void should_return200AndClearReferences_when_updateWithoutReferences() {
        // Arrange
        Spiel spiel = saveSpiel(1, spieltag);
        SpielDto request = spielRequest().build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", spiel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.heimTeamId").isEmpty()
                .jsonPath("$.gastTeamId").isEmpty()
                .jsonPath("$.spieltagId").isEmpty();
        assertThat(spielRepository.findBySpieltagIdOrderBySpielNumberAsc(spieltag.getId())).isEmpty();
    }

    @Test
    void should_return404_when_updateUnknownSpiel() {
        // Arrange
        SpielDto request = spielRequest().build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndKeepTeamsAndSpieltag_when_deleteSpiel() {
        // Arrange
        Spiel spiel = saveSpiel(1, spieltag);

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", spiel.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(spielRepository.existsById(spiel.getId())).isFalse();
        assertThat(teamRepository.existsById(bayern.getId())).isTrue();
        assertThat(spieltagRepository.existsById(spieltag.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownSpiel() {
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

    private Spieltag saveSpieltag(int spieltagNumber, CompetitionRound round) {
        return spieltagRepository.save(Spieltag.builder()
                .spieltagNumber(spieltagNumber)
                .competitionRound(round)
                .build());
    }

    private Spiel saveSpiel(int spielNumber, Spieltag owner) {
        return spielRepository.save(Spiel.builder()
                .spielNumber(spielNumber)
                .anpfiffDate(ANPFIFF)
                .heimTeam(bayern)
                .gastTeam(dortmund)
                .spieltag(owner)
                .build());
    }

    private static SpielDto.SpielDtoBuilder spielRequest() {
        return SpielDto.builder()
                .spielNumber(1)
                .anpfiffDate(ANPFIFF)
                .heimTore(2)
                .gastTore(1)
                .stattgefunden(true);
    }
}
