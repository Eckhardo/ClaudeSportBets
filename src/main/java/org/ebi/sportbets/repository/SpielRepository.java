package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Spiel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpielRepository extends JpaRepository<Spiel, Long> {

    List<Spiel> findBySpieltagIdOrderBySpielNumberAsc(Long spieltagId);

    List<Spiel> findByCompetitionGroupId(Long competitionGroupId);

    boolean existsByHeimTeamIdOrGastTeamId(Long heimTeamId, Long gastTeamId);
}
