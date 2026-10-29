package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private TipperRepository tipperRepository;

    @InjectMocks
    private CommunityService communityService;

    private static CommunityDto validRequest() {
        return CommunityDto.builder()
                .id(99L)
                .name("Stammtisch")
                .description("Tipprunde Stammtisch")
                .build();
    }

    @Test
    void should_mapAllFieldsToDto_when_communitiesExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(communityRepository.findAll()).willReturn(List.of(Community.builder()
                .id(7L)
                .name("Stammtisch")
                .description("Tipprunde Stammtisch")
                .createdOn(createdOn)
                .build()));

        // Act
        List<CommunityDto> result = communityService.findAll();

        // Assert
        assertThat(result).containsExactly(CommunityDto.builder()
                .id(7L)
                .name("Stammtisch")
                .description("Tipprunde Stammtisch")
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_communityWithIdDoesNotExist() {
        // Arrange
        given(communityRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Community with id 42 not found");
    }

    @Test
    void should_saveAndReturnCommunityIgnoringRequestId_when_nameIsUnique() {
        // Arrange
        given(communityRepository.existsByName("Stammtisch")).willReturn(false);
        given(communityRepository.save(any(Community.class))).willAnswer(invocation -> {
            Community toSave = invocation.getArgument(0);
            toSave.setId(7L);
            return toSave;
        });

        // Act
        CommunityDto result = communityService.create(validRequest());

        // Assert
        assertThat(result)
                .extracting(CommunityDto::getId, CommunityDto::getName, CommunityDto::getDescription)
                .containsExactly(7L, "Stammtisch", "Tipprunde Stammtisch");
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithExistingName() {
        // Arrange
        given(communityRepository.existsByName("Stammtisch")).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> communityService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Community with name Stammtisch already exists");
        then(communityRepository).should(never()).save(any(Community.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_communityExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Community existing = Community.builder().id(7L).name("Alt").createdOn(createdOn).build();
        given(communityRepository.findById(7L)).willReturn(Optional.of(existing));
        given(communityRepository.existsByNameAndIdNot("Stammtisch", 7L)).willReturn(false);

        // Act
        CommunityDto result = communityService.update(7L, validRequest());

        // Assert
        assertThat(result).isEqualTo(CommunityDto.builder()
                .id(7L)
                .name("Stammtisch")
                .description("Tipprunde Stammtisch")
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwDuplicateAndKeepName_when_updatingToNameOfOtherCommunity() {
        // Arrange
        Community existing = Community.builder().id(7L).name("Alt").build();
        given(communityRepository.findById(7L)).willReturn(Optional.of(existing));
        given(communityRepository.existsByNameAndIdNot("Stammtisch", 7L)).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> communityService.update(7L, validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(existing.getName()).isEqualTo("Alt");
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownCommunity() {
        // Arrange
        given(communityRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityService.update(42L, validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Community with id 42 not found");
    }

    @Test
    void should_clearTipperDefaultsAndDelete_when_communityExists() {
        // Arrange
        Community existing = Community.builder().id(7L).build();
        given(communityRepository.findById(7L)).willReturn(Optional.of(existing));

        // Act
        communityService.delete(7L);

        // Assert
        InOrder inOrder = inOrder(tipperRepository, communityRepository);
        inOrder.verify(tipperRepository).clearDefaultCommunity(7L);
        inOrder.verify(communityRepository).delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndChangeNothing_when_deletingUnknownCommunity() {
        // Arrange
        given(communityRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityService.delete(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        then(tipperRepository).should(never()).clearDefaultCommunity(anyLong());
        then(communityRepository).should(never()).delete(any(Community.class));
    }
}
