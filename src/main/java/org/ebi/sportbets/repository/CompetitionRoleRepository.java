package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRoleRepository extends JpaRepository<CompetitionRole, Long> {

    boolean existsByCompetitionId(Long competitionId);
}
