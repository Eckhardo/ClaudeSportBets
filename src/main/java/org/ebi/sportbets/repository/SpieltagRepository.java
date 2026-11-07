package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Spieltag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpieltagRepository extends JpaRepository<Spieltag, Long> {

    List<Spieltag> findByCompetitionRoundIdOrderBySpieltagNumberAsc(Long competitionRoundId);

    Optional<Spieltag> findByCompetitionRoundIdAndSpieltagNumber(Long competitionRoundId, int spieltagNumber);

    @Query("select sp from Spieltag sp join  sp.competitionRound cr" +
            " join  cr.competition c"
            + " where  c.id=:id order by sp.spieltagNumber")
    List<Spieltag> findByCompetitionIdOrderBySpieltagNumberAsc(Long id);
}

