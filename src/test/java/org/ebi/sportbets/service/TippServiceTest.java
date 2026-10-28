package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.TippDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.entity.TippModusPoint;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.ebi.sportbets.repository.TippRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TippServiceTest {

    private static final Community COMMUNITY = Community.builder().id(1L).build();

    @Mock
    private TippRepository tippRepository;

    @Mock
    private CommunityMembershipRepository communityMembershipRepository;

    @Mock
    private SpielRepository spielRepository;

    @Mock
    private TippModusRepository tippModusRepository;

    @InjectMocks
    private TippService tippService;

    private final CommunityMembership membership = CommunityMembership.builder().id(2L).community(COMMUNITY).build();
    private final Spiel spiel = Spiel.builder().id(11L).build();
    private final TippModus tippModus = TippModusPoint.builder().id(3L).community(COMMUNITY).build();

    private static TippDto validRequest() {
        return TippDto.builder()
                .id(99L)
                .communityMembershipId(2L)
                .spielId(11L)
                .tippModusId(3L)
                .heimTipp(2)
                .gastTipp(1)
                .build();
    }

    private static TippDto.TippDtoBuilder requestBuilder() {
        return TippDto.builder()
                .communityMembershipId(2L)
                .spielId(11L)
                .tippModusId(3L)
                .heimTipp(2)
                .gastTipp(1);
    }

    private void givenReferencesExist() {
        given(communityMembershipRepository.findById(2L)).willReturn(Optional.of(membership));
        given(spielRepository.findById(11L)).willReturn(Optional.of(spiel));
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(tippModus));
    }

    @Test
    void should_mapAllFieldsToDto_when_findWithoutFilters() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(tippRepository.findAll()).willReturn(List.of(Tipp.builder()
                .id(20L)
                .communityMembership(membership)
                .spiel(spiel)
                .tippModus(tippModus)
                .heimTipp(2)
                .gastTipp(1)
                .remisTipp(0)
                .winPoints(3)
                .createdOn(createdOn)
                .build()));

        // Act
        List<TippDto> result = tippService.find(null, null);

        // Assert
        assertThat(result).containsExactly(TippDto.builder()
                .id(20L)
                .communityMembershipId(2L)
                .spielId(11L)
                .tippModusId(3L)
                .heimTipp(2)
                .gastTipp(1)
                .remisTipp(0)
                .winPoints(3)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_queryByMembership_when_findWithMembershipOnly() {
        // Arrange
        given(tippRepository.findByCommunityMembershipId(2L)).willReturn(List.of(tipp(20L)));

        // Act
        List<TippDto> result = tippService.find(2L, null);

        // Assert
        assertThat(result).extracting(TippDto::getId).containsExactly(20L);
    }

    @Test
    void should_queryBySpiel_when_findWithSpielOnly() {
        // Arrange
        given(tippRepository.findBySpielId(11L)).willReturn(List.of(tipp(20L)));

        // Act
        List<TippDto> result = tippService.find(null, 11L);

        // Assert
        assertThat(result).extracting(TippDto::getId).containsExactly(20L);
    }

    @Test
    void should_queryByMembershipAndSpiel_when_findWithBothFilters() {
        // Arrange
        given(tippRepository.findByCommunityMembershipIdAndSpielId(2L, 11L)).willReturn(List.of(tipp(20L)));

        // Act
        List<TippDto> result = tippService.find(2L, 11L);

        // Assert
        assertThat(result).extracting(TippDto::getId).containsExactly(20L);
    }

    @Test
    void should_throwResourceNotFound_when_tippWithIdDoesNotExist() {
        // Arrange
        given(tippRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipp with id 42 not found");
    }

    @Test
    void should_saveAndReturnTipp_when_referencesExistAndNoTippYet() {
        // Arrange
        givenReferencesExist();
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L)).willReturn(Optional.empty());
        given(tippRepository.save(any(Tipp.class))).willAnswer(invocation -> {
            Tipp toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });

        // Act
        TippDto result = tippService.create(validRequest());

        // Assert
        assertThat(result)
                .extracting(TippDto::getId, TippDto::getCommunityMembershipId, TippDto::getSpielId,
                        TippDto::getTippModusId, TippDto::getHeimTipp, TippDto::getGastTipp)
                .containsExactly(20L, 2L, 11L, 3L, 2, 1);
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_creatingForUnknownSpiel() {
        // Arrange
        given(communityMembershipRepository.findById(2L)).willReturn(Optional.of(membership));
        given(spielRepository.findById(11L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spiel with id 11 not found");
        then(tippRepository).should(never()).save(any(Tipp.class));
    }

    @Test
    void should_throwBusinessRuleAndSaveNothing_when_tippModusBelongsToOtherCommunity() {
        // Arrange
        given(communityMembershipRepository.findById(2L)).willReturn(Optional.of(membership));
        given(spielRepository.findById(11L)).willReturn(Optional.of(spiel));
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(TippModusPoint.builder()
                .id(3L)
                .community(Community.builder().id(9L).build())
                .build()));

        // Act
        Throwable thrown = catchThrowable(() -> tippService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("TippModus 3 does not belong to the community of CommunityMembership 2");
        then(tippRepository).should(never()).save(any(Tipp.class));
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_memberAlreadyTippedSpielWithModus() {
        // Arrange
        givenReferencesExist();
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L))
                .willReturn(Optional.of(tipp(20L)));

        // Act
        Throwable thrown = catchThrowable(() -> tippService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Tipp of CommunityMembership 2 for Spiel 11 and TippModus 3 already exists");
        then(tippRepository).should(never()).save(any(Tipp.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_tippExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Tipp existing = Tipp.builder()
                .id(20L)
                .communityMembership(membership)
                .spiel(spiel)
                .tippModus(tippModus)
                .heimTipp(0)
                .gastTipp(0)
                .createdOn(createdOn)
                .build();
        given(tippRepository.findById(20L)).willReturn(Optional.of(existing));
        givenReferencesExist();
        // The Tipp itself is the existing one, so it must not count as a duplicate
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L))
                .willReturn(Optional.of(existing));

        // Act
        TippDto result = tippService.update(20L, validRequest());

        // Assert
        assertThat(result).isEqualTo(TippDto.builder()
                .id(20L)
                .communityMembershipId(2L)
                .spielId(11L)
                .tippModusId(3L)
                .heimTipp(2)
                .gastTipp(1)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwBusinessRuleAndKeepTipp_when_updatingWithModusOfOtherCommunity() {
        // Arrange
        Tipp existing = Tipp.builder().id(20L).heimTipp(0).build();
        given(tippRepository.findById(20L)).willReturn(Optional.of(existing));
        given(communityMembershipRepository.findById(2L)).willReturn(Optional.of(membership));
        given(spielRepository.findById(11L)).willReturn(Optional.of(spiel));
        given(tippModusRepository.findById(3L)).willReturn(Optional.of(TippModusPoint.builder()
                .id(3L)
                .community(Community.builder().id(9L).build())
                .build()));

        // Act
        Throwable thrown = catchThrowable(() -> tippService.update(20L, validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(existing.getHeimTipp()).isZero();
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownTipp() {
        // Arrange
        given(tippRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippService.update(42L, validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipp with id 42 not found");
    }

    @Test
    void should_deleteTipp_when_tippExists() {
        // Arrange
        Tipp existing = tipp(20L);
        given(tippRepository.findById(20L)).willReturn(Optional.of(existing));

        // Act
        tippService.delete(20L);

        // Assert
        then(tippRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownTipp() {
        // Arrange
        given(tippRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tippService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipp with id 42 not found");
        then(tippRepository).should(never()).delete(any(Tipp.class));
    }

    @Test
    void should_createTipp_when_saveAllWithNewTipp() {
        // Arrange
        givenReferencesExist();
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L)).willReturn(Optional.empty());
        given(tippRepository.save(any(Tipp.class))).willAnswer(invocation -> {
            Tipp toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });
        TippDto request = requestBuilder().id(null).build();

        // Act
        List<TippDto> result = tippService.saveAll(List.of(request));

        // Assert
        assertThat(result)
                .extracting(TippDto::getId, TippDto::getHeimTipp, TippDto::getGastTipp)
                .containsExactly(tuple(20L, 2, 1));
    }

    @Test
    void should_updateExistingTippWithoutSaving_when_saveAllWithoutIdForAlreadyTippedSpiel() {
        // Arrange
        Tipp existing = tipp(20L);
        existing.setHeimTipp(0);
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L))
                .willReturn(Optional.of(existing));
        given(tippRepository.findById(20L)).willReturn(Optional.of(existing));
        givenReferencesExist();
        TippDto request = requestBuilder().id(null).build();

        // Act
        List<TippDto> result = tippService.saveAll(List.of(request));

        // Assert
        assertThat(result)
                .extracting(TippDto::getId, TippDto::getHeimTipp)
                .containsExactly(tuple(20L, 2));
        then(tippRepository).should(never()).save(any(Tipp.class));
    }

    @Test
    void should_updateTippById_when_saveAllWithId() {
        // Arrange
        Tipp existing = tipp(20L);
        given(tippRepository.findById(20L)).willReturn(Optional.of(existing));
        givenReferencesExist();
        given(tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(2L, 11L, 3L))
                .willReturn(Optional.of(existing));
        TippDto request = requestBuilder().id(20L).gastTipp(4).build();

        // Act
        List<TippDto> result = tippService.saveAll(List.of(request));

        // Assert
        assertThat(result)
                .extracting(TippDto::getId, TippDto::getGastTipp)
                .containsExactly(tuple(20L, 4));
    }

    @Test
    void should_throwResourceNotFound_when_saveAllWithUnknownId() {
        // Arrange
        given(tippRepository.findById(42L)).willReturn(Optional.empty());
        TippDto request = requestBuilder().id(42L).build();

        // Act
        Throwable thrown = catchThrowable(() -> tippService.saveAll(List.of(request)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipp with id 42 not found");
    }

    @Test
    void should_throwBusinessRuleAndTouchNothing_when_saveAllWithSameKeyTwice() {
        // Arrange
        TippDto first = requestBuilder().id(null).build();
        TippDto second = requestBuilder().id(null).heimTipp(0).build();

        // Act
        Throwable thrown = catchThrowable(() -> tippService.saveAll(List.of(first, second)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Request contains more than one Tipp of CommunityMembership 2 for Spiel 11 and TippModus 3");
        then(tippRepository).shouldHaveNoInteractions();
    }

    private Tipp tipp(Long id) {
        return Tipp.builder()
                .id(id)
                .communityMembership(membership)
                .spiel(spiel)
                .tippModus(tippModus)
                .build();
    }
}
