package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.TipperRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipperRoleRepository extends JpaRepository<TipperRole, Long> {

    List<TipperRole> findByTipperId(Long tipperId);

    boolean existsByTipperIdAndRoleId(Long tipperId, Long roleId);
}
