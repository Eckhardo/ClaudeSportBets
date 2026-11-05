package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CommunityMembershipDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityMembershipService {

    private final CommunityMembershipRepository communityMembershipRepository;
    private final CommunityRepository communityRepository;
    private final TipperRepository tipperRepository;

    // Both filters are optional; without any filter all memberships are returned
    public List<CommunityMembershipDto> find(Long communityId, Long tipperId) {
        List<CommunityMembership> memberships;
        if (communityId != null && tipperId != null) {
            memberships = communityMembershipRepository.findByCommunityIdAndTipperId(communityId, tipperId)
                    .stream().toList();
        } else if (communityId != null) {
            memberships = communityMembershipRepository.findByCommunityId(communityId);
        } else if (tipperId != null) {
            memberships = communityMembershipRepository.findByTipperId(tipperId);
        } else {
            memberships = communityMembershipRepository.findAll();
        }
        return memberships.stream()
                .map(this::toDto)
                .toList();
    }

    public CommunityMembershipDto findById(Long id) {
        return communityMembershipRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityMembership", id));
    }

    @Transactional
    public CommunityMembershipDto create(CommunityMembershipDto membershipDto) {
        Community community = findCommunity(membershipDto.getCommunityId());
        Tipper tipper = findTipper(membershipDto.getTipperId());
        ensureNotMember(community.getId(), tipper.getId(), null);
        // id and createdOn from the request are ignored; both are generated on insert
        CommunityMembership membership = CommunityMembership.builder()
                .community(community)
                .tipper(tipper)
                .build();
        return toDto(communityMembershipRepository.save(membership));
    }

    @Transactional
    public CommunityMembershipDto update(Long id, CommunityMembershipDto membershipDto) {
        CommunityMembership membership = communityMembershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityMembership", id));
        Community community = findCommunity(membershipDto.getCommunityId());
        Tipper tipper = findTipper(membershipDto.getTipperId());
        // The Tipps reference TippModi of the current community, so they cannot move to another one
        if (!community.getId().equals(membership.getCommunity().getId()) && !membership.getTipps().isEmpty()) {
            throw new BusinessRuleException("CommunityMembership " + id
                    + " has Tipps and cannot be moved to another Community");
        }
        ensureNotMember(community.getId(), tipper.getId(), id);
        // The path id wins over any id in the body; createdOn is never changed.
        // Changing the tipper hands the existing Tipps over to the new tipper.
        membership.setCommunity(community);
        membership.setTipper(tipper);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(membership);
    }

    @Transactional
    public void delete(Long id) {
        CommunityMembership membership = communityMembershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityMembership", id));
        // The Tipps of this membership are removed via JPA cascade
        communityMembershipRepository.delete(membership);
    }

    private Community findCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
    }

    private Tipper findTipper(Long tipperId) {
        return tipperRepository.findById(tipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipper", tipperId));
    }

    // A Tipper may be a member of a Community only once; ownId excludes the membership being updated
    private void ensureNotMember(Long communityId, Long tipperId, Long ownId) {
        communityMembershipRepository.findByCommunityIdAndTipperId(communityId, tipperId)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Tipper " + tipperId
                            + " is already a member of Community " + communityId);
                });
    }

    private CommunityMembershipDto toDto(CommunityMembership membership) {
        return CommunityMembershipDto.builder()
                .id(membership.getId())
                // getId() on the lazy proxies does not trigger a load of community or tipper
                .communityId(membership.getCommunity().getId())
                .tipperId(membership.getTipper().getId())
                .createdOn(membership.getCreatedOn())
                .build();
    }
}
