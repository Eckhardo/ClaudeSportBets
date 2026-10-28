package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.TippDto;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Tipp;
import org.ebi.sportbets.entity.TippModus;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.TippModusRepository;
import org.ebi.sportbets.repository.TippRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TippService {

    private final TippRepository tippRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final SpielRepository spielRepository;
    private final TippModusRepository tippModusRepository;

    // Both filters are optional; null means "do not filter by this"
    public List<TippDto> find(Long communityMembershipId, Long spielId) {
        List<Tipp> tipps;
        if (communityMembershipId != null && spielId != null) {
            tipps = tippRepository.findByCommunityMembershipIdAndSpielId(communityMembershipId, spielId);
        } else if (communityMembershipId != null) {
            tipps = tippRepository.findByCommunityMembershipId(communityMembershipId);
        } else if (spielId != null) {
            tipps = tippRepository.findBySpielId(spielId);
        } else {
            tipps = tippRepository.findAll();
        }
        return tipps.stream()
                .map(this::toDto)
                .toList();
    }

    public TippDto findById(Long id) {
        return tippRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Tipp", id));
    }

    @Transactional
    public TippDto create(TippDto tippDto) {
        Tipp tipp = Tipp.builder().build();
        applyChanges(tipp, tippDto, null);
        // id and createdOn from the request are ignored; both are generated on insert
        return toDto(tippRepository.save(tipp));
    }

    @Transactional
    public TippDto update(Long id, TippDto tippDto) {
        Tipp tipp = tippRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipp", id));
        // The path id wins over any id in the body; createdOn is never changed
        applyChanges(tipp, tippDto, id);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(tipp);
    }

    /**
     * Persists or updates all given Tipps in one transaction: if one item fails, nothing is saved.
     * An item with an id updates that Tipp; an item without an id updates the member's existing Tipp
     * for the same Spiel and TippModus, or creates a new one if there is none.
     */
    @Transactional
    public List<TippDto> saveAll(List<TippDto> tippDtos) {
        ensureUniqueKeysInRequest(tippDtos);
        return tippDtos.stream()
                .map(this::save)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Tipp tipp = tippRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipp", id));
        tippRepository.delete(tipp);
    }

    private TippDto save(TippDto tippDto) {
        if (tippDto.getId() != null) {
            return update(tippDto.getId(), tippDto);
        }
        return tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(
                        tippDto.getCommunityMembershipId(), tippDto.getSpielId(), tippDto.getTippModusId())
                .map(existing -> update(existing.getId(), tippDto))
                .orElseGet(() -> create(tippDto));
    }

    // Two items for the same member, Spiel and TippModus would silently overwrite each other
    private void ensureUniqueKeysInRequest(List<TippDto> tippDtos) {
        Set<TippKey> keys = new HashSet<>();
        for (TippDto tippDto : tippDtos) {
            TippKey key = new TippKey(tippDto.getCommunityMembershipId(), tippDto.getSpielId(), tippDto.getTippModusId());
            if (!keys.add(key)) {
                throw new BusinessRuleException("Request contains more than one Tipp of CommunityMembership "
                        + key.communityMembershipId() + " for Spiel " + key.spielId()
                        + " and TippModus " + key.tippModusId());
            }
        }
    }

    private record TippKey(Long communityMembershipId, Long spielId, Long tippModusId) {
    }

    // All checks run before the entity is touched, so a rejected request leaves it unchanged
    private void applyChanges(Tipp tipp, TippDto tippDto, Long ownId) {
        CommunityMembership communityMembership = communityMembershipRepository.findById(tippDto.getCommunityMembershipId())
                .orElseThrow(() -> new ResourceNotFoundException("CommunityMembership", tippDto.getCommunityMembershipId()));
        Spiel spiel = spielRepository.findById(tippDto.getSpielId())
                .orElseThrow(() -> new ResourceNotFoundException("Spiel", tippDto.getSpielId()));
        TippModus tippModus = tippModusRepository.findById(tippDto.getTippModusId())
                .orElseThrow(() -> new ResourceNotFoundException("TippModus", tippDto.getTippModusId()));
        ensureSameCommunity(communityMembership, tippModus);
        ensureNoOtherTipp(communityMembership.getId(), spiel.getId(), tippModus.getId(), ownId);

        tipp.setCommunityMembership(communityMembership);
        tipp.setSpiel(spiel);
        tipp.setTippModus(tippModus);
        tipp.setHeimTipp(tippDto.getHeimTipp());
        tipp.setGastTipp(tippDto.getGastTipp());
        tipp.setRemisTipp(tippDto.getRemisTipp());
        tipp.setWinPoints(tippDto.getWinPoints());
    }

    // A member may only tipp with a tipp mode of their own community
    private void ensureSameCommunity(CommunityMembership communityMembership, TippModus tippModus) {
        // getId() on the lazy proxies does not trigger a load of the communities
        if (!communityMembership.getCommunity().getId().equals(tippModus.getCommunity().getId())) {
            throw new BusinessRuleException("TippModus " + tippModus.getId()
                    + " does not belong to the community of CommunityMembership " + communityMembership.getId());
        }
    }

    // One Tipp per member, Spiel and tipp mode; ownId excludes the Tipp being updated
    private void ensureNoOtherTipp(Long communityMembershipId, Long spielId, Long tippModusId, Long ownId) {
        tippRepository.findByCommunityMembershipIdAndSpielIdAndTippModusId(communityMembershipId, spielId, tippModusId)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Tipp of CommunityMembership " + communityMembershipId
                            + " for Spiel " + spielId + " and TippModus " + tippModusId + " already exists");
                });
    }

    private TippDto toDto(Tipp tipp) {
        // getId() on the lazy proxies does not trigger a load of the referenced entities
        return TippDto.builder()
                .id(tipp.getId())
                .communityMembershipId(tipp.getCommunityMembership().getId())
                .spielId(tipp.getSpiel().getId())
                .tippModusId(tipp.getTippModus().getId())
                .heimTipp(tipp.getHeimTipp())
                .gastTipp(tipp.getGastTipp())
                .remisTipp(tipp.getRemisTipp())
                .winPoints(tipp.getWinPoints())
                .createdOn(tipp.getCreatedOn())
                .build();
    }
}
