package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.SpielDto;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionGroupRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
import org.ebi.sportbets.repository.TeamRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpielService {

    private final SpielRepository spielRepository;
    private final TeamRepository teamRepository;
    private final SpieltagRepository spieltagRepository;
    private final CompetitionGroupRepository competitionGroupRepository;

    public List<SpielDto> findAll() {
        return spielRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<SpielDto> findBySpieltagId(Long spieltagId) {
        return spielRepository.findBySpieltagIdOrderBySpielNumberAsc(spieltagId).stream()
                .map(this::toDto)
                .toList();
    }

    public SpielDto findById(Long id) {
        return spielRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Spiel", id));
    }

    @Transactional
    public SpielDto create(SpielDto spielDto) {
        ensureDifferentTeams(spielDto);
        // id and createdOn from the request are ignored; both are generated on insert
        Spiel spiel = Spiel.builder().build();
        applyChanges(spiel, spielDto);
        return toDto(spielRepository.save(spiel));
    }

    @Transactional
    public SpielDto update(Long id, SpielDto spielDto) {
        Spiel spiel = spielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spiel", id));
        ensureDifferentTeams(spielDto);
        // The path id wins over any id in the body; createdOn is never changed
        applyChanges(spiel, spielDto);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(spiel);
    }

    @Transactional
    public void delete(Long id) {
        Spiel spiel = spielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spiel", id));
        // Tipps and SpielFormulas are removed via JPA cascade
        spielRepository.delete(spiel);
    }

    private void ensureDifferentTeams(SpielDto spielDto) {
        if (spielDto.getHeimTeamId() != null && spielDto.getHeimTeamId().equals(spielDto.getGastTeamId())) {
            throw new BusinessRuleException("Heim team and Gast team must be different");
        }
    }

    private void applyChanges(Spiel spiel, SpielDto spielDto) {
        spiel.setSpielNumber(spielDto.getSpielNumber());
        spiel.setAnpfiffDate(spielDto.getAnpfiffDate());
        spiel.setHeimTore(spielDto.getHeimTore());
        spiel.setGastTore(spielDto.getGastTore());
        spiel.setStattgefunden(spielDto.getStattgefunden());
        // All references are optional: a null id clears the reference, an unknown id is a 404
        spiel.setHeimTeam(findOptional(teamRepository, "Team", spielDto.getHeimTeamId()));
        spiel.setGastTeam(findOptional(teamRepository, "Team", spielDto.getGastTeamId()));
        spiel.setSpieltag(findOptional(spieltagRepository, "Spieltag", spielDto.getSpieltagId()));
        spiel.setCompetitionGroup(findOptional(competitionGroupRepository, "CompetitionGroup", spielDto.getCompetitionGroupId()));
    }

    private <T> T findOptional(JpaRepository<T, Long> repository, String resourceName, Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, id));
    }

    private SpielDto toDto(Spiel spiel) {
        // getId() on the lazy proxies does not trigger a load of the referenced entities
        return SpielDto.builder()
                .id(spiel.getId())
                .spielNumber(spiel.getSpielNumber())
                .anpfiffDate(spiel.getAnpfiffDate())
                .heimTore(spiel.getHeimTore())
                .gastTore(spiel.getGastTore())
                .stattgefunden(spiel.isStattgefunden())
                .heimTeamId(spiel.getHeimTeam() == null ? null : spiel.getHeimTeam().getId())
                .gastTeamId(spiel.getGastTeam() == null ? null : spiel.getGastTeam().getId())
                .spieltagId(spiel.getSpieltag() == null ? null : spiel.getSpieltag().getId())
                .competitionGroupId(spiel.getCompetitionGroup() == null ? null : spiel.getCompetitionGroup().getId())
                .createdOn(spiel.getCreatedOn())
                .build();
    }
}
