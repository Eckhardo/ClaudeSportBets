package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.TipperRoleDto;
import org.ebi.sportbets.entity.CommunityRole;
import org.ebi.sportbets.entity.Role;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.RoleRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.ebi.sportbets.repository.TipperRoleRepository;
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
class TipperRoleServiceTest {

    @Mock
    private TipperRoleRepository tipperRoleRepo;

    @Mock
    private TipperRepository tipperRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private TipperRoleService tipperRoleService;

    private static TipperRole tipperRole() {
        return TipperRole.builder()
                .id(7L)
                .tipper(Tipper.builder().id(3L).build())
                .role(CommunityRole.builder().id(5L).build())
                .build();
    }

    private static TipperRoleDto validRequest() {
        return TipperRoleDto.builder().id(99L).tipperId(3L).roleId(5L).build();
    }

    @Test
    void should_mapAllFieldsToDto_when_findById() {
        // Arrange
        given(tipperRoleRepo.findById(7L)).willReturn(Optional.of(tipperRole()));

        // Act
        TipperRoleDto result = tipperRoleService.findById(7L);

        // Assert
        assertThat(result).isEqualTo(TipperRoleDto.builder().id(7L).tipperId(3L).roleId(5L).build());
    }

    @Test
    void should_throwResourceNotFound_when_findByUnknownId() {
        // Arrange
        given(tipperRoleRepo.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.findById(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_returnTipperRolesOfTipper_when_findByTipperId() {
        // Arrange
        given(tipperRoleRepo.findByTipperId(3L)).willReturn(List.of(tipperRole()));

        // Act
        List<TipperRoleDto> result = tipperRoleService.find(3L);

        // Assert
        assertThat(result).extracting(TipperRoleDto::getId).containsExactly(7L);
    }

    @Test
    void should_returnAllTipperRoles_when_noFilterIsGiven() {
        // Arrange
        given(tipperRoleRepo.findAll()).willReturn(List.of(tipperRole()));

        // Act
        List<TipperRoleDto> result = tipperRoleService.find(null);

        // Assert
        assertThat(result).extracting(TipperRoleDto::getId).containsExactly(7L);
    }

    @Test
    void should_saveTipperRoleIgnoringRequestId_when_createWithValidRequest() {
        // Arrange
        Tipper tipper = Tipper.builder().id(3L).build();
        Role role = CommunityRole.builder().id(5L).build();
        given(tipperRepository.findById(3L)).willReturn(Optional.of(tipper));
        given(roleRepository.findById(5L)).willReturn(Optional.of(role));
        given(tipperRoleRepo.findByTipperIdAndRoleId(3L, 5L)).willReturn(Optional.empty());
        given(tipperRoleRepo.save(any(TipperRole.class))).willReturn(tipperRole());
        ArgumentCaptor<TipperRole> captor = ArgumentCaptor.forClass(TipperRole.class);

        // Act
        TipperRoleDto result = tipperRoleService.create(validRequest());

        // Assert
        then(tipperRoleRepo).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getTipper()).isSameAs(tipper);
        assertThat(captor.getValue().getRole()).isSameAs(role);
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_roleIsAlreadyAssigned() {
        // Arrange
        given(tipperRepository.findById(3L)).willReturn(Optional.of(Tipper.builder().id(3L).build()));
        given(roleRepository.findById(5L)).willReturn(Optional.of(CommunityRole.builder().id(5L).build()));
        given(tipperRoleRepo.findByTipperIdAndRoleId(3L, 5L)).willReturn(Optional.of(tipperRole()));

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        then(tipperRoleRepo).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownTipper() {
        // Arrange
        given(tipperRepository.findById(3L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Tipper");
        then(tipperRoleRepo).should(never()).save(any());
    }

    @Test
    void should_throwResourceNotFound_when_createWithUnknownRole() {
        // Arrange
        given(tipperRepository.findById(3L)).willReturn(Optional.of(Tipper.builder().id(3L).build()));
        given(roleRepository.findById(5L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.create(validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Role");
        then(tipperRoleRepo).should(never()).save(any());
    }

    @Test
    void should_changeRole_when_updateWithOtherRole() {
        // Arrange
        TipperRole tipperRole = tipperRole();
        Role otherRole = CommunityRole.builder().id(6L).build();
        given(tipperRoleRepo.findById(7L)).willReturn(Optional.of(tipperRole));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(tipperRole.getTipper()));
        given(roleRepository.findById(6L)).willReturn(Optional.of(otherRole));
        given(tipperRoleRepo.findByTipperIdAndRoleId(3L, 6L)).willReturn(Optional.empty());
        TipperRoleDto request = TipperRoleDto.builder().id(99L).tipperId(3L).roleId(6L).build();

        // Act
        TipperRoleDto result = tipperRoleService.update(7L, request);

        // Assert
        assertThat(result).isEqualTo(TipperRoleDto.builder().id(7L).tipperId(3L).roleId(6L).build());
        assertThat(tipperRole.getRole()).isSameAs(otherRole);
    }

    @Test
    void should_succeed_when_updateWithUnchangedValues() {
        // Arrange
        TipperRole tipperRole = tipperRole();
        given(tipperRoleRepo.findById(7L)).willReturn(Optional.of(tipperRole));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(tipperRole.getTipper()));
        given(roleRepository.findById(5L)).willReturn(Optional.of(tipperRole.getRole()));
        given(tipperRoleRepo.findByTipperIdAndRoleId(3L, 5L)).willReturn(Optional.of(tipperRole));

        // Act
        TipperRoleDto result = tipperRoleService.update(7L, validRequest());

        // Assert
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void should_throwDuplicateResource_when_updateToExistingAssignment() {
        // Arrange
        TipperRole tipperRole = tipperRole();
        TipperRole other = TipperRole.builder().id(8L).build();
        given(tipperRoleRepo.findById(7L)).willReturn(Optional.of(tipperRole));
        given(tipperRepository.findById(3L)).willReturn(Optional.of(tipperRole.getTipper()));
        given(roleRepository.findById(6L)).willReturn(Optional.of(CommunityRole.builder().id(6L).build()));
        given(tipperRoleRepo.findByTipperIdAndRoleId(3L, 6L)).willReturn(Optional.of(other));
        TipperRoleDto request = TipperRoleDto.builder().tipperId(3L).roleId(6L).build();

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.update(7L, request));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(tipperRole.getRole().getId()).isEqualTo(5L);
    }

    @Test
    void should_throwResourceNotFound_when_updateUnknownId() {
        // Arrange
        given(tipperRoleRepo.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.update(42L, validRequest()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("TipperRole");
    }

    @Test
    void should_deleteTipperRole_when_deleteExistingId() {
        // Arrange
        TipperRole tipperRole = tipperRole();
        given(tipperRoleRepo.findById(7L)).willReturn(Optional.of(tipperRole));

        // Act
        tipperRoleService.delete(7L);

        // Assert
        then(tipperRoleRepo).should().delete(tipperRole);
    }

    @Test
    void should_throwResourceNotFound_when_deleteUnknownId() {
        // Arrange
        given(tipperRoleRepo.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> tipperRoleService.delete(42L));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        then(tipperRoleRepo).should(never()).delete(any());
    }
}
