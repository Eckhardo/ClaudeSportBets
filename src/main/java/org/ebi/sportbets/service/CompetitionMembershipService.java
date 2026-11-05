package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CompetitionMembershipDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionMembershipService {

    private final CompetitionMembershipRepository competitionMembershipRepository;
    private final CommunityRepository communityRepository;
    private final CompetitionRepository competitionRepository;

    // Both filters are optional; without any filter all memberships are returned
    public List<CompetitionMembershipDto> find(Long communityId, Long competitionId) {
        List<CompetitionMembership> memberships;
        if (communityId != null && competitionId != null) {
            memberships = competitionMembershipRepository.findByCommunityIdAndCompetitionId(communityId, competitionId)
                    .stream().toList();
        } else if (communityId != null) {
            memberships = competitionMembershipRepository.findByCommunityId(communityId);
        } else if (competitionId != null) {
            memberships = competitionMembershipRepository.findByCompetitionId(competitionId);
        } else {
            memberships = competitionMembershipRepository.findAll();
        }
        return memberships.stream()
                .map(this::toDto)
                .toList();
    }

    public CompetitionMembershipDto findById(Long id) {
        return competitionMembershipRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionMembership", id));
    }

    @Transactional
    public CompetitionMembershipDto create(CompetitionMembershipDto membershipDto) {
        Community community = findCommunity(membershipDto.getCommunityId());
        Competition competition = findCompetition(membershipDto.getCompetitionId());
        ensureNotMember(community.getId(), competition.getId(), null);
        // id and createdOn from the request are ignored; both are generated on insert
        CompetitionMembership membership = CompetitionMembership.builder()
                .community(community)
                .competition(competition)
                .build();
        return toDto(competitionMembershipRepository.save(membership));
    }

    @Transactional
    public CompetitionMembershipDto update(Long id, CompetitionMembershipDto membershipDto) {
        CompetitionMembership membership = competitionMembershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionMembership", id));
        Community community = findCommunity(membershipDto.getCommunityId());
        Competition competition = findCompetition(membershipDto.getCompetitionId());
        // The TippConfigs reference Spieltage of the current competition and TippModi of the current community,
        // so they cannot move to another one
        boolean moved = !community.getId().equals(membership.getCommunity().getId())
                || !competition.getId().equals(membership.getCompetition().getId());
        if (moved && !membership.getTippConfigs().isEmpty()) {
            throw new BusinessRuleException("CompetitionMembership " + id
                    + " has TippConfigs and cannot be moved to another Community or Competition");
        }
        ensureNotMember(community.getId(), competition.getId(), id);
        // The path id wins over any id in the body; createdOn is never changed
        membership.setCommunity(community);
        membership.setCompetition(competition);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(membership);
    }

    @Transactional
    public void delete(Long id) {
        CompetitionMembership membership = competitionMembershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionMembership", id));
        // The TippConfigs of this membership are removed via JPA cascade
        competitionMembershipRepository.delete(membership);
    }

    private Community findCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
    }

    private Competition findCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", competitionId));
    }

    // A Community may take part in a Competition only once; ownId excludes the membership being updated
    private void ensureNotMember(Long communityId, Long competitionId, Long ownId) {
        competitionMembershipRepository.findByCommunityIdAndCompetitionId(communityId, competitionId)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Community " + communityId
                            + " already takes part in Competition " + competitionId);
                });
    }

    private CompetitionMembershipDto toDto(CompetitionMembership membership) {
        return CompetitionMembershipDto.builder()
                .id(membership.getId())
                // getId() on the lazy proxies does not trigger a load of community or competition
                .communityId(membership.getCommunity().getId())
                .competitionId(membership.getCompetition().getId())
                .createdOn(membership.getCreatedOn())
                .build();
    }
}
