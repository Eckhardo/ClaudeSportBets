package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.service.SpieltagService;
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
@RequestMapping("/v1/spieltage")
class SpieltagController {

    private final SpieltagService spieltagService;

    @GetMapping
    public ResponseEntity<List<SpieltagDto>> get(@RequestParam(required = false) Long competitionRoundId) {
        List<SpieltagDto> spieltage = competitionRoundId == null
                ? spieltagService.findAll()
                : spieltagService.findByCompetitionRoundId(competitionRoundId);
        log.info("get spieltage {}", spieltage.size());
        return ResponseEntity.ok(spieltage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpieltagDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(spieltagService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SpieltagDto> create(@Valid @RequestBody SpieltagDto spieltagDto) {
        SpieltagDto created = spieltagService.create(spieltagDto);
        log.info("created spieltag {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpieltagDto> update(@PathVariable Long id,
                                              @Valid @RequestBody SpieltagDto spieltagDto) {
        SpieltagDto updated = spieltagService.update(id, spieltagDto);
        log.info("updated spieltag {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        spieltagService.delete(id);
        log.info("deleted spieltag {}", id);
        return ResponseEntity.noContent().build();
    }
}
