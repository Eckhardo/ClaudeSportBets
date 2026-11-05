package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CompetitionMembershipDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.TippConfig;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CompetitionMembershipServiceTest {

    @Mock
    private CompetitionMembershipRepository competitionMembershipRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private CompetitionMembershipService competitionMembershipService;

    private static CompetitionMembership membership() {
        return CompetitionMembership.builder()
                .id(7L)
                .community(Community.builder().id(2L).build())
                .competition(Competition.builder().id(4L).build())
                .build();
    }

    private static CompetitionMembershipDto validRequest() {
        return CompetitionMembershipDto.builder().id(99L).communityId(2L).competitionId(4L).build();
    }

    @Test
    void should_mapAllFieldsToDto_when_findById() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CompetitionMembership membership = membership();
        membership.setCreatedOn(createdOn);
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));

        // Act
        CompetitionMembershipDto result = competitionMembershipService.findById(7L);

        // Assert
        assertThat(result).isEqualTo(CompetitionMembershipDto.builder()
                .id(7L).communityId(2L).competitionId(4L).createdOn(createdOn).build());
    }

    @Test
    void should_throwResourceNotFound_when_findByUnknownId() {
        // Arrange
        given(competitionMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.findById(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_returnMembershipsOfCommunity_when_findByCommunityId() {
        // Arrange
        given(competitionMembershipRepository.findByCommunityId(2L)).willReturn(List.of(membership()));

        // Act
        List<CompetitionMembershipDto> result = competitionMembershipService.find(2L, null);

        // Assert
        assertThat(result).extracting(CompetitionMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnMembershipsOfCompetition_when_findByCompetitionId() {
        // Arrange
        given(competitionMembershipRepository.findByCompetitionId(4L)).willReturn(List.of(membership()));

        // Act
        List<CompetitionMembershipDto> result = competitionMembershipService.find(null, 4L);

        // Assert
        assertThat(result).extracting(CompetitionMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnSingleMembership_when_findByCommunityIdAndCompetitionId() {
        // Arrange
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L))
                .willReturn(Optional.of(membership()));

        // Act
        List<CompetitionMembershipDto> result = competitionMembershipService.find(2L, 4L);

        // Assert
        assertThat(result).extracting(CompetitionMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnAllMemberships_when_noFilterIsGiven() {
        // Arrange
        given(competitionMembershipRepository.findAll()).willReturn(List.of(membership()));

        // Act
        List<CompetitionMembershipDto> result = competitionMembershipService.find(null, null);

        // Assert
        assertThat(result).extracting(CompetitionMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_saveMembershipIgnoringRequestId_when_createWithValidRequest() {
        // Arrange
        Community community = Community.builder().id(2L).build();
        Competition competition = Competition.builder().id(4L).build();
        given(communityRepository.findById(2L)).willReturn(Optional.of(community));
        given(competitionRepository.findById(4L)).willReturn(Optional.of(competition));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L)).willReturn(Optional.empty());
        given(competitionMembershipRepository.save(any(CompetitionMembership.class))).willReturn(membership());
        ArgumentCaptor<CompetitionMembership> captor = ArgumentCaptor.forClass(CompetitionMembership.class);

        // Act
        CompetitionMembershipDto result = competitionMembershipService.create(validRequest());

        // Assert
        then(competitionMembershipRepository).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getCommunity()).isSameAs(community);
        assertThat(captor.getValue().getCompetition()).isSameAs(competition);
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_communityAlreadyTakesPart() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.of(Community.builder().id(2L).build()));
        given(competitionRepository.findById(4L)).willReturn(Optional.of(Competition.builder().id(4L).build()));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L)).willReturn(Optional.of(membership()));

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        then(competitionMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownCommunity() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Community");
        then(competitionMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownCompetition() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.of(Community.builder().id(2L).build()));
        given(competitionRepository.findById(4L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Competition");
        then(competitionMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_moveToOtherCompetition_when_updateMembershipWithoutTippConfigs() {
        // Arrange
        CompetitionMembership membership = membership();
        Competition otherCompetition = Competition.builder().id(9L).build();
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(competitionRepository.findById(9L)).willReturn(Optional.of(otherCompetition));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 9L)).willReturn(Optional.empty());
        CompetitionMembershipDto request = CompetitionMembershipDto.builder()
                .id(99L).communityId(2L).competitionId(9L).build();

        // Act
        CompetitionMembershipDto result = competitionMembershipService.update(7L, request);

        // Assert
        assertThat(result).isEqualTo(CompetitionMembershipDto.builder().id(7L).communityId(2L).competitionId(9L).build());
        assertThat(membership.getCompetition()).isSameAs(otherCompetition);
    }

    @Test
    void should_throwBusinessRule_when_movingMembershipWithTippConfigsToOtherCompetition() {
        // Arrange
        CompetitionMembership membership = membership();
        membership.getTippConfigs().add(TippConfig.builder().id(12L).build());
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(competitionRepository.findById(9L)).willReturn(Optional.of(Competition.builder().id(9L).build()));
        CompetitionMembershipDto request = CompetitionMembershipDto.builder().communityId(2L).competitionId(9L).build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(membership.getCompetition().getId()).isEqualTo(4L);
    }

    @Test
    void should_throwBusinessRule_when_movingMembershipWithTippConfigsToOtherCommunity() {
        // Arrange
        CompetitionMembership membership = membership();
        membership.getTippConfigs().add(TippConfig.builder().id(12L).build());
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(6L)).willReturn(Optional.of(Community.builder().id(6L).build()));
        given(competitionRepository.findById(4L)).willReturn(Optional.of(membership.getCompetition()));
        CompetitionMembershipDto request = CompetitionMembershipDto.builder().communityId(6L).competitionId(4L).build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(membership.getCommunity().getId()).isEqualTo(2L);
    }

    @Test
    void should_succeed_when_updateMembershipWithTippConfigsWithUnchangedValues() {
        // Arrange
        CompetitionMembership membership = membership();
        membership.getTippConfigs().add(TippConfig.builder().id(12L).build());
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(competitionRepository.findById(4L)).willReturn(Optional.of(membership.getCompetition()));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L))
                .willReturn(Optional.of(membership));

        // Act
        CompetitionMembershipDto result = competitionMembershipService.update(7L, validRequest());

        // Assert
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_updateToExistingMembership() {
        // Arrange
        CompetitionMembership membership = membership();
        CompetitionMembership other = CompetitionMembership.builder().id(8L).build();
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(competitionRepository.findById(9L)).willReturn(Optional.of(Competition.builder().id(9L).build()));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 9L)).willReturn(Optional.of(other));
        CompetitionMembershipDto request = CompetitionMembershipDto.builder().communityId(2L).competitionId(9L).build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(membership.getCompetition().getId()).isEqualTo(4L);
    }

    @Test
    void should_throwResourceNotFound_when_updateUnknownId() {
        // Arrange
        given(competitionMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.update(42L, validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("CompetitionMembership");
    }

    @Test
    void should_deleteMembership_when_deleteExistingId() {
        // Arrange
        CompetitionMembership membership = membership();
        given(competitionMembershipRepository.findById(7L)).willReturn(Optional.of(membership));

        // Act
        competitionMembershipService.delete(7L);

        // Assert
        then(competitionMembershipRepository).should().delete(membership);
    }

    @Test
    void should_throwResourceNotFound_when_deleteUnknownId() {
        // Arrange
        given(competitionMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionMembershipService.delete(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        then(competitionMembershipRepository).should(never()).delete(any());
    }
}
