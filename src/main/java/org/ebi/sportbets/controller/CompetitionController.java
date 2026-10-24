package org.ebi.sportbets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.service.CompetitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/competitions")
class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<List<CompetitionDto>> get() {
        List<CompetitionDto> competitions = competitionService.findAll();
        log.info("get comps {}", competitions.size());
        return ResponseEntity.ok(competitions);
    }

    @PostMapping
    public ResponseEntity<CompetitionDto> create(@Valid @RequestBody CompetitionDto competitionDto) {
        CompetitionDto created = competitionService.create(competitionDto);
        log.info("created comp {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
