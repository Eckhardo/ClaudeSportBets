package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.TipperRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TipperRoleRepository extends JpaRepository<TipperRole, Long> {

    List<TipperRole> findByTipperId(Long tipperId);

    // Fetches the polymorphic role in the same query, so its concrete subclass is known without a lazy proxy
    @Query("select tr from TipperRole tr join fetch tr.role where tr.tipper.id = :tipperId")
    List<TipperRole> findWithRoleByTipperId(Long tipperId);

    Optional<TipperRole> findByTipperIdAndRoleId(Long tipperId, Long roleId);

    boolean existsByTipperIdAndRoleId(Long tipperId, Long roleId);
}
