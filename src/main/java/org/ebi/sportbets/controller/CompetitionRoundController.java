package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionRoundDto;
import org.ebi.sportbets.service.CompetitionRoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/competition-rounds")
class CompetitionRoundController {

    private final CompetitionRoundService competitionRoundService;

    @GetMapping
    public ResponseEntity<List<CompetitionRoundDto>> get(@RequestParam(required = false) Long competitionId) {
        List<CompetitionRoundDto> rounds = competitionId == null
                ? competitionRoundService.findAll()
                : competitionRoundService.findByCompetitionId(competitionId);
        log.info("get comp rounds {}", rounds.size());
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionRoundDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionRoundService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitionRoundDto> create(@Valid @RequestBody CompetitionRoundDto competitionRoundDto) {
        CompetitionRoundDto created = competitionRoundService.create(competitionRoundDto);
        log.info("created comp round {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionRoundDto> update(@PathVariable Long id,
                                                      @Valid @RequestBody CompetitionRoundDto competitionRoundDto) {
        CompetitionRoundDto updated = competitionRoundService.update(id, competitionRoundDto);
        log.info("updated comp round {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitionRoundService.delete(id);
        log.info("deleted comp round {}", id);
        return ResponseEntity.noContent().build();
    }
}
