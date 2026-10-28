package org.ebi.sportbets.controller;

import org.ebi.sportbets.dto.TippDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.TippService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(TippController.class)
class TippControllerTest {

    private static final String VALID_REQUEST = """
            {"communityMembershipId": 2, "spielId": 11, "tippModusId": 3, "heimTipp": 2, "gastTipp": 1}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TippService tippService;

    private static TippDto tipp() {
        return TippDto.builder()
                .id(20L)
                .communityMembershipId(2L)
                .spielId(11L)
                .tippModusId(3L)
                .heimTipp(2)
                .gastTipp(1)
                .build();
    }

    @Test
    void should_returnAllTippDtos_when_getTippsWithoutFilter() {
        // Arrange
        given(tippService.find(null, null)).willReturn(List.of(tipp()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipps").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 20, "communityMembershipId": 2, "spielId": 11, "tippModusId": 3}]
                        """);
    }

    @Test
    void should_passBothFiltersToService_when_getTippsWithMembershipAndSpiel() {
        // Arrange
        given(tippService.find(2L, 11L)).willReturn(List.of(tipp()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipps?communityMembershipId={m}&spielId={s}", 2L, 11L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 20}]
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(tippService.findById(42L)).willThrow(new ResourceNotFoundException("Tipp", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipps/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(tippService.create(any(TippDto.class))).willReturn(tipp());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/tipps/20")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 20, "heimTipp": 2, "gastTipp": 1}
                        """);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"heimTipp": -1}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("communityMembershipId", "spielId", "tippModusId", "heimTipp");
    }

    @Test
    void should_return409_when_createSecondTippForSameSpielAndModus() {
        // Arrange
        given(tippService.create(any(TippDto.class))).willThrow(new DuplicateResourceException(
                "Tipp of CommunityMembership 2 for Spiel 11 and TippModus 3 already exists"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void should_return422_when_createWithModusOfOtherCommunity() {
        // Arrange
        given(tippService.create(any(TippDto.class))).willThrow(new BusinessRuleException(
                "TippModus 3 does not belong to the community of CommunityMembership 2"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("TippModus 3 does not belong to the community of CommunityMembership 2");
    }

    @Test
    void should_return200WithSavedList_when_saveAllWithValidRequest() {
        // Arrange
        given(tippService.saveAll(anyList())).willReturn(List.of(tipp()));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + VALID_REQUEST + "]")
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 20, "heimTipp": 2, "gastTipp": 1}]
                        """);
    }

    @Test
    void should_return400WithIndexedFieldErrors_when_saveAllWithInvalidElement() {
        // Arrange
        String invalidRequest = "[" + VALID_REQUEST + ", {\"communityMembershipId\": 2, \"tippModusId\": 3, \"heimTipp\": -1}]";

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("[1].spielId", "[1].heimTipp");
    }

    @Test
    void should_return400_when_saveAllWithEmptyList() {
        // Arrange
        String emptyRequest = "[]";

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactly("tippDtos");
    }

    @Test
    void should_return422_when_saveAllWithSameTippTwice() {
        // Arrange
        given(tippService.saveAll(anyList())).willThrow(new BusinessRuleException(
                "Request contains more than one Tipp of CommunityMembership 2 for Spiel 11 and TippModus 3"));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + VALID_REQUEST + ", " + VALID_REQUEST + "]")
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(tippService.update(eq(20L), any(TippDto.class))).willReturn(tipp());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps/{id}", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 20, "heimTipp": 2}
                        """);
    }

    @Test
    void should_return404_when_updateUnknownTipp() {
        // Arrange
        given(tippService.update(eq(42L), any(TippDto.class))).willThrow(new ResourceNotFoundException("Tipp", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipps/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingTipp() {
        // Arrange
        Long id = 20L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tipps/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownTipp() {
        // Arrange
        willThrow(new ResourceNotFoundException("Tipp", 42L)).given(tippService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tipps/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
