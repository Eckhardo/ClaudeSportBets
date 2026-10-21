package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Spieltag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpieltagRepository extends JpaRepository<Spieltag, Long> {

    List<Spieltag> findByCompetitionRoundIdOrderBySpieltagNumberAsc(Long competitionRoundId);

    Optional<Spieltag> findByCompetitionRoundIdAndSpieltagNumber(Long competitionRoundId, int spieltagNumber);
}
