package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.TippModusDto;
import org.ebi.sportbets.service.TippModusService;
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
@RequestMapping("/v1/tipp-modi")
class TippModusController {

    private final TippModusService tippModusService;

    @GetMapping
    public ResponseEntity<List<TippModusDto>> get(@RequestParam(required = false) Long communityId) {
        List<TippModusDto> tippModi = tippModusService.find(communityId);
        log.info("get tipp modi {}", tippModi.size());
        return ResponseEntity.ok(tippModi);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TippModusDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tippModusService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TippModusDto> create(@Valid @RequestBody TippModusDto tippModusDto) {
        TippModusDto created = tippModusService.create(tippModusDto);
        log.info("created tipp modus {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TippModusDto> update(@PathVariable Long id,
                                               @Valid @RequestBody TippModusDto tippModusDto) {
        TippModusDto updated = tippModusService.update(id, tippModusDto);
        log.info("updated tipp modus {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tippModusService.delete(id);
        log.info("deleted tipp modus {}", id);
        return ResponseEntity.noContent().build();
    }
}
