package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.CompetitionFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionFamilyRepository extends JpaRepository<CompetitionFamily, Long> {

    Optional<CompetitionFamily> findByName(String name);

    boolean existsByName(String name);
}
