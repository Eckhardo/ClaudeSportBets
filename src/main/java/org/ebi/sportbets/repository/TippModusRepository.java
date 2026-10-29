package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.TippModus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TippModusRepository extends JpaRepository<TippModus, Long> {

    List<TippModus> findByCommunityId(Long communityId);

    List<TippModus> findByCommunityIdAndName(Long communityId, String name);
}
