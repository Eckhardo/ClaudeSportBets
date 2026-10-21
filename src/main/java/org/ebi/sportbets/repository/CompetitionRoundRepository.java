package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionRoundRepository extends JpaRepository<CompetitionRound, Long> {

    List<CompetitionRound> findByCompetitionIdOrderByRoundNumberAsc(Long competitionId);

    Optional<CompetitionRound> findByCompetitionIdAndRoundNumber(Long competitionId, int roundNumber);
}
