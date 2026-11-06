package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionMembershipDto;
import org.ebi.sportbets.service.CompetitionMembershipService;
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
@RequestMapping("/v1/compMembs")
class CompetitionMembershipController {

    private final CompetitionMembershipService competitionMembershipService;

    @GetMapping
    public ResponseEntity<List<CompetitionMembershipDto>> get(@RequestParam(required = false) Long communityId,
                                                              @RequestParam(required = false) Long competitionId) {
        List<CompetitionMembershipDto> memberships = competitionMembershipService.find(communityId, competitionId);
        log.info("get competition memberships {}", memberships.size());
        return ResponseEntity.ok(memberships);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionMembershipDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionMembershipService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitionMembershipDto> create(@Valid @RequestBody CompetitionMembershipDto membershipDto) {
        CompetitionMembershipDto created = competitionMembershipService.create(membershipDto);
        log.info("created competition membership {}", created.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionMembershipDto> update(@PathVariable Long id,
                                                           @Valid @RequestBody CompetitionMembershipDto membershipDto) {
        CompetitionMembershipDto updated = competitionMembershipService.update(id, membershipDto);
        log.info("updated competition membership {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitionMembershipService.delete(id);
        log.info("deleted competition membership {}", id);
        return ResponseEntity.noContent().build();
    }
}
