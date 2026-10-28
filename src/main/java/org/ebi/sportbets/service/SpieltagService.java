package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpieltagService {

    private final SpieltagRepository spieltagRepository;
    private final CompetitionRoundRepository competitionRoundRepository;

    public List<SpieltagDto> findAll() {
        return spieltagRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<SpieltagDto> findByCompetitionRoundId(Long competitionRoundId) {
        return spieltagRepository.findByCompetitionRoundIdOrderBySpieltagNumberAsc(competitionRoundId).stream()
                .map(this::toDto)
                .toList();
    }

    public SpieltagDto findById(Long id) {
        return spieltagRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Spieltag", id));
    }

    @Transactional
    public SpieltagDto create(SpieltagDto spieltagDto) {
        CompetitionRound competitionRound = findCompetitionRound(spieltagDto.getCompetitionRoundId());
        ensureSpieltagNumberIsFree(competitionRound.getId(), spieltagDto.getSpieltagNumber(), null);
        // id and createdOn from the request are ignored; both are generated on insert
        Spieltag spieltag = Spieltag.builder()
                .spieltagNumber(spieltagDto.getSpieltagNumber())
                .startDate(spieltagDto.getStartDate())
                .competitionRound(competitionRound)
                .build();
        return toDto(spieltagRepository.save(spieltag));
    }

    @Transactional
    public SpieltagDto update(Long id, SpieltagDto spieltagDto) {
        Spieltag spieltag = spieltagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spieltag", id));
        CompetitionRound competitionRound = findCompetitionRound(spieltagDto.getCompetitionRoundId());
        ensureSpieltagNumberIsFree(competitionRound.getId(), spieltagDto.getSpieltagNumber(), id);
        // The path id wins over any id in the body; createdOn is never changed
        spieltag.setSpieltagNumber(spieltagDto.getSpieltagNumber());
        spieltag.setStartDate(spieltagDto.getStartDate());
        spieltag.setCompetitionRound(competitionRound);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(spieltag);
    }

    @Transactional
    public void delete(Long id) {
        Spieltag spieltag = spieltagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spieltag", id));
        // Spiele and TippConfigs (and everything below them) are removed via JPA cascade
        spieltagRepository.delete(spieltag);
    }

    private CompetitionRound findCompetitionRound(Long competitionRoundId) {
        return competitionRoundRepository.findById(competitionRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionRound", competitionRoundId));
    }

    // A Spieltag number may only be used once per round; ownId excludes the Spieltag being updated
    private void ensureSpieltagNumberIsFree(Long competitionRoundId, int spieltagNumber, Long ownId) {
        spieltagRepository.findByCompetitionRoundIdAndSpieltagNumber(competitionRoundId, spieltagNumber)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Spieltag with number " + spieltagNumber
                            + " already exists for CompetitionRound " + competitionRoundId);
                });
    }

    private SpieltagDto toDto(Spieltag spieltag) {
        return SpieltagDto.builder()
                .id(spieltag.getId())
                .spieltagNumber(spieltag.getSpieltagNumber())
                .startDate(spieltag.getStartDate())
                // getId() on the lazy proxy does not trigger a load of the round
                .competitionRoundId(spieltag.getCompetitionRound().getId())
                .createdOn(spieltag.getCreatedOn())
                .build();
    }
}
