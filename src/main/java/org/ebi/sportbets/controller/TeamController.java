package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.TeamDto;
import org.ebi.sportbets.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/teams")
class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamDto>> get() {
        List<TeamDto> teams = teamService.findAll();
        log.info("get teams {}", teams.size());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TeamDto> create(@Valid @RequestBody TeamDto teamDto) {
        TeamDto created = teamService.create(teamDto);
        log.info("created team {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDto> update(@PathVariable Long id,
                                          @Valid @RequestBody TeamDto teamDto) {
        TeamDto updated = teamService.update(id, teamDto);
        log.info("updated team {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        log.info("deleted team {}", id);
        return ResponseEntity.noContent().build();
    }
}
