package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CommunityRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final TipperRepository tipperRepository;

    public List<CommunityDto> findAll() {
        return communityRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public CommunityDto findById(Long id) {
        return communityRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Community", id));
    }

    @Transactional
    public CommunityDto create(CommunityDto communityDto) {
        if (communityRepository.existsByName(communityDto.getName())) {
            throw duplicate(communityDto.getName());
        }
        // id and createdOn from the request are ignored; both are generated on insert
        Community community = Community.builder()
                .name(communityDto.getName())
                .description(communityDto.getDescription())
                .build();
        return toDto(communityRepository.save(community));
    }

    @Transactional
    public CommunityDto update(Long id, CommunityDto communityDto) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community", id));
        if (communityRepository.existsByNameAndIdNot(communityDto.getName(), id)) {
            throw duplicate(communityDto.getName());
        }
        // The path id wins over any id in the body; createdOn is never changed
        community.setName(communityDto.getName());
        community.setDescription(communityDto.getDescription());
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(community);
    }

    @Transactional
    public void delete(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community", id));
        tipperRepository.clearDefaultCommunity(id);
        // Memberships, Tipps, TippConfigs, TippModi and CommunityRoles are removed via JPA cascade
        communityRepository.delete(community);
    }

    private DuplicateResourceException duplicate(String name) {
        return new DuplicateResourceException("Community with name " + name + " already exists");
    }

    private CommunityDto toDto(Community community) {
        return CommunityDto.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .createdOn(community.getCreatedOn())
                .build();
    }
}
