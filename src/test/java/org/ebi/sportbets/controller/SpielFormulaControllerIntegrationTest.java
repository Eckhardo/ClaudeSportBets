package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.SpielFormulaDto;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.SpielFormula;
import org.ebi.sportbets.repository.SpielFormulaRepository;
import org.ebi.sportbets.repository.SpielRepository;
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
class SpielFormulaControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private SpielFormulaRepository spielFormulaRepository;

    @Autowired
    private SpielRepository spielRepository;

    private WebTestClient client;
    private Spiel spiel;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/spiel-formulas")
                .build();
        spiel = spielRepository.save(Spiel.builder().spielNumber(1).heimTore(2).gastTore(1).build());
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Deleting the Spiele cascades to their formulas.
    @AfterEach
    void tearDown() {
        spielRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        SpielFormulaDto request = heimWinRequest(spiel.getId()).build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        SpielFormulaDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/spiel-formulas/\\d+")
                .expectBody(SpielFormulaDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created)
                .extracting(SpielFormulaDto::getSpielId, SpielFormulaDto::getTeamName, SpielFormulaDto::getPoints)
                .containsExactly(spiel.getId(), "FC Bayern München", 3);
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(spielFormulaRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return201WithNegativeDiff_when_createForLosingTeam() {
        // Arrange
        SpielFormulaDto request = SpielFormulaDto.builder()
                .spielId(spiel.getId())
                .teamName("Borussia Dortmund")
                .heimTeam(false)
                .heimTore(2)
                .gastTore(1)
                .diffTore(-1)
                .won(0)
                .remis(0)
                .lost(1)
                .points(0)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.diffTore").isEqualTo(-1)
                .jsonPath("$.heimTeam").isEqualTo(false);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"teamName": " ", "heimTeam": true, "heimTore": -1, "gastTore": 0, "diffTore": -1,
                 "won": 0, "remis": 0, "lost": 1, "points": 0}
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/spiel-formulas");
    }

    @Test
    void should_return404AndPersistNothing_when_createForUnknownSpiel() {
        // Arrange
        long unknownId = spiel.getId() + 1000;
        SpielFormulaDto request = heimWinRequest(unknownId).build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Spiel with id " + unknownId + " not found");
        assertThat(spielFormulaRepository.count()).isZero();
    }

    @Test
    void should_returnAllSpielFormulas_when_getAll() {
        // Arrange
        Spiel other = spielRepository.save(Spiel.builder().spielNumber(2).build());
        saveFormula(spiel, "FC Bayern München");
        saveFormula(other, "Borussia Dortmund");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].teamName").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("FC Bayern München", "Borussia Dortmund"));
    }

    @Test
    void should_returnOnlyFormulasOfSpiel_when_getWithSpielId() {
        // Arrange
        Spiel other = spielRepository.save(Spiel.builder().spielNumber(2).build());
        saveFormula(spiel, "FC Bayern München");
        saveFormula(spiel, "Borussia Dortmund");
        saveFormula(other, "VfB Stuttgart");

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("spielId", spiel.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].teamName").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("FC Bayern München", "Borussia Dortmund"));
    }

    @Test
    void should_returnSpielFormula_when_getByExistingId() {
        // Arrange
        SpielFormula formula = saveFormula(spiel, "FC Bayern München");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", formula.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(formula.getId())
                .jsonPath("$.spielId").isEqualTo(spiel.getId())
                .jsonPath("$.teamName").isEqualTo("FC Bayern München");
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
                .jsonPath("$.message").isEqualTo("SpielFormula with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        SpielFormula formula = saveFormula(spiel, "Bayern");
        SpielFormulaDto request = heimWinRequest(spiel.getId())
                .teamName("FC Bayern München")
                .heimTore(3)
                .diffTore(2)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", formula.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(formula.getId())
                .jsonPath("$.teamName").isEqualTo("FC Bayern München");
        assertThat(spielFormulaRepository.findById(formula.getId()))
                .get()
                .extracting(SpielFormula::getTeamName, SpielFormula::getHeimTore, SpielFormula::getDiffTore)
                .containsExactly("FC Bayern München", 3, 2);
    }

    @Test
    void should_return404_when_updateUnknownSpielFormula() {
        // Arrange
        SpielFormulaDto request = heimWinRequest(spiel.getId()).build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndKeepSpiel_when_deleteSpielFormula() {
        // Arrange
        SpielFormula formula = saveFormula(spiel, "FC Bayern München");

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", formula.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(spielFormulaRepository.existsById(formula.getId())).isFalse();
        assertThat(spielRepository.existsById(spiel.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownSpielFormula() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private SpielFormula saveFormula(Spiel owner, String teamName) {
        return spielFormulaRepository.save(SpielFormula.builder()
                .spiel(owner)
                .teamName(teamName)
                .heimTeam(true)
                .heimTore(2)
                .gastTore(1)
                .diffTore(1)
                .won(1)
                .points(3)
                .build());
    }

    private static SpielFormulaDto.SpielFormulaDtoBuilder heimWinRequest(Long spielId) {
        return SpielFormulaDto.builder()
                .spielId(spielId)
                .teamName("FC Bayern München")
                .teamNameAcronym("FCB")
                .heimTeam(true)
                .heimTore(2)
                .gastTore(1)
                .diffTore(1)
                .won(1)
                .remis(0)
                .lost(0)
                .points(3);
    }
}
