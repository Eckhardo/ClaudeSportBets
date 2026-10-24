package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {

    List<CompetitionTeam> findByCompetitionId(Long competitionId);

    boolean existsByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    boolean existsByCompetitionId(Long competitionId);
}
