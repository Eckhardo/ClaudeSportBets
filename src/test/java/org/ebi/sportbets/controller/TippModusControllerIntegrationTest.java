package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.TippModusDto;
import org.ebi.sportbets.dto.TippModusPointDto;
import org.ebi.sportbets.dto.TippModusResultDto;
import org.ebi.sportbets.dto.TippModusTotoDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippConfig;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusPoint;
import org.ebi.sportbets.entity.TippModusResult;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.enums.TippModusType;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
import org.ebi.sportbets.repository.TippConfigRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.ebi.sportbets.repository.TippRepository;
import org.ebi.sportbets.repository.TipperRepository;
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
class TippModusControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TippModusRepository tippModusRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private TipperRepository tipperRepository;

    @Autowired
    private CommunityMembershipRepository communityMembershipRepository;

    @Autowired
    private CompetitionMembershipRepository competitionMembershipRepository;

    @Autowired
    private TippRepository tippRepository;

    @Autowired
    private TippConfigRepository tippConfigRepository;

    @Autowired
    private SpielRepository spielRepository;

    @Autowired
    private SpieltagRepository spieltagRepository;

    @Autowired
    private CompetitionRoundRepository competitionRoundRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionFamilyRepository competitionFamilyRepository;

    private WebTestClient client;
    private Community community;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/tipp-modi")
                .build();
        community = communityRepository.save(Community.builder().name("Stammtisch").build());
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Tippers and communities cascade to everything below them; Spiele and families are separate roots.
    @AfterEach
    void tearDown() {
        tipperRepository.deleteAll();
        communityRepository.deleteAll();
        spielRepository.deleteAll();
        competitionFamilyRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createPointModus() {
        // Arrange
        TippModusDto request = TippModusPointDto.builder()
                .name("Punkte")
                .deadline(30)
                .communityId(community.getId())
                .totalPoints(10)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        TippModusDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/tipp-modi/\\d+")
                .expectBody(TippModusDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isInstanceOf(TippModusPointDto.class);
        assertThat((TippModusPointDto) created)
                .extracting(TippModusDto::getName, TippModusDto::getType, TippModusDto::getDeadline,
                        TippModusDto::getCommunityId, TippModusPointDto::getTotalPoints)
                .containsExactly("Punkte", "PointTipp", 30, community.getId(), 10);
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(tippModusRepository.findById(created.getId()))
                .get()
                .isInstanceOf(TippModusPoint.class)
                .extracting(TippModus::getType)
                .isEqualTo(TippModusType.TIPPMODUS_POINT);
    }

    @Test
    void should_return201AndPersistSubtypeFields_when_createResultModus() {
        // Arrange
        String request = """
                {"modus": "RESULT", "name": "Ergebnis", "communityId": %d, "tendencyPoints": 1, "bonusPoints": 2}
                """.formatted(community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.modus").isEqualTo("RESULT")
                .jsonPath("$.type").isEqualTo("ResultTipp")
                .jsonPath("$.tendencyPoints").isEqualTo(1)
                .jsonPath("$.bonusPoints").isEqualTo(2);
        assertThat(tippModusRepository.findAll())
                .singleElement()
                .isInstanceOf(TippModusResult.class);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"modus": "POINT", "name": " ", "deadline": -1, "totalPoints": -1}
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/tipp-modi");
    }

    @Test
    void should_return400_when_createWithoutModus() {
        // Arrange
        String request = """
                {"name": "Punkte", "communityId": %d}
                """.formatted(community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest();
    }

    @Test
    void should_return422AndPersistNothing_when_createWithTypeNotMatchingModus() {
        // Arrange
        TippModusDto request = TippModusTotoDto.builder()
                .name("Toto")
                .type("PointTipp")
                .communityId(community.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Type PointTipp does not match the modus of the request, expected TotoTipp");
        assertThat(tippModusRepository.count()).isZero();
    }

    @Test
    void should_return404AndPersistNothing_when_createForUnknownCommunity() {
        // Arrange
        TippModusDto request = totoRequest("Toto", 999_999L);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Community with id 999999 not found");
        assertThat(tippModusRepository.count()).isZero();
    }

    @Test
    void should_return409AndPersistNothing_when_createWithNameOfSameModusInCommunity() {
        // Arrange
        saveToto("Toto", community);
        TippModusDto request = totoRequest("Toto", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("TotoTipp with name Toto already exists in Community " + community.getId());
        assertThat(tippModusRepository.count()).isEqualTo(1);
    }

    @Test
    void should_return201_when_createWithNameUsedByOtherModusOrCommunity() {
        // Arrange
        Community other = communityRepository.save(Community.builder().name("Kollegen").build());
        saveToto("Standard", other);
        tippModusRepository.save(TippModusPoint.builder()
                .name("Standard")
                .community(community)
                .build());
        TippModusDto request = totoRequest("Standard", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    @Test
    void should_returnModiOfCommunity_when_getWithCommunityFilter() {
        // Arrange
        Community other = communityRepository.save(Community.builder().name("Kollegen").build());
        saveToto("Toto", community);
        tippModusRepository.save(TippModusPoint.builder()
                .name("Punkte")
                .community(community)
                .build());
        saveToto("Fremd", other);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("communityId", community.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].modus").value(modi -> assertThat(modi)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("TOTO", "POINT"));
    }

    @Test
    void should_returnAllModi_when_getWithoutFilter() {
        // Arrange
        Community other = communityRepository.save(Community.builder().name("Kollegen").build());
        saveToto("Toto", community);
        saveToto("Toto", other);

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
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
                .jsonPath("$.message").isEqualTo("TippModus with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        TippModus tippModus = tippModusRepository.save(TippModusResult.builder()
                .name("Ergebnis")
                .community(community)
                .tendencyPoints(1)
                .bonusPoints(1)
                .build());
        TippModusDto request = TippModusResultDto.builder()
                .name("Ergebnis plus")
                .deadline(15)
                .communityId(community.getId())
                .tendencyPoints(2)
                .bonusPoints(3)
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tippModus.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(tippModus.getId())
                .jsonPath("$.name").isEqualTo("Ergebnis plus");
        assertThat(tippModusRepository.findById(tippModus.getId()))
                .get()
                .asInstanceOf(InstanceOfAssertFactories.type(TippModusResult.class))
                .extracting(TippModus::getName, TippModus::getDeadline, TippModus::getType,
                        TippModusResult::getTendencyPoints, TippModusResult::getBonusPoints)
                .containsExactly("Ergebnis plus", 15, TippModusType.TIPPMODUS_RESULT, 2, 3);
    }

    @Test
    void should_return422AndKeepModus_when_updateToOtherModus() {
        // Arrange
        TippModus tippModus = saveToto("Toto", community);
        TippModusDto request = TippModusPointDto.builder()
                .name("Toto")
                .communityId(community.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tippModus.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("TippModus " + tippModus.getId()
                        + " is a TotoTipp and cannot be changed to a PointTipp");
        assertThat(tippModusRepository.findById(tippModus.getId()))
                .get()
                .isInstanceOf(TippModusToto.class);
    }

    @Test
    void should_return422AndKeepCommunity_when_updateToOtherCommunity() {
        // Arrange
        Community other = communityRepository.save(Community.builder().name("Kollegen").build());
        TippModus tippModus = saveToto("Toto", community);
        TippModusDto request = totoRequest("Toto", other.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tippModus.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("TippModus " + tippModus.getId() + " cannot be moved to another community");
        assertThat(tippModusRepository.findByCommunityId(other.getId())).isEmpty();
    }

    @Test
    void should_return200_when_updateKeepsOwnName() {
        // Arrange
        TippModus tippModus = saveToto("Toto", community);
        TippModusDto request = totoRequest("Toto", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tippModus.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    void should_return409AndKeepName_when_updateToNameOfOtherModusOfSameSubtype() {
        // Arrange
        saveToto("Toto", community);
        TippModus tippModus = saveToto("Toto 2", community);
        TippModusDto request = totoRequest("Toto", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tippModus.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(tippModusRepository.findById(tippModus.getId()))
                .get()
                .extracting(TippModus::getName)
                .isEqualTo("Toto 2");
    }

    @Test
    void should_return404_when_updateUnknownModus() {
        // Arrange
        TippModusDto request = totoRequest("Toto", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndCascadeToTippsAndTippConfigs_when_deleteModus() {
        // Arrange
        TippModus tippModus = saveToto("Toto", community);
        Tipper tipper = tipperRepository.save(Tipper.builder()
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .passwort("geheim")
                .build());
        CommunityMembership membership = communityMembershipRepository.save(CommunityMembership.builder()
                .community(community)
                .tipper(tipper)
                .build());
        Spiel spiel = spielRepository.save(Spiel.builder().spielNumber(1).build());
        Tipp tipp = tippRepository.save(Tipp.builder()
                .communityMembership(membership)
                .spiel(spiel)
                .tippModus(tippModus)
                .remisTipp(1)
                .build());
        Competition competition = saveCompetition();
        CompetitionMembership competitionMembership = competitionMembershipRepository.save(CompetitionMembership.builder()
                .community(community)
                .competition(competition)
                .build());
        TippConfig tippConfig = tippConfigRepository.save(TippConfig.builder()
                .competitionMembership(competitionMembership)
                .spieltag(saveSpieltag(competition))
                .tippModus(tippModus)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", tippModus.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(tippModusRepository.existsById(tippModus.getId())).isFalse();
        assertThat(tippRepository.existsById(tipp.getId())).isFalse();
        assertThat(tippConfigRepository.existsById(tippConfig.getId())).isFalse();
        assertThat(communityMembershipRepository.existsById(membership.getId())).isTrue();
        assertThat(competitionMembershipRepository.existsById(competitionMembership.getId())).isTrue();
        assertThat(spielRepository.existsById(spiel.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownModus() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private TippModus saveToto(String name, Community owner) {
        return tippModusRepository.save(TippModusToto.builder()
                .name(name)
                .community(owner)
                .build());
    }

    private Competition saveCompetition() {
        CompetitionFamily family = competitionFamilyRepository.save(CompetitionFamily.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build());
        return competitionRepository.save(Competition.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(family)
                .build());
    }

    private Spieltag saveSpieltag(Competition competition) {
        CompetitionRound round = competitionRoundRepository.save(CompetitionRound.builder()
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .competition(competition)
                .build());
        return spieltagRepository.save(Spieltag.builder()
                .spieltagNumber(1)
                .competitionRound(round)
                .build());
    }

    private static TippModusDto totoRequest(String name, Long communityId) {
        return TippModusTotoDto.builder()
                .name(name)
                .communityId(communityId)
                .build();
    }
}
