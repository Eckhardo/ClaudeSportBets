package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.TippConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TippConfigRepository extends JpaRepository<TippConfig, Long> {

    List<TippConfig> findByCompetitionMembershipId(Long competitionMembershipId);

    Optional<TippConfig> findByCompetitionMembershipIdAndSpieltagId(Long competitionMembershipId, Long spieltagId);
}
