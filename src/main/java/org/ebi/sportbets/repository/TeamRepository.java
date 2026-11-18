package org.ebi.sportbets.repository;

import jakarta.persistence.QueryHint;
import org.ebi.sportbets.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);

    Optional<Team> findByAcronym(String acronym);

    boolean existsByName(String name);

    boolean existsByAcronym(String acronym);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByAcronymAndIdNot(String acronym, Long id);

    @Query(" select t from Team t where t.hasClub=true")
    List<Team> findAllClubTeams();

    @Query(" select t from Team t where t.hasClub=false")
    List<Team> findAllNationTeams();

}
