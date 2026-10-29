package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.TipperDto;
import org.ebi.sportbets.service.TipperService;
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
@RequestMapping("/v1/tippers")
class TipperController {

    private final TipperService tipperService;

    @GetMapping
    public ResponseEntity<List<TipperDto>> get() {
        List<TipperDto> tippers = tipperService.findAll();
        log.info("get tippers {}", tippers.size());
        return ResponseEntity.ok(tippers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipperDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tipperService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TipperDto> create(@Valid @RequestBody TipperDto tipperDto) {
        TipperDto created = tipperService.create(tipperDto);
        log.info("created tipper {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipperDto> update(@PathVariable Long id,
                                            @Valid @RequestBody TipperDto tipperDto) {
        TipperDto updated = tipperService.update(id, tipperDto);
        log.info("updated tipper {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tipperService.delete(id);
        log.info("deleted tipper {}", id);
        return ResponseEntity.noContent().build();
    }
}
