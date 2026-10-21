package org.ebi.sportbets.controller;

import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static java.rmi.server.LogStream.log;

@Slf4j
@RestController
@RequestMapping("/")
class IntroController {

    private CompetitionRepository competitionRepository;
    @GetMapping
    public ResponseEntity<List<Competition>> get() {
  log.info("get comps");
        List<Competition> competitions = competitionRepository.findAll();
        log.info("get comps {}", competitions.size());
        return  ResponseEntity.ok().body(competitions);

    }
}
