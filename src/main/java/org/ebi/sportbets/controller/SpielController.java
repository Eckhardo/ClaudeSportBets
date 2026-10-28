package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.SpielDto;
import org.ebi.sportbets.service.SpielService;
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
@RequestMapping("/v1/spiele")
class SpielController {

    private final SpielService spielService;

    @GetMapping
    public ResponseEntity<List<SpielDto>> get(@RequestParam(required = false) Long spieltagId) {
        List<SpielDto> spiele = spieltagId == null
                ? spielService.findAll()
                : spielService.findBySpieltagId(spieltagId);
        log.info("get spiele {}", spiele.size());
        return ResponseEntity.ok(spiele);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpielDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(spielService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SpielDto> create(@Valid @RequestBody SpielDto spielDto) {
        SpielDto created = spielService.create(spielDto);
        log.info("created spiel {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpielDto> update(@PathVariable Long id,
                                           @Valid @RequestBody SpielDto spielDto) {
        SpielDto updated = spielService.update(id, spielDto);
        log.info("updated spiel {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        spielService.delete(id);
        log.info("deleted spiel {}", id);
        return ResponseEntity.noContent().build();
    }
}
