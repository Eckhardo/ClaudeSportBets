package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.SpielFormulaDto;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.SpielFormula;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.SpielFormulaRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpielFormulaService {

    private final SpielFormulaRepository spielFormulaRepository;
    private final SpielRepository spielRepository;

    public List<SpielFormulaDto> findAll() {
        return spielFormulaRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<SpielFormulaDto> findBySpielId(Long spielId) {
        return spielFormulaRepository.findBySpielId(spielId).stream()
                .map(this::toDto)
                .toList();
    }

    public SpielFormulaDto findById(Long id) {
        return spielFormulaRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SpielFormula", id));
    }

    @Transactional
    public SpielFormulaDto create(SpielFormulaDto spielFormulaDto) {
        // id and createdOn from the request are ignored; both are generated on insert
        SpielFormula spielFormula = SpielFormula.builder().build();
        applyChanges(spielFormula, spielFormulaDto);
        return toDto(spielFormulaRepository.save(spielFormula));
    }

    @Transactional
    public SpielFormulaDto update(Long id, SpielFormulaDto spielFormulaDto) {
        SpielFormula spielFormula = spielFormulaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpielFormula", id));
        // The path id wins over any id in the body; createdOn is never changed
        applyChanges(spielFormula, spielFormulaDto);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(spielFormula);
    }

    @Transactional
    public void delete(Long id) {
        SpielFormula spielFormula = spielFormulaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpielFormula", id));
        spielFormulaRepository.delete(spielFormula);
    }

    private void applyChanges(SpielFormula spielFormula, SpielFormulaDto spielFormulaDto) {
        Spiel spiel = spielRepository.findById(spielFormulaDto.getSpielId())
                .orElseThrow(() -> new ResourceNotFoundException("Spiel", spielFormulaDto.getSpielId()));
        spielFormula.setSpiel(spiel);
        spielFormula.setTeamName(spielFormulaDto.getTeamName());
        spielFormula.setTeamNameAcronym(spielFormulaDto.getTeamNameAcronym());
        spielFormula.setHeimTeam(spielFormulaDto.getHeimTeam());
        spielFormula.setHeimTore(spielFormulaDto.getHeimTore());
        spielFormula.setGastTore(spielFormulaDto.getGastTore());
        spielFormula.setDiffTore(spielFormulaDto.getDiffTore());
        spielFormula.setWon(spielFormulaDto.getWon());
        spielFormula.setRemis(spielFormulaDto.getRemis());
        spielFormula.setLost(spielFormulaDto.getLost());
        spielFormula.setPoints(spielFormulaDto.getPoints());
    }

    private SpielFormulaDto toDto(SpielFormula spielFormula) {
        return SpielFormulaDto.builder()
                .id(spielFormula.getId())
                // getId() on the lazy proxy does not trigger a load of the Spiel
                .spielId(spielFormula.getSpiel().getId())
                .teamName(spielFormula.getTeamName())
                .teamNameAcronym(spielFormula.getTeamNameAcronym())
                .heimTeam(spielFormula.isHeimTeam())
                .heimTore(spielFormula.getHeimTore())
                .gastTore(spielFormula.getGastTore())
                .diffTore(spielFormula.getDiffTore())
                .won(spielFormula.getWon())
                .remis(spielFormula.getRemis())
                .lost(spielFormula.getLost())
                .points(spielFormula.getPoints())
                .createdOn(spielFormula.getCreatedOn())
                .build();
    }
}
