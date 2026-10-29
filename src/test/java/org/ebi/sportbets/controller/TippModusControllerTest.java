package org.ebi.sportbets.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.ebi.sportbets.dto.TippModusDto;
import org.ebi.sportbets.dto.TippModusPointDto;
import org.ebi.sportbets.dto.TippModusTotoDto;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.service.TippModusService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(TippModusController.class)
class TippModusControllerTest {

    private static final String VALID_REQUEST = """
            {"modus": "POINT", "name": "Punkte", "deadline": 30, "communityId": 7, "totalPoints": 10}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TippModusService tippModusService;

    private static TippModusDto punkte() {
        return TippModusPointDto.builder()
                .id(3L)
                .name("Punkte")
                .type("PointTipp")
                .deadline(30)
                .communityId(7L)
                .totalPoints(10)
                .build();
    }

    @Test
    void should_returnModiWithModusDiscriminator_when_getWithCommunityId() {
        // Arrange
        given(tippModusService.find(7L)).willReturn(List.of(
                punkte(),
                TippModusTotoDto.builder().id(4L).name("Toto").type("TotoTipp").communityId(7L).build()));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipp-modi").param("communityId", "7").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": 3, "modus": "POINT", "totalPoints": 10}, {"id": 4, "modus": "TOTO"}]
                        """);
    }

    @Test
    void should_passNullFilterToService_when_getWithoutCommunityId() {
        // Arrange
        given(tippModusService.find(null)).willReturn(List.of());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipp-modi").exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isEqualTo("[]");
    }

    @Test
    void should_returnModusDto_when_getByExistingId() {
        // Arrange
        given(tippModusService.findById(3L)).willReturn(punkte());

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipp-modi/{id}", 3L).exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 3, "modus": "POINT", "type": "PointTipp", "communityId": 7}
                        """);
    }

    @Test
    void should_return404_when_getByUnknownId() {
        // Arrange
        given(tippModusService.findById(42L)).willThrow(new ResourceNotFoundException("TippModus", 42L));

        // Act
        MvcTestResult result = mvc.get().uri("/v1/tipp-modi/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return201WithLocationAndBody_when_createWithValidRequest() {
        // Arrange
        given(tippModusService.create(any(TippModusDto.class))).willReturn(punkte());

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipp-modi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/v1/tipp-modi/3")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 3, "modus": "POINT", "name": "Punkte"}
                        """);
    }

    @Test
    void should_deserializeSubtypeFromModus_when_createWithValidRequest() {
        // Arrange
        given(tippModusService.create(any(TippModusDto.class))).willReturn(punkte());
        ArgumentCaptor<TippModusDto> request = ArgumentCaptor.forClass(TippModusDto.class);

        // Act
        mvc.post().uri("/v1/tipp-modi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        then(tippModusService).should().create(request.capture());
        assertThat(request.getValue())
                .asInstanceOf(InstanceOfAssertFactories.type(TippModusPointDto.class))
                .extracting(TippModusPointDto::getName, TippModusPointDto::getTotalPoints)
                .containsExactly("Punkte", 10);
    }

    @Test
    void should_return400WithFieldErrors_when_createWithInvalidRequest() {
        // Arrange
        String invalidRequest = """
                {"modus": "POINT", "name": " ", "deadline": -1, "totalPoints": -1}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipp-modi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fieldErrors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "deadline", "communityId", "totalPoints");
    }

    @Test
    void should_return400_when_createWithUnknownModus() {
        // Arrange
        String invalidRequest = """
                {"modus": "LOTTO", "name": "Lotto", "communityId": 7}
                """;

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipp-modi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return409_when_createWithNameTakenInCommunity() {
        // Arrange
        given(tippModusService.create(any(TippModusDto.class)))
                .willThrow(new DuplicateResourceException("PointTipp with name Punkte already exists in Community 7"));

        // Act
        MvcTestResult result = mvc.post().uri("/v1/tipp-modi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("PointTipp with name Punkte already exists in Community 7");
    }

    @Test
    void should_return200WithUpdatedBody_when_updateWithValidRequest() {
        // Arrange
        given(tippModusService.update(eq(3L), any(TippModusDto.class))).willReturn(punkte());

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipp-modi/{id}", 3L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 3, "name": "Punkte"}
                        """);
    }

    @Test
    void should_return422_when_updateToOtherSubtype() {
        // Arrange
        given(tippModusService.update(eq(3L), any(TippModusDto.class)))
                .willThrow(new BusinessRuleException("TippModus 3 is a TotoTipp and cannot be changed to a PointTipp"));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipp-modi/{id}", 3L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("TippModus 3 is a TotoTipp and cannot be changed to a PointTipp");
    }

    @Test
    void should_return404_when_updateUnknownModus() {
        // Arrange
        given(tippModusService.update(eq(42L), any(TippModusDto.class)))
                .willThrow(new ResourceNotFoundException("TippModus", 42L));

        // Act
        MvcTestResult result = mvc.put().uri("/v1/tipp-modi/{id}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST)
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_return204_when_deleteExistingModus() {
        // Arrange
        Long id = 3L;

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tipp-modi/{id}", id).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void should_return404_when_deleteUnknownModus() {
        // Arrange
        willThrow(new ResourceNotFoundException("TippModus", 42L)).given(tippModusService).delete(42L);

        // Act
        MvcTestResult result = mvc.delete().uri("/v1/tipp-modi/{id}", 42L).exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
