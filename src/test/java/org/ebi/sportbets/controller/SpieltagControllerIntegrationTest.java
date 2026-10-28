package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpielRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class SpieltagControllerIntegrationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 21, 20, 30);

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
    private SpielRepository spielRepository;

    private WebTestClient client;
    private CompetitionRound round;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/spieltage")
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
        round = saveRound("Hinrunde", 1, competition);
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Deleting the families cascades to competitions, rounds, Spieltage and their Spiele.
    @AfterEach
    void tearDown() {
        competitionFamilyRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        SpieltagDto request = spieltagRequest(1, round.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        SpieltagDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/spieltage/\\d+")
                .expectBody(SpieltagDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getStartDate()).isEqualTo(START);
        assertThat(created.getCompetitionRoundId()).isEqualTo(round.getId());
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(spieltagRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"spieltagNumber": 0}
                """;

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.length()").isEqualTo(2)
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/spieltage");
    }

    @Test
    void should_return404_when_createWithUnknownRound() {
        // Arrange
        long unknownId = round.getId() + 1000;
        SpieltagDto request = spieltagRequest(1, unknownId);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("CompetitionRound with id " + unknownId + " not found");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithTakenNumber() {
        // Arrange
        saveSpieltag(1, round);
        SpieltagDto request = spieltagRequest(1, round.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Spieltag with number 1 already exists for CompetitionRound " + round.getId());
        assertThat(spieltagRepository.count()).isEqualTo(1);
    }

    @Test
    void should_returnAllSpieltage_when_getAll() {
        // Arrange
        saveSpieltag(1, round);
        saveSpieltag(2, round);

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void should_returnOnlySpieltageOfRoundOrderedByNumber_when_getWithCompetitionRoundId() {
        // Arrange
        CompetitionRound other = saveRound("Rückrunde", 2, round.getCompetition());
        saveSpieltag(2, round);
        saveSpieltag(1, round);
        saveSpieltag(18, other);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("competitionRoundId", round.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].spieltagNumber").isEqualTo(1)
                .jsonPath("$[1].spieltagNumber").isEqualTo(2);
    }

    @Test
    void should_returnSpieltag_when_getByExistingId() {
        // Arrange
        Spieltag spieltag = saveSpieltag(1, round);

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", spieltag.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(spieltag.getId())
                .jsonPath("$.spieltagNumber").isEqualTo(1)
                .jsonPath("$.competitionRoundId").isEqualTo(round.getId());
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
                .jsonPath("$.message").isEqualTo("Spieltag with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Spieltag spieltag = saveSpieltag(1, round);
        SpieltagDto request = SpieltagDto.builder()
                .spieltagNumber(3)
                .startDate(START.plusWeeks(2))
                .competitionRoundId(round.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", spieltag.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(spieltag.getId())
                .jsonPath("$.spieltagNumber").isEqualTo(3);
        assertThat(spieltagRepository.findById(spieltag.getId()))
                .get()
                .extracting(Spieltag::getSpieltagNumber, Spieltag::getStartDate)
                .containsExactly(3, START.plusWeeks(2));
    }

    @Test
    void should_return409AndKeepNumber_when_updateToNumberOfOtherSpieltag() {
        // Arrange
        saveSpieltag(1, round);
        Spieltag spieltag = saveSpieltag(2, round);
        SpieltagDto request = spieltagRequest(1, round.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", spieltag.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(spieltagRepository.findById(spieltag.getId()))
                .get()
                .extracting(Spieltag::getSpieltagNumber)
                .isEqualTo(2);
    }

    @Test
    void should_return404_when_updateUnknownSpieltag() {
        // Arrange
        SpieltagDto request = spieltagRequest(1, round.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndCascadeToSpiele_when_deleteSpieltagWithSpiele() {
        // Arrange
        Spieltag spieltag = saveSpieltag(1, round);
        Spiel spiel = spielRepository.save(Spiel.builder()
                .spielNumber(1)
                .spieltag(spieltag)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", spieltag.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(spieltagRepository.existsById(spieltag.getId())).isFalse();
        assertThat(spielRepository.existsById(spiel.getId())).isFalse();
        assertThat(competitionRoundRepository.existsById(round.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownSpieltag() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private CompetitionRound saveRound(String name, int roundNumber, Competition competition) {
        return competitionRoundRepository.save(CompetitionRound.builder()
                .name(name)
                .roundNumber(roundNumber)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competition(competition)
                .build());
    }

    private Spieltag saveSpieltag(int spieltagNumber, CompetitionRound owner) {
        return spieltagRepository.save(Spieltag.builder()
                .spieltagNumber(spieltagNumber)
                .startDate(START)
                .competitionRound(owner)
                .build());
    }

    private static SpieltagDto spieltagRequest(int spieltagNumber, Long competitionRoundId) {
        return SpieltagDto.builder()
                .spieltagNumber(spieltagNumber)
                .startDate(START)
                .competitionRoundId(competitionRoundId)
                .build();
    }
}
