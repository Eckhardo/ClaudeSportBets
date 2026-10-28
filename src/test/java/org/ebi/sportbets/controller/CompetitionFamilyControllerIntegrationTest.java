package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.CompetitionFamilyDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
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
class CompetitionFamilyControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private CompetitionFamilyRepository competitionFamilyRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/competition-families")
                .build();
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
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Bundesliga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        CompetitionFamilyDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/competition-families/\\d+")
                .expectBody(CompetitionFamilyDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Bundesliga");
        assertThat(created.getCountry()).isEqualTo("Deutschland");
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(competitionFamilyRepository.existsById(created.getId())).isTrue();
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/competition-families");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithExistingName() {
        // Arrange
        saveFamily("Bundesliga");
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("CompetitionFamily with name Bundesliga already exists");
        assertThat(competitionFamilyRepository.count()).isEqualTo(1);
    }

    @Test
    void should_returnAllFamilies_when_getAll() {
        // Arrange
        saveFamily("Bundesliga");
        saveFamily("Premier League");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].name").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("Bundesliga", "Premier League"));
    }

    @Test
    void should_returnFamily_when_getByExistingId() {
        // Arrange
        CompetitionFamily family = saveFamily("Bundesliga");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", family.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(family.getId())
                .jsonPath("$.name").isEqualTo("Bundesliga")
                .jsonPath("$.hasClubs").isEqualTo(true);
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
                .jsonPath("$.message").isEqualTo("CompetitionFamily with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        CompetitionFamily family = saveFamily("Old name");
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("New name")
                .country("England")
                .hasClubs(false)
                .hasLigaModus(false)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", family.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(family.getId())
                .jsonPath("$.name").isEqualTo("New name");
        assertThat(competitionFamilyRepository.findById(family.getId()))
                .get()
                .extracting(CompetitionFamily::getName, CompetitionFamily::getCountry, CompetitionFamily::isHasClubs)
                .containsExactly("New name", "England", false);
    }

    @Test
    void should_return409AndKeepName_when_updateToNameOfOtherFamily() {
        // Arrange
        saveFamily("Premier League");
        CompetitionFamily family = saveFamily("Bundesliga");
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Premier League")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", family.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(competitionFamilyRepository.findById(family.getId()))
                .get()
                .extracting(CompetitionFamily::getName)
                .isEqualTo("Bundesliga");
    }

    @Test
    void should_return404_when_updateUnknownFamily() {
        // Arrange
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndCascadeToCompetitions_when_deleteFamilyWithCompetitions() {
        // Arrange
        CompetitionFamily family = saveFamily("Bundesliga");
        Competition competition = competitionRepository.save(Competition.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(family)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", family.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(competitionFamilyRepository.existsById(family.getId())).isFalse();
        assertThat(competitionRepository.existsById(competition.getId())).isFalse();
    }

    @Test
    void should_return404_when_deleteUnknownFamily() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private CompetitionFamily saveFamily(String name) {
        return competitionFamilyRepository.save(CompetitionFamily.builder()
                .name(name)
                .hasClubs(true)
                .hasLigaModus(true)
                .build());
    }
}
