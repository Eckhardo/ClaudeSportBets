package org.ebi.sportbets.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionTeamDto;

import org.ebi.sportbets.service.CompetitionTeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/compTeams")
public class CompetitionTeamController {
    private final CompetitionTeamService competitionTeamService;

    @GetMapping
    public ResponseEntity<List<CompetitionTeamDto>> get(@RequestParam(required = false) Long teamId,
                                                              @RequestParam(required = false) Long competitionId) {
        List<CompetitionTeamDto> memberships = competitionTeamService.find(teamId, competitionId);
        log.info("get competition memberships {}", memberships.size());
        return ResponseEntity.ok(memberships);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionTeamDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionTeamService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitionTeamDto> create(@Valid @RequestBody CompetitionTeamDto competitionTeamDto) {
        CompetitionTeamDto created = competitionTeamService.create(competitionTeamDto);
        log.info("created competition team {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionTeamDto> update(@PathVariable Long id,
                                                           @Valid @RequestBody CompetitionTeamDto membershipDto) {
        CompetitionTeamDto updated = competitionTeamService.update(id, membershipDto);
        log.info("updated competition membership {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitionTeamService.delete(id);
        log.info("deleted competition membership {}", id);
        return ResponseEntity.noContent().build();
    }
}
