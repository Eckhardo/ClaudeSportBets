package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Optional<Community> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(" select c from CommunityMembership cm join cm.tipper t join cm.community c " +
            "where t.username=:username   ")
    List<Community> findCommunities(String username);
}
