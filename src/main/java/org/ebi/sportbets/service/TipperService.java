package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipperService {

    private final TipperRepository tipperRepository;
    private final CommunityRepository communityRepository;
    private final CompetitionRepository competitionRepository;

    public List<TipperDto> findAll() {
        return tipperRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public TipperDto findById(Long id) {
        return tipperRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Tipper", id));
    }

    @Transactional
    public TipperDto create(TipperDto tipperDto) {
        if (tipperRepository.existsByUsername(tipperDto.getUsername())) {
            throw duplicate(tipperDto.getUsername());
        }
        // id from the request is ignored; it is generated on insert
        Tipper tipper = Tipper.builder().build();
        applyChanges(tipper, tipperDto);
        return toDto(tipperRepository.save(tipper));
    }

    @Transactional
    public TipperDto update(Long id, TipperDto tipperDto) {
        Tipper tipper = tipperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipper", id));
        if (tipperRepository.existsByUsernameAndIdNot(tipperDto.getUsername(), id)) {
            throw duplicate(tipperDto.getUsername());
        }
        // The path id wins over any id in the body
        applyChanges(tipper, tipperDto);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(tipper);
    }

    @Transactional
    public void delete(Long id) {
        Tipper tipper = tipperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipper", id));
        // TipperRoles, CommunityMemberships and their Tipps are removed via JPA cascade
        tipperRepository.delete(tipper);
    }

    // All checks run before the entity is touched, so a rejected request leaves it unchanged
    private void applyChanges(Tipper tipper, TipperDto tipperDto) {
        // The default ids are plain columns without FK constraints, so their targets are checked here
        Long defaultCommunityId = tipperDto.getDefaultCommunityId();
        if (defaultCommunityId != null && !communityRepository.existsById(defaultCommunityId)) {
            throw new ResourceNotFoundException("Community", defaultCommunityId);
        }
        Long defaultCompetitionId = tipperDto.getDefaultCompetitionId();
        if (defaultCompetitionId != null && !competitionRepository.existsById(defaultCompetitionId)) {
            throw new ResourceNotFoundException("Competition", defaultCompetitionId);
        }

        tipper.setUsername(tipperDto.getUsername());
        tipper.setFirstname(tipperDto.getFirstname());
        tipper.setLastname(tipperDto.getLastname());
        tipper.setEmail(tipperDto.getEmail());
        tipper.setPasswort(tipperDto.getPasswort());
        tipper.setPasswortHint(tipperDto.getPasswortHint());
        tipper.setCommunityAdmin(tipperDto.getCommunityAdmin());
        tipper.setCompetitionAdmin(tipperDto.getCompetitionAdmin());
        tipper.setDefaultCommunityId(defaultCommunityId);
        tipper.setDefaultCompetitionId(defaultCompetitionId);
    }

    private DuplicateResourceException duplicate(String username) {
        return new DuplicateResourceException("Tipper with username " + username + " already exists");
    }

    // passwort is write-only in the DTO, so it is never mapped back
    private TipperDto toDto(Tipper tipper) {
        return TipperDto.builder()
                .id(tipper.getId())
                .username(tipper.getUsername())
                .firstname(tipper.getFirstname())
                .lastname(tipper.getLastname())
                .email(tipper.getEmail())
                .passwortHint(tipper.getPasswortHint())
                .communityAdmin(tipper.isCommunityAdmin())
                .competitionAdmin(tipper.isCompetitionAdmin())
                .defaultCommunityId(tipper.getDefaultCommunityId())
                .defaultCompetitionId(tipper.getDefaultCompetitionId())
                .build();
    }
}
