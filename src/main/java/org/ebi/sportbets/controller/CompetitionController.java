package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.service.CompetitionService;
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
@RequestMapping("/v1/competitions")
class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<List<CompetitionDto>> get() {
        List<CompetitionDto> competitions = competitionService.findAll();
        log.info("get comps {}", competitions.size());
        return ResponseEntity.ok(competitions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitionDto> create(@Valid @RequestBody CompetitionDto competitionDto) {
        CompetitionDto created = competitionService.create(competitionDto);
        log.info("created comp {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody CompetitionDto competitionDto) {
        CompetitionDto updated = competitionService.update(id, competitionDto);
        log.info("updated comp {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitionService.delete(id);
        log.info("deleted comp {}", id);
        return ResponseEntity.noContent().build();
    }
}
