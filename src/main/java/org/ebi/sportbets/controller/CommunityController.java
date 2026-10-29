package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.service.CommunityService;
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
@RequestMapping("/v1/communities")
class CommunityController {

    private final CommunityService communityService;

    @GetMapping
    public ResponseEntity<List<CommunityDto>> get() {
        List<CommunityDto> communities = communityService.findAll();
        log.info("get communities {}", communities.size());
        return ResponseEntity.ok(communities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CommunityDto> create(@Valid @RequestBody CommunityDto communityDto) {
        CommunityDto created = communityService.create(communityDto);
        log.info("created community {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityDto> update(@PathVariable Long id,
                                               @Valid @RequestBody CommunityDto communityDto) {
        CommunityDto updated = communityService.update(id, communityDto);
        log.info("updated community {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        communityService.delete(id);
        log.info("deleted community {}", id);
        return ResponseEntity.noContent().build();
    }
}
