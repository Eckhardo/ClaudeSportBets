package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.CompetitionRoundDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionGroup;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionGroupRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
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
class CompetitionRoundControllerIntegrationTest {

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
    private SpieltagRepository spieltagRepository;

    @Autowired
    private CompetitionGroupRepository competitionGroupRepository;

    private WebTestClient client;
    private Competition competition;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/competition-rounds")
                .build();
        CompetitionFamily family = competitionFamilyRepository.save(CompetitionFamily.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());
        competition = saveCompetition("Bundesliga 2026/27", family);
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Deleting the families cascades to competitions, rounds and everything below them.
    @AfterEach
    void tearDown() {
        competitionFamilyRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        CompetitionRoundDto request = roundRequest("Hinrunde", 1, competition.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        CompetitionRoundDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/competition-rounds/\\d+")
                .expectBody(CompetitionRoundDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Hinrunde");
        assertThat(created.getCompetitionId()).isEqualTo(competition.getId());
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(competitionRoundRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "roundNumber": 0, "firstMatchday": 1, "matchdaysSize": 17, "teamsSize": 18}
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/competition-rounds");
    }

    @Test
    void should_return404_when_createWithUnknownCompetition() {
        // Arrange
        long unknownId = competition.getId() + 1000;
        CompetitionRoundDto request = roundRequest("Hinrunde", 1, unknownId);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Competition with id " + unknownId + " not found");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithTakenRoundNumber() {
        // Arrange
        saveRound("Hinrunde", 1, competition);
        CompetitionRoundDto request = roundRequest("Rückrunde", 1, competition.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("CompetitionRound with round number 1 already exists for Competition "
                        + competition.getId());
        assertThat(competitionRoundRepository.count()).isEqualTo(1);
    }

    @Test
    void should_returnAllRounds_when_getAll() {
        // Arrange
        saveRound("Hinrunde", 1, competition);
        saveRound("Rückrunde", 2, competition);

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].name").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("Hinrunde", "Rückrunde"));
    }

    @Test
    void should_returnOnlyRoundsOfCompetitionOrderedByNumber_when_getWithCompetitionId() {
        // Arrange
        Competition other = saveCompetition("Bundesliga 2027/28", competition.getCompetitionFamily());
        saveRound("Rückrunde", 2, competition);
        saveRound("Hinrunde", 1, competition);
        saveRound("Andere Hinrunde", 1, other);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("competitionId", competition.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Hinrunde")
                .jsonPath("$[1].name").isEqualTo("Rückrunde");
    }

    @Test
    void should_returnRound_when_getByExistingId() {
        // Arrange
        CompetitionRound round = saveRound("Hinrunde", 1, competition);

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", round.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(round.getId())
                .jsonPath("$.name").isEqualTo("Hinrunde")
                .jsonPath("$.competitionId").isEqualTo(competition.getId());
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
                .jsonPath("$.message").isEqualTo("CompetitionRound with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        CompetitionRound round = saveRound("Old name", 1, competition);
        CompetitionRoundDto request = CompetitionRoundDto.builder()
                .name("New name")
                .roundNumber(1)
                .firstMatchday(18)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(true)
                .competitionId(competition.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", round.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(round.getId())
                .jsonPath("$.name").isEqualTo("New name");
        assertThat(competitionRoundRepository.findById(round.getId()))
                .get()
                .extracting(CompetitionRound::getName, CompetitionRound::getFirstMatchday, CompetitionRound::isHasGroups)
                .containsExactly("New name", 18, true);
    }

    @Test
    void should_return409AndKeepRoundNumber_when_updateToRoundNumberOfOtherRound() {
        // Arrange
        saveRound("Hinrunde", 1, competition);
        CompetitionRound round = saveRound("Rückrunde", 2, competition);
        CompetitionRoundDto request = roundRequest("Rückrunde", 1, competition.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", round.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(competitionRoundRepository.findById(round.getId()))
                .get()
                .extracting(CompetitionRound::getRoundNumber)
                .isEqualTo(2);
    }

    @Test
    void should_return404_when_updateUnknownRound() {
        // Arrange
        CompetitionRoundDto request = roundRequest("Hinrunde", 1, competition.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndCascadeToSpieltageAndGroups_when_deleteRoundWithDependents() {
        // Arrange
        CompetitionRound round = saveRound("Hinrunde", 1, competition);
        Spieltag spieltag = spieltagRepository.save(Spieltag.builder()
                .spieltagNumber(1)
                .competitionRound(round)
                .build());
        CompetitionGroup group = competitionGroupRepository.save(CompetitionGroup.builder()
                .name("Gruppe A")
                .groupNumber(1)
                .competitionRound(round)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", round.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(competitionRoundRepository.existsById(round.getId())).isFalse();
        assertThat(spieltagRepository.existsById(spieltag.getId())).isFalse();
        assertThat(competitionGroupRepository.existsById(group.getId())).isFalse();
        assertThat(competitionRepository.existsById(competition.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownRound() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private Competition saveCompetition(String name, CompetitionFamily family) {
        return competitionRepository.save(Competition.builder()
                .name(name)
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(family)
                .build());
    }

    private CompetitionRound saveRound(String name, int roundNumber, Competition owner) {
        return competitionRoundRepository.save(CompetitionRound.builder()
                .name(name)
                .roundNumber(roundNumber)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competition(owner)
                .build());
    }

    private static CompetitionRoundDto roundRequest(String name, int roundNumber, Long competitionId) {
        return CompetitionRoundDto.builder()
                .name(name)
                .roundNumber(roundNumber)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competitionId(competitionId)
                .build();
    }
}
