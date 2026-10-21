package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);

    Optional<Team> findByAcronym(String acronym);

    boolean existsByName(String name);

    boolean existsByAcronym(String acronym);
}
