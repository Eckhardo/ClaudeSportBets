package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionFamilyDto;
import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.service.CompetitionFamilyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/families")
class CompetitionFamilyController {

    private final CompetitionFamilyService competitionFamilyService;

    @GetMapping
    public ResponseEntity<List<CompetitionFamilyDto>> get() {
        List<CompetitionFamilyDto> families = competitionFamilyService.findAll();
        log.info("get comp families {}", families.size());
        return ResponseEntity.ok(families);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionFamilyDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionFamilyService.findById(id));
    }
    @GetMapping(params = "competitionId")
    public ResponseEntity<CompetitionFamilyDto> findByCompId(@RequestParam() Long competitionId) {

        CompetitionFamilyDto family = competitionFamilyService.findByByCompetitionId(competitionId);
        log.info("get family");
        return ResponseEntity.ok(family);
    }
    @PostMapping
    public ResponseEntity<CompetitionFamilyDto> create(@Valid @RequestBody CompetitionFamilyDto competitionFamilyDto) {
        CompetitionFamilyDto created = competitionFamilyService.create(competitionFamilyDto);
        log.info("created comp family {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionFamilyDto> update(@PathVariable Long id,
                                                       @Valid @RequestBody CompetitionFamilyDto competitionFamilyDto) {
        CompetitionFamilyDto updated = competitionFamilyService.update(id, competitionFamilyDto);
        log.info("updated comp family {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitionFamilyService.delete(id);
        log.info("deleted comp family {}", id);
        return ResponseEntity.noContent().build();
    }
}
