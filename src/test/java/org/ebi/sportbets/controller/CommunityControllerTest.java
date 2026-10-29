package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.CommunityService;
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

@WebMvcTest(CommunityController.class)
class CommunityControllerTest {

    private static final String VALID_REQUEST = """
            {"name": "Stammtisch", "description": "Tipprunde Stammtisch"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CommunityService communityService;

    private static CommunityDto stammtisch() {
        return CommunityDto.builder().id(7L).name("Stammtisch").description("Tipprunde Stammtisch").build();
    }

    @Test
    void should_returnCommunityDtos_when_getCommunities() {
        // Arrange
        given(communityService.findAll()).willReturn(List.of(stammtisch()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/communities").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 7, "name": "Stammtisch"}]
                        """);
    }

    @Test
    void should_returnCommunityDto_when_getByExistingId() {
        // Arrange
        given(communityService.findById(7L)).willReturn(stammtisch());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/communities/{id}", 7L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "description": "Tipprunde Stammtisch"}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(communityService.findById(42L)).willThrow(new ResourceNotFoundException("Community", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/communities/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(communityService.create(any(CommunityDto.class))).willReturn(stammtisch());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/communities/7")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "name": "Stammtisch"}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"name": " ", "description": "%s"}
                """.formatted("x".repeat(256));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "description");
    }

    @Test
    void should_return409_when_createWithExistingName() {
        // Arrange
        given(communityService.create(any(CommunityDto.class)))
                .willThrow(new DuplicateResourceException("Community with name Stammtisch already exists"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Community with name Stammtisch already exists");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(communityService.update(eq(7L), any(CommunityDto.class))).willReturn(stammtisch());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/communities/{id}", 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 7, "name": "Stammtisch"}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownCommunity() {
        // Arrange
        given(communityService.update(eq(42L), any(CommunityDto.class)))
                .willThrow(new ResourceNotFoundException("Community", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/communities/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingCommunity() {
        // Arrange
        Long id = 7L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/communities/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownCommunity() {
        // Arrange
        willThrow(new ResourceNotFoundException("Community", 42L)).given(communityService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/communities/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
