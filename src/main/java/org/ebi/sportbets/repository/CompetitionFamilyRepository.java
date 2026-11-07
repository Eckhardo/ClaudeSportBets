package org.ebi.sportbets.repository;

import io.micrometer.core.instrument.config.MeterFilter;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CompetitionFamilyRepository extends JpaRepository<CompetitionFamily, Long> {

    Optional<CompetitionFamily> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(" select cf from CompetitionFamily cf join cf.competitions c  "
            + " where  c.id=:id ")
    Optional<CompetitionFamily> findByCompetitionId(Long id);
}
