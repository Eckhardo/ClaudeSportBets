package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CompetitionMembershipDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CompetitionMembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(CompetitionMembershipController.class)
class CompetitionMembershipControllerTest {

    private static final String VALID_REQUEST = """
            {"communityId": 2, "competitionId": 4}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CompetitionMembershipService competitionMembershipService;

    private static CompetitionMembershipDto membership() {
        return CompetitionMembershipDto.builder().id(7L).communityId(2L).competitionId(4L).build();
    }

    @Test
    void should_returnAllMembershipDtos_when_getMembershipsWithoutFilter() {
        // Arrange
        given(competitionMembershipService.find(null, null)).willReturn(List.of(membership()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-memberships").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 7, "communityId": 2, "competitionId": 4}]
                        """);
    }

    @Test
    void should_passFiltersToService_when_getMembershipsWithCommunityIdAndCompetitionId() {
        // Arrange
        given(competitionMembershipService.find(2L, 4L)).willReturn(List.of(membership()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-memberships?communityId=2&competitionId=4").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 7}]
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(competitionMembershipService.findById(42L))
                .willThrow(new ResourceNotFoundException("CompetitionMembership", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/competition-memberships/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(competitionMembershipService.create(any(CompetitionMembershipDto.class))).willReturn(membership());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/competition-memberships/7")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "communityId": 2, "competitionId": 4}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = "{}";

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("communityId", "competitionId");
    }

    @Test
    void should_return409_when_createForCommunityAlreadyTakingPart() {
        // Arrange
        given(competitionMembershipService.create(any(CompetitionMembershipDto.class)))
                .willThrow(new DuplicateResourceException("Community 2 already takes part in Competition 4"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/competition-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(competitionMembershipService.update(eq(7L), any(CompetitionMembershipDto.class)))
                .willReturn(membership());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-memberships/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "communityId": 2, "competitionId": 4}
                        """);
    }

    @Test
    void should_return422_when_updateMovesMembershipWithTippConfigs() {
        // Arrange
        given(competitionMembershipService.update(eq(7L), any(CompetitionMembershipDto.class)))
                .willThrow(new BusinessRuleException(
                        "CompetitionMembership 7 has TippConfigs and cannot be moved to another Community or Competition"));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-memberships/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void should_return404_when_updateUnknownMembership() {
        // Arrange
        given(competitionMembershipService.update(eq(42L), any(CompetitionMembershipDto.class)))
                .willThrow(new ResourceNotFoundException("CompetitionMembership", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/competition-memberships/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingMembership() {
        // Arrange
        Long id = 7L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-memberships/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownMembership() {
        // Arrange
        willThrow(new ResourceNotFoundException("CompetitionMembership", 42L))
                .given(competitionMembershipService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/competition-memberships/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
