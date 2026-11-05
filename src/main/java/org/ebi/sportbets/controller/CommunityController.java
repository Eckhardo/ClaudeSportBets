package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CommunityDto;
import org.ebi.sportbets.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

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
    @GetMapping("/{username}/communities")
    public ResponseEntity<List<CommunityDto>> findTipperCommunities(@PathVariable String username) {
        log.debug(":find communities for tipper {}", username);
        List<CommunityDto> commDtos = communityService.findCommunities(username);
        log.info("get commDtos {}", commDtos.size());
        return ResponseEntity.ok(commDtos);

    }
    @GetMapping("/search")
    public  ResponseEntity<CommunityDto>  findCommunityByName(@RequestParam String name) {
        log.debug(":find community for url {}", name);
        return ResponseEntity.ok(communityService.findByName(name));

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
