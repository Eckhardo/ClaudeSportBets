package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CommunityMembershipDto;
import org.ebi.sportbets.service.CommunityMembershipService;
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
@RequestMapping("/v1/community-memberships")
class CommunityMembershipController {

    private final CommunityMembershipService communityMembershipService;

    @GetMapping
    public ResponseEntity<List<CommunityMembershipDto>> get(@RequestParam(required = false) Long communityId,
                                                            @RequestParam(required = false) Long tipperId) {
        List<CommunityMembershipDto> memberships = communityMembershipService.find(communityId, tipperId);
        log.info("get community memberships {}", memberships.size());
        return ResponseEntity.ok(memberships);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityMembershipDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(communityMembershipService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CommunityMembershipDto> create(@Valid @RequestBody CommunityMembershipDto membershipDto) {
        CommunityMembershipDto created = communityMembershipService.create(membershipDto);
        log.info("created community membership {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityMembershipDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody CommunityMembershipDto membershipDto) {
        CommunityMembershipDto updated = communityMembershipService.update(id, membershipDto);
        log.info("updated community membership {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        communityMembershipService.delete(id);
        log.info("deleted community membership {}", id);
        return ResponseEntity.noContent().build();
    }
}
