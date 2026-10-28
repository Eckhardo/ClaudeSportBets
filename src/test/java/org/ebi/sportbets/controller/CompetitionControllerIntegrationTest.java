package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
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
class CompetitionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionFamilyRepository competitionFamilyRepository;

    @Autowired
    private CompetitionRoundRepository competitionRoundRepository;

    private WebTestClient client;
    private CompetitionFamily family;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/competitions")
                .build();
        family = competitionFamilyRepository.save(CompetitionFamily.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Deleting the families cascades to competitions and everything below them.
    @AfterEach
    void tearDown() {
        competitionFamilyRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        CompetitionDto request = CompetitionDto.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(family.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        CompetitionDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/competitions/\\d+")
                .expectBody(CompetitionDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Bundesliga 2026/27");
        assertThat(created.getCompetitionFamilyId()).isEqualTo(family.getId());
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(competitionRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "winMultiplicator": -1}
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/competitions");
    }

    @Test
    void should_return404_when_createWithUnknownFamily() {
        // Arrange
        CompetitionDto request = CompetitionDto.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(family.getId() + 1000)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("CompetitionFamily with id " + (family.getId() + 1000) + " not found");
    }

    @Test
    void should_returnAllCompetitions_when_getAll() {
        // Arrange
        saveCompetition("Bundesliga 2026/27");
        saveCompetition("Bundesliga 2027/28");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].name").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("Bundesliga 2026/27", "Bundesliga 2027/28"));
    }

    @Test
    void should_returnCompetition_when_getByExistingId() {
        // Arrange
        Competition competition = saveCompetition("Bundesliga 2026/27");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", competition.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(competition.getId())
                .jsonPath("$.name").isEqualTo("Bundesliga 2026/27")
                .jsonPath("$.competitionFamilyId").isEqualTo(family.getId());
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
                .jsonPath("$.message").isEqualTo("Competition with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Competition competition = saveCompetition("Old name");
        CompetitionDto request = CompetitionDto.builder()
                .name("New name")
                .description("Updated")
                .winMultiplicator(4)
                .remisMultiplicator(2)
                .competitionFamilyId(family.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", competition.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(competition.getId())
                .jsonPath("$.name").isEqualTo("New name");
        assertThat(competitionRepository.findById(competition.getId()))
                .get()
                .extracting(Competition::getName, Competition::getDescription, Competition::getWinMultiplicator)
                .containsExactly("New name", "Updated", 4);
    }

    @Test
    void should_return204AndRemove_when_deleteCompetitionWithoutDependents() {
        // Arrange
        Competition competition = saveCompetition("Bundesliga 2026/27");

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", competition.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(competitionRepository.existsById(competition.getId())).isFalse();
    }

    @Test
    void should_return204AndCascadeToRounds_when_deleteCompetitionWithRounds() {
        // Arrange
        Competition competition = saveCompetition("Bundesliga 2026/27");
        CompetitionRound round = competitionRoundRepository.save(CompetitionRound.builder()
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competition(competition)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", competition.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent();
        assertThat(competitionRepository.existsById(competition.getId())).isFalse();
        assertThat(competitionRoundRepository.existsById(round.getId())).isFalse();
    }

    private Competition saveCompetition(String name) {
        return competitionRepository.save(Competition.builder()
                .name(name)
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(family)
                .build());
    }
}
