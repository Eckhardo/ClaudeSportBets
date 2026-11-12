package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.CompetitionTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {


    List<CompetitionTeam> findByTeamId(Long teamId);


    List<CompetitionTeam> findByCompetitionId(Long competitionId);
    Optional<CompetitionTeam> findByTeamIdAndCompetitionId(Long teamId, Long competitionId);

    boolean existsByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    boolean existsByCompetitionId(Long competitionId);

    boolean existsByTeamId(Long teamId);
}
