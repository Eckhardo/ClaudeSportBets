package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Tipper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TipperRepository extends JpaRepository<Tipper, Long> {

    Optional<Tipper> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    // default_community_id has no FK, so it has to be cleared by hand when the community is deleted
    @Modifying
    @Query("update Tipper t set t.defaultCommunityId = null where t.defaultCommunityId = :communityId")
    int clearDefaultCommunity(Long communityId);
}
