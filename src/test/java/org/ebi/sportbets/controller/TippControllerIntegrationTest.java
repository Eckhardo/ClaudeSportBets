package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.TippDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusPoint;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.SpielRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class TippControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TippRepository tippRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private TipperRepository tipperRepository;

    @Autowired
    private CommunityMembershipRepository communityMembershipRepository;

    @Autowired
    private TippModusRepository tippModusRepository;

    @Autowired
    private SpielRepository spielRepository;

    private WebTestClient client;
    private Community community;
    private CommunityMembership membership;
    private TippModus tippModus;
    private Spiel spiel1;
    private Spiel spiel2;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + contextPath + "/v1/tipps")
                .build();
        community = communityRepository.save(Community.builder().name("Stammtisch").build());
        membership = saveMembership(community, "erika");
        tippModus = saveTippModus(community, "Punkte");
        spiel1 = saveSpiel(1);
        spiel2 = saveSpiel(2);
    }

    // The server runs in its own thread, so there is no test transaction to roll back.
    // Deleting tippers cascades to their memberships and Tipps; the rest has no owner to cascade from.
    @AfterEach
    void tearDown() {
        tipperRepository.deleteAll();
        tippModusRepository.deleteAll();
        communityRepository.deleteAll();
        spielRepository.deleteAll();
    }

    @Test
    void should_return201WithLocationAndPersist_when_createWithValidRequest() {
        // Arrange
        TippDto request = tippRequest(spiel1, 2, 1);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        TippDto created = response
                .expectStatus().isCreated()
                .expectHeader().valueMatches(HttpHeaders.LOCATION, ".*" + contextPath + "/v1/tipps/\\d+")
                .expectBody(TippDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created)
                .extracting(TippDto::getCommunityMembershipId, TippDto::getSpielId, TippDto::getTippModusId,
                        TippDto::getHeimTipp, TippDto::getGastTipp)
                .containsExactly(membership.getId(), spiel1.getId(), tippModus.getId(), 2, 1);
        assertThat(created.getCreatedOn()).isNotNull();
        assertThat(tippRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"heimTipp": -1}
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
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/tipps");
    }

    @Test
    void should_return404AndPersistNothing_when_createForUnknownSpiel() {
        // Arrange
        long unknownId = spiel2.getId() + 1000;
        TippDto request = TippDto.builder()
                .communityMembershipId(membership.getId())
                .spielId(unknownId)
                .tippModusId(tippModus.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Spiel with id " + unknownId + " not found");
        assertThat(tippRepository.count()).isZero();
    }

    @Test
    void should_return422AndPersistNothing_when_createWithModusOfOtherCommunity() {
        // Arrange
        Community other = communityRepository.save(Community.builder().name("Kollegen").build());
        TippModus foreignModus = saveTippModus(other, "Punkte");
        TippDto request = TippDto.builder()
                .communityMembershipId(membership.getId())
                .spielId(spiel1.getId())
                .tippModusId(foreignModus.getId())
                .build();

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("TippModus " + foreignModus.getId()
                        + " does not belong to the community of CommunityMembership " + membership.getId());
        assertThat(tippRepository.count()).isZero();
    }

    @Test
    void should_return409AndKeepFirstTipp_when_createSecondTippForSameSpielAndModus() {
        // Arrange
        Tipp existing = saveTipp(membership, spiel1, 1, 0);
        TippDto request = tippRequest(spiel1, 3, 3);

        // Act
        WebTestClient.ResponseSpec response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(409);
        assertThat(tippRepository.findAll())
                .extracting(Tipp::getId, Tipp::getHeimTipp)
                .containsExactly(tuple(existing.getId(), 1));
    }

    @Test
    void should_returnTippsOfMembership_when_getWithCommunityMembershipId() {
        // Arrange
        CommunityMembership otherMember = saveMembership(community, "max");
        saveTipp(membership, spiel1, 1, 0);
        saveTipp(membership, spiel2, 2, 2);
        saveTipp(otherMember, spiel1, 0, 3);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("communityMembershipId", membership.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].spielId").value(spielIds -> assertThat(spielIds)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder(spiel1.getId().intValue(), spiel2.getId().intValue()));
    }

    @Test
    void should_returnTippsOfSpiel_when_getWithSpielId() {
        // Arrange
        CommunityMembership otherMember = saveMembership(community, "max");
        saveTipp(membership, spiel1, 1, 0);
        saveTipp(membership, spiel2, 2, 2);
        saveTipp(otherMember, spiel1, 0, 3);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("spielId", spiel1.getId()).build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[*].communityMembershipId").value(memberIds -> assertThat(memberIds)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder(membership.getId().intValue(), otherMember.getId().intValue()));
    }

    @Test
    void should_returnSingleTipp_when_getWithCommunityMembershipIdAndSpielId() {
        // Arrange
        CommunityMembership otherMember = saveMembership(community, "max");
        Tipp tipp = saveTipp(membership, spiel1, 1, 0);
        saveTipp(membership, spiel2, 2, 2);
        saveTipp(otherMember, spiel1, 0, 3);

        // Act
        WebTestClient.ResponseSpec response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("communityMembershipId", membership.getId())
                        .queryParam("spielId", spiel1.getId())
                        .build())
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(tipp.getId());
    }

    @Test
    void should_returnTipp_when_getByExistingId() {
        // Arrange
        Tipp tipp = saveTipp(membership, spiel1, 2, 1);

        // Act
        WebTestClient.ResponseSpec response = client.get().uri("/{id}", tipp.getId()).exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(tipp.getId())
                .jsonPath("$.communityMembershipId").isEqualTo(membership.getId())
                .jsonPath("$.tippModusId").isEqualTo(tippModus.getId())
                .jsonPath("$.heimTipp").isEqualTo(2);
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
                .jsonPath("$.message").isEqualTo("Tipp with id 999999 not found");
    }

    @Test
    void should_return200AndPersistChanges_when_updateWithValidRequest() {
        // Arrange
        Tipp tipp = saveTipp(membership, spiel1, 0, 0);
        TippDto request = tippRequest(spiel1, 4, 2);

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", tipp.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(tipp.getId())
                .jsonPath("$.heimTipp").isEqualTo(4);
        assertThat(tippRepository.findById(tipp.getId()))
                .get()
                .extracting(Tipp::getHeimTipp, Tipp::getGastTipp)
                .containsExactly(4, 2);
    }

    @Test
    void should_return404_when_updateUnknownTipp() {
        // Arrange
        TippDto request = tippRequest(spiel1, 1, 1);

        // Act
        WebTestClient.ResponseSpec response = client.put().uri("/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    @Test
    void should_return200AndCreateOrUpdateEachTipp_when_saveAllWithMixedList() {
        // Arrange
        Tipp tippedSpiel1 = saveTipp(membership, spiel1, 0, 0);
        List<TippDto> request = List.of(
                // No id, but spiel1 is already tipped: updates the existing Tipp
                tippRequest(spiel1, 3, 1),
                // No id and not tipped yet: creates a new Tipp
                tippRequest(spiel2, 1, 1));

        // Act
        WebTestClient.ResponseSpec response = client.put()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(tippedSpiel1.getId())
                .jsonPath("$[0].heimTipp").isEqualTo(3)
                .jsonPath("$[1].spielId").isEqualTo(spiel2.getId());
        assertThat(tippRepository.findAll())
                .extracting(tipp -> tipp.getSpiel().getId(), Tipp::getHeimTipp, Tipp::getGastTipp)
                .containsExactlyInAnyOrder(
                        tuple(spiel1.getId(), 3, 1),
                        tuple(spiel2.getId(), 1, 1));
    }

    @Test
    void should_return200AndUpdateById_when_saveAllWithId() {
        // Arrange
        Tipp tipp = saveTipp(membership, spiel1, 0, 0);
        TippDto request = tippRequest(spiel1, 2, 2);
        request.setId(tipp.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(request))
                .exchange();

        // Assert
        response.expectStatus().isOk();
        assertThat(tippRepository.findById(tipp.getId()))
                .get()
                .extracting(Tipp::getHeimTipp, Tipp::getGastTipp)
                .containsExactly(2, 2);
    }

    @Test
    void should_return404AndRollBackWholeList_when_saveAllWithOneUnknownSpiel() {
        // Arrange
        Tipp tipp = saveTipp(membership, spiel1, 0, 0);
        long unknownId = spiel2.getId() + 1000;
        TippDto unknownSpiel = TippDto.builder()
                .communityMembershipId(membership.getId())
                .spielId(unknownId)
                .tippModusId(tippModus.getId())
                .build();
        List<TippDto> request = List.of(tippRequest(spiel1, 5, 5), tippRequest(spiel2, 1, 0), unknownSpiel);

        // Act
        WebTestClient.ResponseSpec response = client.put()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Spiel with id " + unknownId + " not found");
        assertThat(tippRepository.findAll())
                .extracting(Tipp::getId, Tipp::getHeimTipp)
                .containsExactly(tuple(tipp.getId(), 0));
    }

    @Test
    void should_return400WithIndexedFieldErrors_when_saveAllWithInvalidElement() {
        // Arrange
        String invalidRequest = """
                [{"communityMembershipId": %d, "spielId": %d, "tippModusId": %d},
                 {"communityMembershipId": %d, "tippModusId": %d, "gastTipp": -1}]
                """.formatted(membership.getId(), spiel1.getId(), tippModus.getId(), membership.getId(), tippModus.getId());

        // Act
        WebTestClient.ResponseSpec response = client.put()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.path").isEqualTo(contextPath + "/v1/tipps")
                .jsonPath("$.fieldErrors[*].field").value(fields -> assertThat(fields)
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder("[1].spielId", "[1].gastTipp"));
        assertThat(tippRepository.count()).isZero();
    }

    @Test
    void should_return422AndPersistNothing_when_saveAllWithSameSpielTwice() {
        // Arrange
        List<TippDto> request = List.of(tippRequest(spiel1, 1, 0), tippRequest(spiel1, 2, 0));

        // Act
        WebTestClient.ResponseSpec response = client.put()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isEqualTo(422);
        assertThat(tippRepository.count()).isZero();
    }

    @Test
    void should_return204AndKeepSpielAndMembership_when_deleteTipp() {
        // Arrange
        Tipp tipp = saveTipp(membership, spiel1, 2, 1);

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", tipp.getId()).exchange();

        // Assert
        response.expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(tippRepository.existsById(tipp.getId())).isFalse();
        assertThat(spielRepository.existsById(spiel1.getId())).isTrue();
        assertThat(communityMembershipRepository.existsById(membership.getId())).isTrue();
    }

    @Test
    void should_return404_when_deleteUnknownTipp() {
        // Arrange
        long unknownId = 999_999L;

        // Act
        WebTestClient.ResponseSpec response = client.delete().uri("/{id}", unknownId).exchange();

        // Assert
        response.expectStatus().isNotFound();
    }

    private CommunityMembership saveMembership(Community owner, String username) {
        Tipper tipper = tipperRepository.save(Tipper.builder()
                .username(username)
                .firstname(username)
                .lastname("Mustermann")
                .passwort("geheim")
                .build());
        return communityMembershipRepository.save(CommunityMembership.builder()
                .community(owner)
                .tipper(tipper)
                .build());
    }

    private TippModus saveTippModus(Community owner, String name) {
        return tippModusRepository.save(TippModusPoint.builder()
                .name(name)
                .community(owner)
                .build());
    }

    private Spiel saveSpiel(int spielNumber) {
        return spielRepository.save(Spiel.builder().spielNumber(spielNumber).build());
    }

    private Tipp saveTipp(CommunityMembership owner, Spiel spiel, int heimTipp, int gastTipp) {
        return tippRepository.save(Tipp.builder()
                .communityMembership(owner)
                .spiel(spiel)
                .tippModus(tippModus)
                .heimTipp(heimTipp)
                .gastTipp(gastTipp)
                .build());
    }

    private TippDto tippRequest(Spiel spiel, int heimTipp, int gastTipp) {
        return TippDto.builder()
                .communityMembershipId(membership.getId())
                .spielId(spiel.getId())
                .tippModusId(tippModus.getId())
                .heimTipp(heimTipp)
                .gastTipp(gastTipp)
                .build();
    }
}
