package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Tipp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TippRepository extends JpaRepository<Tipp, Long> {

    List<Tipp> findByCommunityMembershipId(Long communityMembershipId);

    List<Tipp> findBySpielId(Long spielId);

    List<Tipp> findByCommunityMembershipIdAndSpielId(Long communityMembershipId, Long spielId);

    Optional<Tipp> findByCommunityMembershipIdAndSpielIdAndTippModusId(Long communityMembershipId, Long spielId, Long tippModusId);
}
