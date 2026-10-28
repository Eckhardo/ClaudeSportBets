package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CompetitionFamilyDto;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionFamilyService {

    private final CompetitionFamilyRepository competitionFamilyRepository;

    public List<CompetitionFamilyDto> findAll() {
        return competitionFamilyRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public CompetitionFamilyDto findById(Long id) {
        return competitionFamilyRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionFamily", id));
    }

    @Transactional
    public CompetitionFamilyDto create(CompetitionFamilyDto competitionFamilyDto) {
        if (competitionFamilyRepository.existsByName(competitionFamilyDto.getName())) {
            throw duplicateName(competitionFamilyDto.getName());
        }
        // id and createdOn from the request are ignored; both are generated on insert
        CompetitionFamily competitionFamily = CompetitionFamily.builder()
                .name(competitionFamilyDto.getName())
                .description(competitionFamilyDto.getDescription())
                .country(competitionFamilyDto.getCountry())
                .hasClubs(competitionFamilyDto.getHasClubs())
                .hasLigaModus(competitionFamilyDto.getHasLigaModus())
                .build();
        return toDto(competitionFamilyRepository.save(competitionFamily));
    }

    @Transactional
    public CompetitionFamilyDto update(Long id, CompetitionFamilyDto competitionFamilyDto) {
        CompetitionFamily competitionFamily = competitionFamilyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionFamily", id));
        if (competitionFamilyRepository.existsByNameAndIdNot(competitionFamilyDto.getName(), id)) {
            throw duplicateName(competitionFamilyDto.getName());
        }
        // The path id wins over any id in the body; createdOn is never changed
        competitionFamily.setName(competitionFamilyDto.getName());
        competitionFamily.setDescription(competitionFamilyDto.getDescription());
        competitionFamily.setCountry(competitionFamilyDto.getCountry());
        competitionFamily.setHasClubs(competitionFamilyDto.getHasClubs());
        competitionFamily.setHasLigaModus(competitionFamilyDto.getHasLigaModus());
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(competitionFamily);
    }

    @Transactional
    public void delete(Long id) {
        CompetitionFamily competitionFamily = competitionFamilyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionFamily", id));
        // Competitions (and everything below them) are removed via JPA cascade
        competitionFamilyRepository.delete(competitionFamily);
    }

    private DuplicateResourceException duplicateName(String name) {
        return new DuplicateResourceException("CompetitionFamily with name " + name + " already exists");
    }

    private CompetitionFamilyDto toDto(CompetitionFamily competitionFamily) {
        return CompetitionFamilyDto.builder()
                .id(competitionFamily.getId())
                .name(competitionFamily.getName())
                .description(competitionFamily.getDescription())
                .country(competitionFamily.getCountry())
                .hasClubs(competitionFamily.isHasClubs())
                .hasLigaModus(competitionFamily.isHasLigaModus())
                .createdOn(competitionFamily.getCreatedOn())
                .build();
    }
}
