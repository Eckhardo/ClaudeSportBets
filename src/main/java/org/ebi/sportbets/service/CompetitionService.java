package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionFamilyRepository competitionFamilyRepository;

    public List<CompetitionDto> findAll() {
        return competitionRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CompetitionDto create(CompetitionDto competitionDto) {
        CompetitionFamily competitionFamily = competitionFamilyRepository.findById(competitionDto.getCompetitionFamilyId())
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionFamily", competitionDto.getCompetitionFamilyId()));
        // id and createdOn from the request are ignored; both are generated on insert
        Competition competition = Competition.builder()
                .name(competitionDto.getName())
                .description(competitionDto.getDescription())
                .winMultiplicator(competitionDto.getWinMultiplicator())
                .remisMultiplicator(competitionDto.getRemisMultiplicator())
                .competitionFamily(competitionFamily)
                .build();
        return toDto(competitionRepository.save(competition));
    }

    private CompetitionDto toDto(Competition competition) {
        return CompetitionDto.builder()
                .id(competition.getId())
                .name(competition.getName())
                .description(competition.getDescription())
                .winMultiplicator(competition.getWinMultiplicator())
                .remisMultiplicator(competition.getRemisMultiplicator())
                // getId() on the lazy proxy does not trigger a load of the family
                .competitionFamilyId(competition.getCompetitionFamily().getId())
                .createdOn(competition.getCreatedOn())
                .build();
    }
}
