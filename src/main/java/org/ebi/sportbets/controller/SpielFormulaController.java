package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.SpielFormulaDto;
import org.ebi.sportbets.service.SpielFormulaService;
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
@RequestMapping("/v1/spiel-formulas")
class SpielFormulaController {

    private final SpielFormulaService spielFormulaService;

    @GetMapping
    public ResponseEntity<List<SpielFormulaDto>> get(@RequestParam(required = false) Long spielId) {
        List<SpielFormulaDto> spielFormulas = spielId == null
                ? spielFormulaService.findAll()
                : spielFormulaService.findBySpielId(spielId);
        log.info("get spiel formulas {}", spielFormulas.size());
        return ResponseEntity.ok(spielFormulas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpielFormulaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(spielFormulaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SpielFormulaDto> create(@Valid @RequestBody SpielFormulaDto spielFormulaDto) {
        SpielFormulaDto created = spielFormulaService.create(spielFormulaDto);
        log.info("created spiel formula {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpielFormulaDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody SpielFormulaDto spielFormulaDto) {
        SpielFormulaDto updated = spielFormulaService.update(id, spielFormulaDto);
        log.info("updated spiel formula {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        spielFormulaService.delete(id);
        log.info("deleted spiel formula {}", id);
        return ResponseEntity.noContent().build();
    }
}
