package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.CompetitionRoundDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionRoundService {

    private final CompetitionRoundRepository competitionRoundRepository;
    private final CompetitionRepository competitionRepository;

    public List<CompetitionRoundDto> findAll() {
        return competitionRoundRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<CompetitionRoundDto> findByCompetitionId(Long competitionId) {
        return competitionRoundRepository.findByCompetitionIdOrderByRoundNumberAsc(competitionId).stream()
                .map(this::toDto)
                .toList();
    }

    public CompetitionRoundDto findById(Long id) {
        return competitionRoundRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionRound", id));
    }

    @Transactional
    public CompetitionRoundDto create(CompetitionRoundDto competitionRoundDto) {
        Competition competition = findCompetition(competitionRoundDto.getCompetitionId());
        ensureRoundNumberIsFree(competition.getId(), competitionRoundDto.getRoundNumber(), null);
        // id and createdOn from the request are ignored; both are generated on insert
        CompetitionRound competitionRound = CompetitionRound.builder()
                .name(competitionRoundDto.getName())
                .roundNumber(competitionRoundDto.getRoundNumber())
                .firstMatchday(competitionRoundDto.getFirstMatchday())
                .matchdaysSize(competitionRoundDto.getMatchdaysSize())
                .teamsSize(competitionRoundDto.getTeamsSize())
                .hasGroups(competitionRoundDto.getHasGroups())
                .competition(competition)
                .build();
        return toDto(competitionRoundRepository.save(competitionRound));
    }

    @Transactional
    public CompetitionRoundDto update(Long id, CompetitionRoundDto competitionRoundDto) {
        CompetitionRound competitionRound = competitionRoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionRound", id));
        Competition competition = findCompetition(competitionRoundDto.getCompetitionId());
        ensureRoundNumberIsFree(competition.getId(), competitionRoundDto.getRoundNumber(), id);
        // The path id wins over any id in the body; createdOn is never changed
        competitionRound.setName(competitionRoundDto.getName());
        competitionRound.setRoundNumber(competitionRoundDto.getRoundNumber());
        competitionRound.setFirstMatchday(competitionRoundDto.getFirstMatchday());
        competitionRound.setMatchdaysSize(competitionRoundDto.getMatchdaysSize());
        competitionRound.setTeamsSize(competitionRoundDto.getTeamsSize());
        competitionRound.setHasGroups(competitionRoundDto.getHasGroups());
        competitionRound.setCompetition(competition);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(competitionRound);
    }

    @Transactional
    public void delete(Long id) {
        CompetitionRound competitionRound = competitionRoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionRound", id));
        // Spieltage and groups (and everything below them) are removed via JPA cascade
        competitionRoundRepository.delete(competitionRound);
    }

    private Competition findCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", competitionId));
    }

    // A round number may only be used once per competition; ownId excludes the round being updated
    private void ensureRoundNumberIsFree(Long competitionId, int roundNumber, Long ownId) {
        competitionRoundRepository.findByCompetitionIdAndRoundNumber(competitionId, roundNumber)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("CompetitionRound with round number " + roundNumber
                            + " already exists for Competition " + competitionId);
                });
    }

    private CompetitionRoundDto toDto(CompetitionRound competitionRound) {
        return CompetitionRoundDto.builder()
                .id(competitionRound.getId())
                .name(competitionRound.getName())
                .roundNumber(competitionRound.getRoundNumber())
                .firstMatchday(competitionRound.getFirstMatchday())
                .matchdaysSize(competitionRound.getMatchdaysSize())
                .teamsSize(competitionRound.getTeamsSize())
                .hasGroups(competitionRound.isHasGroups())
                // getId() on the lazy proxy does not trigger a load of the competition
                .competitionId(competitionRound.getCompetition().getId())
                .createdOn(competitionRound.getCreatedOn())
                .build();
    }
}
