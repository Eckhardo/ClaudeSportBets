package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CommunityMembershipDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CommunityMembershipService;
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

@WebMvcTest(CommunityMembershipController.class)
class CommunityMembershipControllerTest {

    private static final String VALID_REQUEST = """
            {"communityId": 2, "tipperId": 3}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CommunityMembershipService communityMembershipService;

    private static CommunityMembershipDto membership() {
        return CommunityMembershipDto.builder().id(7L).communityId(2L).tipperId(3L).build();
    }

    @Test
    void should_returnAllMembershipDtos_when_getMembershipsWithoutFilter() {
        // Arrange
        given(communityMembershipService.find(null, null)).willReturn(List.of(membership()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/community-memberships").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 7, "communityId": 2, "tipperId": 3}]
                        """);
    }

    @Test
    void should_passFiltersToService_when_getMembershipsWithCommunityIdAndTipperId() {
        // Arrange
        given(communityMembershipService.find(2L, 3L)).willReturn(List.of(membership()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/community-memberships?communityId=2&tipperId=3").exchange();

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
        given(communityMembershipService.findById(42L))
                .willThrow(new ResourceNotFoundException("CommunityMembership", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/community-memberships/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(communityMembershipService.create(any(CommunityMembershipDto.class))).willReturn(membership());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/community-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/community-memberships/7")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "communityId": 2, "tipperId": 3}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = "{}";

        // Act
        MvcTestResult result = mvc.post().uri("/v1/community-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("communityId", "tipperId");
    }

    @Test
    void should_return409_when_createForExistingMember() {
        // Arrange
        given(communityMembershipService.create(any(CommunityMembershipDto.class)))
                .willThrow(new DuplicateResourceException("Tipper 3 is already a member of Community 2"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/community-memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(communityMembershipService.update(eq(7L), any(CommunityMembershipDto.class))).willReturn(membership());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/community-memberships/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "communityId": 2, "tipperId": 3}
                        """);
    }

    @Test
    void should_return422_when_updateMovesMembershipWithTipps() {
        // Arrange
        given(communityMembershipService.update(eq(7L), any(CommunityMembershipDto.class)))
                .willThrow(new BusinessRuleException("CommunityMembership 7 has Tipps and cannot be moved to another Community"));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/community-memberships/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void should_return404_when_updateUnknownMembership() {
        // Arrange
        given(communityMembershipService.update(eq(42L), any(CommunityMembershipDto.class)))
                .willThrow(new ResourceNotFoundException("CommunityMembership", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/community-memberships/{id}", 42L)
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
        MvcTestResult result = mvc.delete().uri("/v1/community-memberships/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownMembership() {
        // Arrange
        willThrow(new ResourceNotFoundException("CommunityMembership", 42L))
                .given(communityMembershipService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/community-memberships/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
