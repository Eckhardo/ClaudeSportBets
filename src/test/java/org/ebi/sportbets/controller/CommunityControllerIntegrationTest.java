package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.CommunityRole;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippConfig;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.RoleRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
import org.ebi.sportbets.repository.TippConfigRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.ebi.sportbets.repository.TippRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.ebi.sportbets.repository.TipperRoleRepository;
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
class CommunityControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private TipperRepository tipperRepository;

    @Autowired
    private CommunityMembershipRepository communityMembershipRepository;

    @Autowired
    private CompetitionMembershipRepository competitionMembershipRepository;

    @Autowired
    private TippModusRepository tippModusRepository;

    @Autowired
    private TippRepository tippRepository;

    @Autowired
    private TippConfigRepository tippConfigRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TipperRoleRepository tipperRoleRepository;

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

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/communities")
                .build();
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
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        CommunityDto request = communityRequest("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        CommunityDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/communities/\\d+")
                .expectBody(CommunityDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Stammtisch");
        assertThat(created.getDescription()).isEqualTo("Tipprunde Stammtisch");
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(communityRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "description": "%s"}
                """.formatted("x".repeat(256));

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.length()").isEqualTo(2)
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/communities");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithExistingName() {
        // Arrange
        saveCommunity("Stammtisch");
        CommunityDto request = communityRequest("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Community with name Stammtisch already exists");
        assertThat(communityRepository.count()).isEqualTo(1);
    }

    @Test
    void should_returnAllCommunities_when_getAll() {
        // Arrange
        saveCommunity("Stammtisch");
        saveCommunity("Kollegen");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].name").value(names -> assertThat(names)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("Stammtisch", "Kollegen"));
    }

    @Test
    void should_returnCommunity_when_getByExistingId() {
        // Arrange
        Community community = saveCommunity("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", community.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(community.getId())
                .jsonPath("$.name").isEqualTo("Stammtisch");
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
                .jsonPath("$.message").isEqualTo("Community with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Community community = saveCommunity("Stammtisch");
        CommunityDto request = CommunityDto.builder()
                .name("Stammtisch Nord")
                .description("Neue Beschreibung")
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", community.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(community.getId())
                .jsonPath("$.name").isEqualTo("Stammtisch Nord");
        assertThat(communityRepository.findById(community.getId()))
                .get()
                .extracting(Community::getName, Community::getDescription)
                .containsExactly("Stammtisch Nord", "Neue Beschreibung");
    }

    @Test
    void should_return200_when_updateKeepsOwnName() {
        // Arrange
        Community community = saveCommunity("Stammtisch");
        CommunityDto request = communityRequest("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", community.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    void should_return409AndKeepName_when_updateToNameOfOtherCommunity() {
        // Arrange
        saveCommunity("Kollegen");
        Community community = saveCommunity("Stammtisch");
        CommunityDto request = communityRequest("Kollegen");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", community.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(communityRepository.findById(community.getId()))
                .get()
                .extracting(Community::getName)
                .isEqualTo("Stammtisch");
    }

    @Test
    void should_return404_when_updateUnknownCommunity() {
        // Arrange
        CommunityDto request = communityRequest("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndRemove_when_deleteCommunityWithoutChildren() {
        // Arrange
        Community community = saveCommunity("Stammtisch");

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", community.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(communityRepository.existsById(community.getId())).isFalse();
    }

    @Test
    void should_return204AndCascadeToAllChildren_when_deleteCommunityWithChildren() {
        // Arrange
        Community community = saveCommunity("Stammtisch");
        Tipper tipper = saveTipper("erika", community.getId());
        CommunityMembership membership = communityMembershipRepository.save(CommunityMembership.builder()
                .community(community)
                .tipper(tipper)
                .build());
        TippModus tippModus = tippModusRepository.save(TippModusToto.builder()
                .name("Toto")
                .community(community)
                .build());
        Spiel spiel = spielRepository.save(Spiel.builder().spielNumber(1).build());
        Tipp tipp = tippRepository.save(Tipp.builder()
                .communityMembership(membership)
                .spiel(spiel)
                .tippModus(tippModus)
                .remisTipp(1)
                .build());
        CompetitionMembership competitionMembership = competitionMembershipRepository.save(CompetitionMembership.builder()
                .community(community)
                .competition(saveCompetition())
                .build());
        TippConfig tippConfig = tippConfigRepository.save(TippConfig.builder()
                .competitionMembership(competitionMembership)
                .spieltag(saveSpieltag(competitionMembership.getCompetition()))
                .tippModus(tippModus)
                .build());
        CommunityRole role = roleRepository.save(CommunityRole.builder()
                .name("Admin")
                .community(community)
                .build());
        TipperRole tipperRole = tipperRoleRepository.save(TipperRole.builder()
                .tipper(tipper)
                .role(role)
                .build());

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", community.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent();
        assertThat(communityRepository.existsById(community.getId())).isFalse();
        assertThat(communityMembershipRepository.existsById(membership.getId())).isFalse();
        assertThat(competitionMembershipRepository.existsById(competitionMembership.getId())).isFalse();
        assertThat(tippModusRepository.existsById(tippModus.getId())).isFalse();
        assertThat(tippRepository.existsById(tipp.getId())).isFalse();
        assertThat(tippConfigRepository.existsById(tippConfig.getId())).isFalse();
        assertThat(roleRepository.existsById(role.getId())).isFalse();
        assertThat(tipperRoleRepository.existsById(tipperRole.getId())).isFalse();
        assertThat(spielRepository.existsById(spiel.getId())).isTrue();
        assertThat(tipperRepository.findById(tipper.getId()))
                .get()
                .extracting(Tipper::getDefaultCommunityId)
                .isNull();
    }

    @Test
    void should_return404_when_deleteUnknownCommunity() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private Community saveCommunity(String name) {
        return communityRepository.save(Community.builder()
                .name(name)
                .build());
    }

    private Tipper saveTipper(String username, Long defaultCommunityId) {
        return tipperRepository.save(Tipper.builder()
                .username(username)
                .firstname(username)
                .lastname("Mustermann")
                .passwort("geheim")
                .defaultCommunityId(defaultCommunityId)
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

    private static CommunityDto communityRequest(String name) {
        return CommunityDto.builder()
                .name(name)
                .description("Tipprunde " + name)
                .build();
    }
}
