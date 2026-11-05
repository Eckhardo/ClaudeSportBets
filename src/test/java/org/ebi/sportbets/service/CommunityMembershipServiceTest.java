package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CommunityMembershipDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TipperRepository;
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
class CommunityMembershipServiceTest {

    @Mock
    private CommunityMembershipRepository communityMembershipRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private TipperRepository tipperRepository;

    @InjectMocks
    private CommunityMembershipService communityMembershipService;

    private static CommunityMembership membership() {
        return CommunityMembership.builder()
                .id(7L)
                .community(Community.builder().id(2L).build())
                .tipper(Tipper.builder().id(3L).build())
                .build();
    }

    private static CommunityMembershipDto validRequest() {
        return CommunityMembershipDto.builder().id(99L).communityId(2L).tipperId(3L).build();
    }

    @Test
    void should_mapAllFieldsToDto_when_findById() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CommunityMembership membership = membership();
        membership.setCreatedOn(createdOn);
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));

        // Act
        CommunityMembershipDto result = communityMembershipService.findById(7L);

        // Assert
        assertThat(result).isEqualTo(CommunityMembershipDto.builder()
                .id(7L).communityId(2L).tipperId(3L).createdOn(createdOn).build());
    }

    @Test
    void should_throwResourceNotFound_when_findByUnknownId() {
        // Arrange
        given(communityMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.findById(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_returnMembershipsOfCommunity_when_findByCommunityId() {
        // Arrange
        given(communityMembershipRepository.findByCommunityId(2L)).willReturn(List.of(membership()));

        // Act
        List<CommunityMembershipDto> result = communityMembershipService.find(2L, null);

        // Assert
        assertThat(result).extracting(CommunityMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnMembershipsOfTipper_when_findByTipperId() {
        // Arrange
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of(membership()));

        // Act
        List<CommunityMembershipDto> result = communityMembershipService.find(null, 3L);

        // Assert
        assertThat(result).extracting(CommunityMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnSingleMembership_when_findByCommunityIdAndTipperId() {
        // Arrange
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 3L))
                .willReturn(Optional.of(membership()));

        // Act
        List<CommunityMembershipDto> result = communityMembershipService.find(2L, 3L);

        // Assert
        assertThat(result).extracting(CommunityMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnAllMemberships_when_noFilterIsGiven() {
        // Arrange
        given(communityMembershipRepository.findAll()).willReturn(List.of(membership()));

        // Act
        List<CommunityMembershipDto> result = communityMembershipService.find(null, null);

        // Assert
        assertThat(result).extracting(CommunityMembershipDto::getId).containsExactly(7L);
    }

    @Test
    void should_saveMembershipIgnoringRequestId_when_createWithValidRequest() {
        // Arrange
        Community community = Community.builder().id(2L).build();
        Tipper tipper = Tipper.builder().id(3L).build();
        given(communityRepository.findById(2L)).willReturn(Optional.of(community));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 3L)).willReturn(Optional.empty());
        given(communityMembershipRepository.save(any(CommunityMembership.class))).willReturn(membership());
        ArgumentCaptor<CommunityMembership> captor = ArgumentCaptor.forClass(CommunityMembership.class);

        // Act
        CommunityMembershipDto result = communityMembershipService.create(validRequest());

        // Assert
        then(communityMembershipRepository).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getCommunity()).isSameAs(community);
        assertThat(captor.getValue().getTipper()).isSameAs(tipper);
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_tipperIsAlreadyMember() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.of(Community.builder().id(2L).build()));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(Tipper.builder().id(3L).build()));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 3L)).willReturn(Optional.of(membership()));

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        then(communityMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownCommunity() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Community");
        then(communityMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownTipper() {
        // Arrange
        given(communityRepository.findById(2L)).willReturn(Optional.of(Community.builder().id(2L).build()));
        given(tipperRepository.findById(3L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Tipper");
        then(communityMembershipRepository).should(never()).save(any());
    }

    @Test
    void should_changeTipperAndKeepTipps_when_updateWithOtherTipper() {
        // Arrange
        CommunityMembership membership = membership();
        membership.getTipps().add(Tipp.builder().id(11L).build());
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(tipperRepository.findById(5L)).willReturn(Optional.of(Tipper.builder().id(5L).build()));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 5L)).willReturn(Optional.empty());
        CommunityMembershipDto request = CommunityMembershipDto.builder().id(99L).communityId(2L).tipperId(5L).build();

        // Act
        CommunityMembershipDto result = communityMembershipService.update(7L, request);

        // Assert
        assertThat(result).isEqualTo(CommunityMembershipDto.builder().id(7L).communityId(2L).tipperId(5L).build());
        assertThat(membership.getTipps()).hasSize(1);
    }

    @Test
    void should_moveToOtherCommunity_when_updateMembershipWithoutTipps() {
        // Arrange
        CommunityMembership membership = membership();
        Community otherCommunity = Community.builder().id(6L).build();
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(6L)).willReturn(Optional.of(otherCommunity));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(membership.getTipper()));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(6L, 3L)).willReturn(Optional.empty());
        CommunityMembershipDto request = CommunityMembershipDto.builder().communityId(6L).tipperId(3L).build();

        // Act
        CommunityMembershipDto result = communityMembershipService.update(7L, request);

        // Assert
        assertThat(result.getCommunityId()).isEqualTo(6L);
        assertThat(membership.getCommunity()).isSameAs(otherCommunity);
    }

    @Test
    void should_throwBusinessRule_when_movingMembershipWithTippsToOtherCommunity() {
        // Arrange
        CommunityMembership membership = membership();
        membership.getTipps().add(Tipp.builder().id(11L).build());
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(6L)).willReturn(Optional.of(Community.builder().id(6L).build()));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(membership.getTipper()));
        CommunityMembershipDto request = CommunityMembershipDto.builder().communityId(6L).tipperId(3L).build();

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(membership.getCommunity().getId()).isEqualTo(2L);
    }

    @Test
    void should_succeed_when_updateWithUnchangedValues() {
        // Arrange
        CommunityMembership membership = membership();
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(membership.getTipper()));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 3L)).willReturn(Optional.of(membership));

        // Act
        CommunityMembershipDto result = communityMembershipService.update(7L, validRequest());

        // Assert
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_updateToExistingMembership() {
        // Arrange
        CommunityMembership membership = membership();
        CommunityMembership other = CommunityMembership.builder().id(8L).build();
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));
        given(communityRepository.findById(2L)).willReturn(Optional.of(membership.getCommunity()));
        given(tipperRepository.findById(5L)).willReturn(Optional.of(Tipper.builder().id(5L).build()));
        given(communityMembershipRepository.findByCommunityIdAndTipperId(2L, 5L)).willReturn(Optional.of(other));
        CommunityMembershipDto request = CommunityMembershipDto.builder().communityId(2L).tipperId(5L).build();

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(membership.getTipper().getId()).isEqualTo(3L);
    }

    @Test
    void should_throwResourceNotFound_when_updateUnknownId() {
        // Arrange
        given(communityMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.update(42L, validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("CommunityMembership");
    }

    @Test
    void should_deleteMembership_when_deleteExistingId() {
        // Arrange
        CommunityMembership membership = membership();
        given(communityMembershipRepository.findById(7L)).willReturn(Optional.of(membership));

        // Act
        communityMembershipService.delete(7L);

        // Assert
        then(communityMembershipRepository).should().delete(membership);
    }

    @Test
    void should_throwResourceNotFound_when_deleteUnknownId() {
        // Arrange
        given(communityMembershipRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> communityMembershipService.delete(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        then(communityMembershipRepository).should(never()).delete(any());
    }
}
