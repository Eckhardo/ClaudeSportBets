package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CommunityMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, Long> {

    List<CommunityMembership> findByCommunityId(Long communityId);

    List<CommunityMembership> findByTipperId(Long tipperId);

    Optional<CommunityMembership> findByCommunityIdAndTipperId(Long communityId, Long tipperId);

    boolean existsByCommunityIdAndTipperId(Long communityId, Long tipperId);
}
