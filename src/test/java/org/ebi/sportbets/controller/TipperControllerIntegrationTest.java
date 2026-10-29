package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.CommunityRole;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.RoleRepository;
import org.ebi.sportbets.repository.SpielRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class TipperControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TipperRepository tipperRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository communityMembershipRepository;

    @Autowired
    private TippModusRepository tippModusRepository;

    @Autowired
    private TippRepository tippRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TipperRoleRepository tipperRoleRepository;

    @Autowired
    private SpielRepository spielRepository;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/tippers")
                .build();
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Tippers and communities cascade to everything below them; Spiele are a separate root.
    @AfterEach
    void tearDown() {
        tipperRepository.deleteAll();
        communityRepository.deleteAll();
        spielRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        Map<String, Object> request = tipperRequest("erika");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        TipperDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/tippers/\\d+")
                .expectBody(TipperDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created)
                .extracting(TipperDto::getUsername, TipperDto::getFirstname, TipperDto::getLastname,
                        TipperDto::getEmail, TipperDto::getPasswort)
                .containsExactly("erika", "Erika", "Mustermann", "erika@example.org", null);
        assertThat(tipperRepository.findById(created.getId()))
                .get()
                .extracting(Tipper::getPasswort)
                .isEqualTo("geheim");
    }

    @Test
    void should_notExposePasswort_when_getById() {
        // Arrange
        Tipper tipper = saveTipper("erika");

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", tipper.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("erika")
                .jsonPath("$.passwort").doesNotExist();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"username": " ", "email": "no-email"}
                """;

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.length()").isEqualTo(7)
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/tippers");
    }

    @Test
    void should_return409AndPersistNothing_when_createWithExistingUsername() {
        // Arrange
        saveTipper("erika");
        Map<String, Object> request = tipperRequest("erika");

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Tipper with username erika already exists");
        assertThat(tipperRepository.count()).isEqualTo(1);
    }

    @Test
    void should_return404AndPersistNothing_when_createWithUnknownDefaultCommunity() {
        // Arrange
        Map<String, Object> request = tipperRequest("erika");
        request.put("defaultCommunityId", 999_999L);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Community with id 999999 not found");
        assertThat(tipperRepository.count()).isZero();
    }

    @Test
    void should_return404AndPersistNothing_when_createWithUnknownDefaultCompetition() {
        // Arrange
        Map<String, Object> request = tipperRequest("erika");
        request.put("defaultCompetitionId", 999_999L);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Competition with id 999999 not found");
        assertThat(tipperRepository.count()).isZero();
    }

    @Test
    void should_returnAllTippers_when_getAll() {
        // Arrange
        saveTipper("erika");
        saveTipper("max");

        // Act
        WebTestClient.ResponseSpec response = client.get().exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].username").value(usernames -> assertThat(usernames)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("erika", "max"));
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
                .jsonPath("$.message").isEqualTo("Tipper with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Tipper tipper = saveTipper("erika");
        Community community = communityRepository.save(Community.builder().name("Stammtisch").build());
        Map<String, Object> request = tipperRequest("erika.m");
        request.put("passwort", "neu");
        request.put("communityAdmin", true);
        request.put("defaultCommunityId", community.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tipper.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(tipper.getId())
                .jsonPath("$.username").isEqualTo("erika.m");
        assertThat(tipperRepository.findById(tipper.getId()))
                .get()
                .extracting(Tipper::getUsername, Tipper::getPasswort, Tipper::isCommunityAdmin,
                        Tipper::getDefaultCommunityId)
                .containsExactly("erika.m", "neu", true, community.getId());
    }

    @Test
    void should_return200_when_updateKeepsOwnUsername() {
        // Arrange
        Tipper tipper = saveTipper("erika");
        Map<String, Object> request = tipperRequest("erika");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tipper.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk();
    }

    @Test
    void should_return409AndKeepUsername_when_updateToUsernameOfOtherTipper() {
        // Arrange
        saveTipper("max");
        Tipper tipper = saveTipper("erika");
        Map<String, Object> request = tipperRequest("max");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tipper.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(tipperRepository.findById(tipper.getId()))
                .get()
                .extracting(Tipper::getUsername)
                .isEqualTo("erika");
    }

    @Test
    void should_return404_when_updateUnknownTipper() {
        // Arrange
        Map<String, Object> request = tipperRequest("erika");

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return204AndCascadeToMembershipsTippsAndRoles_when_deleteTipper() {
        // Arrange
        Tipper tipper = saveTipper("erika");
        Community community = communityRepository.save(Community.builder().name("Stammtisch").build());
        CommunityMembership membership = communityMembershipRepository.save(CommunityMembership.builder()
                .community(community)
                .tipper(tipper)
                .build());
        TippModus tippModus = tippModusRepository.save(TippModusToto.builder()
                .name("Toto")
                .community(community)
                .build());
        Tipp tipp = tippRepository.save(Tipp.builder()
                .communityMembership(membership)
                .spiel(spielRepository.save(Spiel.builder().spielNumber(1).build()))
                .tippModus(tippModus)
                .remisTipp(1)
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
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", tipper.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(tipperRepository.existsById(tipper.getId())).isFalse();
        assertThat(communityMembershipRepository.existsById(membership.getId())).isFalse();
        assertThat(tippRepository.existsById(tipp.getId())).isFalse();
        assertThat(tipperRoleRepository.existsById(tipperRole.getId())).isFalse();
        assertThat(roleRepository.existsById(role.getId())).isTrue();
        assertThat(communityRepository.existsById(community.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownTipper() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private Tipper saveTipper(String username) {
        return tipperRepository.save(Tipper.builder()
                .username(username)
                .firstname("Erika")
                .lastname("Mustermann")
                .passwort("geheim")
                .build());
    }

    // A map instead of TipperDto, because the write-only passwort would not be serialized from the DTO
    private static Map<String, Object> tipperRequest(String username) {
        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("firstname", "Erika");
        request.put("lastname", "Mustermann");
        request.put("email", username + "@example.org");
        request.put("passwort", "geheim");
        request.put("communityAdmin", false);
        request.put("competitionAdmin", false);
        return request;
    }
}
