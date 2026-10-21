package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionGroupRepository extends JpaRepository<CompetitionGroup, Long> {

    List<CompetitionGroup> findByCompetitionRoundIdOrderByGroupNumberAsc(Long competitionRoundId);
}
