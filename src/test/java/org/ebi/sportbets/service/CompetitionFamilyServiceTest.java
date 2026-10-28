package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CompetitionFamilyDto;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
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
class CompetitionFamilyServiceTest {

    @Mock
    private CompetitionFamilyRepository competitionFamilyRepository;

    @InjectMocks
    private CompetitionFamilyService competitionFamilyService;

    @Test
    void should_mapAllFieldsToDto_when_familiesExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CompetitionFamily family = CompetitionFamily.builder()
                .id(7L)
                .name("Bundesliga")
                .description("1. Liga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .createdOn(createdOn)
                .build();
        given(competitionFamilyRepository.findAll()).willReturn(List.of(family));

        // Act
        List<CompetitionFamilyDto> result = competitionFamilyService.findAll();

        // Assert
        assertThat(result).containsExactly(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .description("1. Liga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_returnDto_when_familyWithIdExists() {
        // Arrange
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.of(CompetitionFamily.builder()
                .id(7L)
                .name("Bundesliga")
                .hasClubs(true)
                .build()));

        // Act
        CompetitionFamilyDto result = competitionFamilyService.findById(7L);

        // Assert
        assertThat(result).isEqualTo(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(false)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_familyWithIdDoesNotExist() {
        // Arrange
        given(competitionFamilyRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionFamilyService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionFamily with id 42 not found");
    }

    @Test
    void should_saveAndReturnFamily_when_nameIsUnique() {
        // Arrange
        given(competitionFamilyRepository.existsByName("Bundesliga")).willReturn(false);
        given(competitionFamilyRepository.save(any(CompetitionFamily.class))).willAnswer(invocation -> {
            CompetitionFamily toSave = invocation.getArgument(0);
            toSave.setId(7L);
            return toSave;
        });
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .id(99L)
                .name("Bundesliga")
                .country("Deutschland")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        CompetitionFamilyDto result = competitionFamilyService.create(request);

        // Assert
        assertThat(result)
                .extracting(CompetitionFamilyDto::getId, CompetitionFamilyDto::getName, CompetitionFamilyDto::getCountry)
                .containsExactly(7L, "Bundesliga", "Deutschland");
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithExistingName() {
        // Arrange
        given(competitionFamilyRepository.existsByName("Bundesliga")).willReturn(true);
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionFamilyService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CompetitionFamily with name Bundesliga already exists");
        then(competitionFamilyRepository).should(never()).save(any(CompetitionFamily.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_familyExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CompetitionFamily existing = CompetitionFamily.builder()
                .id(7L)
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .createdOn(createdOn)
                .build();
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.of(existing));
        given(competitionFamilyRepository.existsByNameAndIdNot("Premier League", 7L)).willReturn(false);
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .id(99L)
                .name("Premier League")
                .country("England")
                .hasClubs(true)
                .hasLigaModus(false)
                .createdOn(LocalDateTime.of(2000, 1, 1, 0, 0))
                .build();

        // Act
        CompetitionFamilyDto result = competitionFamilyService.update(7L, request);

        // Assert
        assertThat(result).isEqualTo(CompetitionFamilyDto.builder()
                .id(7L)
                .name("Premier League")
                .country("England")
                .hasClubs(true)
                .hasLigaModus(false)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownFamily() {
        // Arrange
        given(competitionFamilyRepository.findById(42L)).willReturn(Optional.empty());
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Bundesliga")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionFamilyService.update(42L, request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionFamily with id 42 not found");
    }

    @Test
    void should_throwDuplicateAndKeepName_when_updatingToNameOfOtherFamily() {
        // Arrange
        CompetitionFamily existing = CompetitionFamily.builder().id(7L).name("Bundesliga").build();
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.of(existing));
        given(competitionFamilyRepository.existsByNameAndIdNot("Premier League", 7L)).willReturn(true);
        CompetitionFamilyDto request = CompetitionFamilyDto.builder()
                .name("Premier League")
                .hasClubs(true)
                .hasLigaModus(true)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionFamilyService.update(7L, request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CompetitionFamily with name Premier League already exists");
        assertThat(existing.getName()).isEqualTo("Bundesliga");
    }

    @Test
    void should_deleteFamily_when_familyExists() {
        // Arrange
        CompetitionFamily existing = CompetitionFamily.builder().id(7L).build();
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.of(existing));

        // Act
        competitionFamilyService.delete(7L);

        // Assert
        then(competitionFamilyRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownFamily() {
        // Arrange
        given(competitionFamilyRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionFamilyService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionFamily with id 42 not found");
        then(competitionFamilyRepository).should(never()).delete(any(CompetitionFamily.class));
    }
}
