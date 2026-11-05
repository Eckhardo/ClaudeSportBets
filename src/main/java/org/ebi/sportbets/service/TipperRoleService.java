package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.TipperRoleDto;
import org.ebi.sportbets.entity.Role;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.RoleRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.ebi.sportbets.repository.TipperRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipperRoleService {
    private final TipperRoleRepository tipperRoleRepo;

    private final TipperRepository tipperRepository;
    private final RoleRepository roleRepository;

    // The filter is optional; without it all TipperRoles are returned
    public List<TipperRoleDto> find(Long tipperId) {
        List<TipperRole> tipperRoles = tipperId != null
                ? tipperRoleRepo.findByTipperId(tipperId)
                : tipperRoleRepo.findAll();
        return tipperRoles.stream()
                .map(this::toDto)
                .toList();
    }

    public TipperRoleDto findById(Long id) {
        return tipperRoleRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("TipperRole", id));
    }

    @Transactional
    public TipperRoleDto create(TipperRoleDto tipperRoleDto) {
        Tipper tipper = findTipper(tipperRoleDto.getTipperId());
        Role role = findRole(tipperRoleDto.getRoleId());
        ensureRoleNotAssigned(tipper.getId(), role.getId(), null);
        // id from the request is ignored; it is generated on insert
        TipperRole tipperRole = TipperRole.builder()
                .tipper(tipper)
                .role(role)
                .build();
        return toDto(tipperRoleRepo.save(tipperRole));
    }

    @Transactional
    public TipperRoleDto update(Long id, TipperRoleDto tipperRoleDto) {
        TipperRole tipperRole = tipperRoleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipperRole", id));
        Tipper tipper = findTipper(tipperRoleDto.getTipperId());
        Role role = findRole(tipperRoleDto.getRoleId());
        ensureRoleNotAssigned(tipper.getId(), role.getId(), id);
        // The path id wins over any id in the body
        tipperRole.setTipper(tipper);
        tipperRole.setRole(role);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(tipperRole);
    }

    @Transactional
    public void delete(Long id) {
        TipperRole tipperRole = tipperRoleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipperRole", id));
        tipperRoleRepo.delete(tipperRole);
    }

    private Tipper findTipper(Long tipperId) {
        return tipperRepository.findById(tipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipper", tipperId));
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    // A Role may be assigned to a Tipper only once; ownId excludes the TipperRole being updated
    private void ensureRoleNotAssigned(Long tipperId, Long roleId, Long ownId) {
        tipperRoleRepo.findByTipperIdAndRoleId(tipperId, roleId)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Role " + roleId
                            + " is already assigned to Tipper " + tipperId);
                });
    }

    private TipperRoleDto toDto(TipperRole tipperRole) {
        return TipperRoleDto.builder()
                .id(tipperRole.getId())
                // getId() on the lazy proxies does not trigger a load of tipper or role
                .tipperId(tipperRole.getTipper().getId())
                .roleId(tipperRole.getRole().getId())
                .build();
    }
}
