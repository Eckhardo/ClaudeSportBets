package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.TippModusDto;
import org.ebi.sportbets.dto.TippModusPointDto;
import org.ebi.sportbets.dto.TippModusResultDto;
import org.ebi.sportbets.dto.TippModusTotoDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusPoint;
import org.ebi.sportbets.entity.TippModusResult;
import org.ebi.sportbets.entity.TippModusToto;
import org.ebi.sportbets.enums.TippModusType;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TippModusServiceTest {

    private static final Community COMMUNITY = Community.builder().id(7L).build();

    @Mock
    private TippModusRepository tippModusRepository;

    @Mock
    private CommunityRepository communityRepository;

    @InjectMocks
    private TippModusService tippModusService;

    private static TippModusPointDto pointRequest() {
        return TippModusPointDto.builder()
                .id(99L)
                .name("Punkte")
                .deadline(30)
                .communityId(7L)
                .totalPoints(10)
                .build();
    }

    private static TippModusToto toto(Long id, String name) {
        return TippModusToto.builder().id(id).name(name).community(COMMUNITY).build();
    }

    @Test
    void should_queryByCommunity_when_findWithCommunityId() {
        // Arrange
        given(tippModusRepository.findByCommunityId(7L)).willReturn(List.of(toto(3L, "Toto")));

        // Act
        List<TippModusDto> result = tippModusService.find(7L);

        // Assert
        assertThat(result)
                .singleElement()
                .isInstanceOf(TippModusTotoDto.class)
                .extracting(TippModusDto::getId, TippModusDto::getType)
                .containsExactly(3L, "TotoTipp");
        then(tippModusRepository).should(never()).findAll();
    }

    @Test
    void should_returnAllModi_when_findWithoutCommunityId() {
        // Arrange
        given(tippModusRepository.findAll()).willReturn(List.of(toto(3L, "Toto"), toto(4L, "Toto 2")));

        // Act
        List<TippModusDto> result = tippModusService.find(null);

        // Assert
        assertThat(result).extracting(TippModusDto::getId).containsExactly(3L, 4L);
    }

    @Test
    void should_mapAllSubtypeFieldsToDto_when_resultModusExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(tippModusRepository.findById(5L)).willReturn(Optional.of(TippModusResult.builder()
                .id(5L)
                .name("Ergebnis")
                .deadline(15)
                .community(COMMUNITY)
                .tendencyPoints(1)
                .bonusPoints(2)
                .createdOn(createdOn)
                .build()));

        // Act
        TippModusDto result = tippModusService.findById(5L);

        // Assert
        assertThat(result).isEqualTo(TippModusResultDto.builder()
                .id(5L)
                .name("Ergebnis")
                .type("ResultTipp")
                .deadline(15)
                .communityId(7L)
                .tendencyPoints(1)
                .bonusPoints(2)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_deriveTypeFromSubtype_when_storedTypeColumnIsEmpty() {
        // Arrange
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(toto(3L, "Toto")));

        // Act
        TippModusDto result = tippModusService.findById(3L);

        // Assert
        assertThat(result.getType()).isEqualTo("TotoTipp");
    }

    @Test
    void should_throwResourceNotFound_when_modusWithIdDoesNotExist() {
        // Arrange
        given(tippModusRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("TippModus with id 42 not found");
    }

    @Test
    void should_saveAndFlushPointEntityWithType_when_createPointModus() {
        // Arrange
        given(communityRepository.findById(7L)).willReturn(Optional.of(COMMUNITY));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte")).willReturn(List.of());
        given(tippModusRepository.saveAndFlush(any(TippModus.class))).willAnswer(invocation -> {
            TippModus toSave = invocation.getArgument(0);
            toSave.setId(3L);
            return toSave;
        });

        // Act
        TippModusDto result = tippModusService.create(pointRequest());

        // Assert
        assertThat(result).isEqualTo(TippModusPointDto.builder()
                .id(3L)
                .name("Punkte")
                .type("PointTipp")
                .deadline(30)
                .communityId(7L)
                .totalPoints(10)
                .build());
    }

    @Test
    void should_throwBusinessRuleAndSaveNothing_when_typeDoesNotMatchModus() {
        // Arrange
        TippModusPointDto request = pointRequest();
        request.setType("TotoTipp");

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Type TotoTipp does not match the modus of the request, expected PointTipp");
        then(tippModusRepository).should(never()).saveAndFlush(any(TippModus.class));
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_communityDoesNotExist() {
        // Arrange
        given(communityRepository.findById(7L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.create(pointRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Community with id 7 not found");
        then(tippModusRepository).should(never()).saveAndFlush(any(TippModus.class));
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_nameIsTakenBySameSubtypeInCommunity() {
        // Arrange
        given(communityRepository.findById(7L)).willReturn(Optional.of(COMMUNITY));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte"))
                .willReturn(List.of(TippModusPoint.builder().id(3L).name("Punkte").community(COMMUNITY).build()));

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.create(pointRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("PointTipp with name Punkte already exists in Community 7");
        then(tippModusRepository).should(never()).saveAndFlush(any(TippModus.class));
    }

    @Test
    void should_create_when_nameIsOnlyTakenByOtherSubtype() {
        // Arrange
        given(communityRepository.findById(7L)).willReturn(Optional.of(COMMUNITY));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte")).willReturn(List.of(toto(3L, "Punkte")));
        given(tippModusRepository.saveAndFlush(any(TippModus.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        TippModusDto result = tippModusService.create(pointRequest());

        // Assert
        assertThat(result).isInstanceOf(TippModusPointDto.class);
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_subtypeAndCommunityAreUnchanged() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        TippModusPoint existing = TippModusPoint.builder()
                .id(3L)
                .name("Alt")
                .community(COMMUNITY)
                .totalPoints(5)
                .createdOn(createdOn)
                .build();
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte")).willReturn(List.of());

        // Act
        TippModusDto result = tippModusService.update(3L, pointRequest());

        // Assert
        assertThat(result).isEqualTo(TippModusPointDto.builder()
                .id(3L)
                .name("Punkte")
                .type("PointTipp")
                .deadline(30)
                .communityId(7L)
                .totalPoints(10)
                .createdOn(createdOn)
                .build());
        assertThat(existing.getType()).isEqualTo(TippModusType.TIPPMODUS_POINT);
    }

    @Test
    void should_update_when_nameIsTakenOnlyByItself() {
        // Arrange
        TippModusPoint existing = TippModusPoint.builder().id(3L).name("Punkte").community(COMMUNITY).build();
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte")).willReturn(List.of(existing));

        // Act
        TippModusDto result = tippModusService.update(3L, pointRequest());

        // Assert
        assertThat(((TippModusPointDto) result).getTotalPoints()).isEqualTo(10);
    }

    @Test
    void should_throwBusinessRuleAndKeepFields_when_updatingToOtherSubtype() {
        // Arrange
        TippModusToto existing = toto(3L, "Toto");
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.update(3L, pointRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("TippModus 3 is a TotoTipp and cannot be changed to a PointTipp");
        assertThat(existing.getName()).isEqualTo("Toto");
    }

    @Test
    void should_throwBusinessRuleAndKeepFields_when_updatingToOtherCommunity() {
        // Arrange
        TippModusPoint existing = TippModusPoint.builder().id(3L).name("Alt").community(COMMUNITY).build();
        TippModusPointDto request = pointRequest();
        request.setCommunityId(8L);
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.update(3L, request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("TippModus 3 cannot be moved to another community");
        assertThat(existing.getName()).isEqualTo("Alt");
    }

    @Test
    void should_throwDuplicateAndKeepName_when_updatingToNameOfOtherModusOfSameSubtype() {
        // Arrange
        TippModusPoint existing = TippModusPoint.builder().id(3L).name("Alt").community(COMMUNITY).build();
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tippModusRepository.findByCommunityIdAndName(7L, "Punkte"))
                .willReturn(List.of(TippModusPoint.builder().id(4L).name("Punkte").community(COMMUNITY).build()));

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.update(3L, pointRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(existing.getName()).isEqualTo("Alt");
    }

    @Test
    void should_deleteModus_when_modusExists() {
        // Arrange
        TippModusToto existing = toto(3L, "Toto");
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(existing));

        // Act
        tippModusService.delete(3L);

        // Assert
        then(tippModusRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownModus() {
        // Arrange
        given(tippModusRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippModusService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("TippModus with id 42 not found");
        then(tippModusRepository).should(never()).delete(any(TippModus.class));
    }
}
