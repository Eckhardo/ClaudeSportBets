package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TipperServiceTest {

    @Mock
    private TipperRepository tipperRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private TipperService tipperService;

    private static TipperDto validRequest() {
        return TipperDto.builder()
                .id(99L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .email("erika@example.org")
                .passwort("geheim")
                .passwortHint("Hinweis")
                .communityAdmin(true)
                .competitionAdmin(false)
                .build();
    }

    private static Tipper erika() {
        return Tipper.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .email("erika@example.org")
                .passwort("geheim")
                .passwortHint("Hinweis")
                .communityAdmin(true)
                .defaultCommunityId(7L)
                .defaultCompetitionId(8L)
                .build();
    }

    @Test
    void should_mapAllFieldsExceptPasswortToDto_when_tippersExist() {
        // Arrange
        given(tipperRepository.findAll()).willReturn(List.of(erika()));

        // Act
        List<TipperDto> result = tipperService.findAll();

        // Assert
        assertThat(result).containsExactly(TipperDto.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .email("erika@example.org")
                .passwortHint("Hinweis")
                .communityAdmin(true)
                .competitionAdmin(false)
                .defaultCommunityId(7L)
                .defaultCompetitionId(8L)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_tipperWithIdDoesNotExist() {
        // Arrange
        given(tipperRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipper with id 42 not found");
    }

    @Test
    void should_saveTipperWithPasswortIgnoringRequestId_when_usernameIsUnique() {
        // Arrange
        given(tipperRepository.existsByUsername("erika")).willReturn(false);
        given(tipperRepository.save(any(Tipper.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Tipper> saved = ArgumentCaptor.forClass(Tipper.class);

        // Act
        tipperService.create(validRequest());

        // Assert
        then(tipperRepository).should().save(saved.capture());
        assertThat(saved.getValue())
                .extracting(Tipper::getId, Tipper::getUsername, Tipper::getPasswort, Tipper::isCommunityAdmin)
                .containsExactly(null, "erika", "geheim", true);
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithExistingUsername() {
        // Arrange
        given(tipperRepository.existsByUsername("erika")).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Tipper with username erika already exists");
        then(tipperRepository).should(never()).save(any(Tipper.class));
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_defaultCommunityDoesNotExist() {
        // Arrange
        TipperDto request = validRequest();
        request.setDefaultCommunityId(42L);
        given(tipperRepository.existsByUsername("erika")).willReturn(false);
        given(communityRepository.existsById(42L)).willReturn(false);

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Community with id 42 not found");
        then(tipperRepository).should(never()).save(any(Tipper.class));
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_defaultCompetitionDoesNotExist() {
        // Arrange
        TipperDto request = validRequest();
        request.setDefaultCompetitionId(42L);
        given(tipperRepository.existsByUsername("erika")).willReturn(false);
        given(competitionRepository.existsById(42L)).willReturn(false);

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competition with id 42 not found");
        then(tipperRepository).should(never()).save(any(Tipper.class));
    }

    @Test
    void should_updateFieldsAndKeepId_when_tipperExists() {
        // Arrange
        Tipper existing = erika();
        TipperDto request = validRequest();
        request.setUsername("erika.m");
        request.setPasswort("neu");
        request.setDefaultCommunityId(7L);
        given(tipperRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tipperRepository.existsByUsernameAndIdNot("erika.m", 3L)).willReturn(false);
        given(communityRepository.existsById(7L)).willReturn(true);

        // Act
        TipperDto result = tipperService.update(3L, request);

        // Assert
        assertThat(result)
                .extracting(TipperDto::getId, TipperDto::getUsername, TipperDto::getDefaultCommunityId,
                        TipperDto::getDefaultCompetitionId, TipperDto::getPasswort)
                .containsExactly(3L, "erika.m", 7L, null, null);
        assertThat(existing.getPasswort()).isEqualTo("neu");
    }

    @Test
    void should_throwDuplicateAndKeepUsername_when_updatingToUsernameOfOtherTipper() {
        // Arrange
        Tipper existing = erika();
        TipperDto request = validRequest();
        request.setUsername("max");
        given(tipperRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tipperRepository.existsByUsernameAndIdNot("max", 3L)).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.update(3L, request));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(existing.getUsername()).isEqualTo("erika");
    }

    @Test
    void should_throwResourceNotFoundAndKeepFields_when_updatingWithUnknownDefaultCommunity() {
        // Arrange
        Tipper existing = erika();
        TipperDto request = validRequest();
        request.setFirstname("Neu");
        request.setDefaultCommunityId(42L);
        given(tipperRepository.findById(3L)).willReturn(Optional.of(existing));
        given(tipperRepository.existsByUsernameAndIdNot("erika", 3L)).willReturn(false);
        given(communityRepository.existsById(42L)).willReturn(false);

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.update(3L, request));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        assertThat(existing)
                .extracting(Tipper::getFirstname, Tipper::getDefaultCommunityId)
                .containsExactly("Erika", 7L);
    }

    @Test
    void should_deleteTipper_when_tipperExists() {
        // Arrange
        Tipper existing = erika();
        given(tipperRepository.findById(3L)).willReturn(Optional.of(existing));

        // Act
        tipperService.delete(3L);

        // Assert
        then(tipperRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownTipper() {
        // Arrange
        given(tipperRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipper with id 42 not found");
        then(tipperRepository).should(never()).delete(any(Tipper.class));
    }
}
