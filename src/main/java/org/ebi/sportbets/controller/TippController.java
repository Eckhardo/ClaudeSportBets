package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.TippDto;
import org.ebi.sportbets.service.TippService;
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
@RequestMapping("/v1/tipps")
class TippController {

    private final TippService tippService;

    @GetMapping
    public ResponseEntity<List<TippDto>> get(@RequestParam(required = false) Long communityMembershipId,
                                             @RequestParam(required = false) Long spielId) {
        List<TippDto> tipps = tippService.find(communityMembershipId, spielId);
        log.info("get tipps {}", tipps.size());
        return ResponseEntity.ok(tipps);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TippDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tippService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TippDto> create(@Valid @RequestBody TippDto tippDto) {
        TippDto created = tippService.create(tippDto);
        log.info("created tipp {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping
    public ResponseEntity<List<TippDto>> saveAll(@RequestBody @NotEmpty List<@Valid TippDto> tippDtos) {
        List<TippDto> saved = tippService.saveAll(tippDtos);
        log.info("saved tipps {}", saved.size());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TippDto> update(@PathVariable Long id,
                                          @Valid @RequestBody TippDto tippDto) {
        TippDto updated = tippService.update(id, tippDto);
        log.info("updated tipp {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tippService.delete(id);
        log.info("deleted tipp {}", id);
        return ResponseEntity.noContent().build();
    }
}
