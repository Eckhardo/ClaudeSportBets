package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionMembershipRepository extends JpaRepository<CompetitionMembership, Long> {

    List<CompetitionMembership> findByCommunityId(Long communityId);

    List<CompetitionMembership> findByCompetitionId(Long competitionId);

    Optional<CompetitionMembership> findByCommunityIdAndCompetitionId(Long communityId, Long competitionId);

    boolean existsByCommunityIdAndCompetitionId(Long communityId, Long competitionId);

    boolean existsByCompetitionId(Long competitionId);
}
