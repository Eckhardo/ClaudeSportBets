package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionTeam;
import org.ebi.sportbets.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {


    List<CompetitionTeam> findByTeamId(Long teamId);

    List<CompetitionTeam> findByCompetitionId(Long competitionId);

    Optional<CompetitionTeam> findByTeamIdAndCompetitionId(Long teamId, Long competitionId);

    boolean existsByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    boolean existsByCompetitionId(Long competitionId);

    boolean existsByTeamId(Long teamId);

    @Query("""
                SELECT t FROM Team t 
                WHERE t.hasClub=:hasClubs AND NOT EXISTS (
                    SELECT ct FROM CompetitionTeam ct 
                    WHERE ct.team = t  AND ct.competition.id = :compId
                )
            """)
    List<Team> findUnregisteredTeams(Long compId, boolean hasClubs);


}
