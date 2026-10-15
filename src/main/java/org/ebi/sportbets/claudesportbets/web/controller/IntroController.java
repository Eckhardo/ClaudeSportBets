package org.ebi.sportbets.claudesportbets.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/")
class IntroController {
    @GetMapping
    public ResponseEntity<?> get() {
        return  ResponseEntity.ok().build();

    }
}
