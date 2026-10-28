package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import org.ebi.sportbets.dto.TeamDto;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionTeamRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final SpielRepository spielRepository;
    private final CompetitionTeamRepository competitionTeamRepository;

    public List<TeamDto> findAll() {
        return teamRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public TeamDto findById(Long id) {
        return teamRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    @Transactional
    public TeamDto create(TeamDto teamDto) {
        if (teamRepository.existsByName(teamDto.getName())) {
            throw duplicate("name", teamDto.getName());
        }
        if (teamRepository.existsByAcronym(teamDto.getAcronym())) {
            throw duplicate("acronym", teamDto.getAcronym());
        }
        // id and createdOn from the request are ignored; both are generated on insert
        Team team = Team.builder()
                .name(teamDto.getName())
                .acronym(teamDto.getAcronym())
                .hasClub(teamDto.getHasClub())
                .build();
        return toDto(teamRepository.save(team));
    }

    @Transactional
    public TeamDto update(Long id, TeamDto teamDto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
        if (teamRepository.existsByNameAndIdNot(teamDto.getName(), id)) {
            throw duplicate("name", teamDto.getName());
        }
        if (teamRepository.existsByAcronymAndIdNot(teamDto.getAcronym(), id)) {
            throw duplicate("acronym", teamDto.getAcronym());
        }
        // The path id wins over any id in the body; createdOn is never changed
        team.setName(teamDto.getName());
        team.setAcronym(teamDto.getAcronym());
        team.setHasClub(teamDto.getHasClub());
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(team);
    }

    @Transactional
    public void delete(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
        // Teams are shared across competitions and not cascaded, so a referenced team must stay
        if (spielRepository.existsByHeimTeamIdOrGastTeamId(id, id)
                || competitionTeamRepository.existsByTeamId(id)) {
            throw new BusinessRuleException("Team with id " + id + " is still used by a Spiel or Competition");
        }
        teamRepository.delete(team);
    }

    private DuplicateResourceException duplicate(String field, String value) {
        return new DuplicateResourceException("Team with " + field + " " + value + " already exists");
    }

    private TeamDto toDto(Team team) {
        return TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .acronym(team.getAcronym())
                .hasClub(team.isHasClub())
                .createdOn(team.getCreatedOn())
                .build();
    }
}
