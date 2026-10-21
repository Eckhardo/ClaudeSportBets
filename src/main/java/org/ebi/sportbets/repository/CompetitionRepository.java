package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    List<Competition> findByCompetitionFamilyId(Long competitionFamilyId);
}
