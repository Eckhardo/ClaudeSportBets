package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.Tipper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipperRepository extends JpaRepository<Tipper, Long> {

    Optional<Tipper> findByUsername(String username);

    boolean existsByUsername(String username);
}
