package org.ebi.sportbets.repository;

import org.ebi.sportbets.entity.SpielFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpielFormulaRepository extends JpaRepository<SpielFormula, Long> {

    List<SpielFormula> findBySpielId(Long spielId);
}
