package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.CompetitionTeamDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionTeam;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionTeamRepository;
import org.ebi.sportbets.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionTeamService {

    private final CompetitionTeamRepository competitionTeamRepository;
    private final TeamRepository teamRepository;
    private final CompetitionRepository competitionRepository;

    // Both filters are optional; without any filter all memberships are returned
    public List<CompetitionTeamDto> find(Long teamId, Long competitionId) {
        List<CompetitionTeam> competitionTeams;
        if (teamId != null && competitionId != null) {
            competitionTeams = competitionTeamRepository.findByTeamIdAndCompetitionId(teamId, competitionId)
                    .stream().toList();
        } else if (teamId != null) {
            competitionTeams = competitionTeamRepository.findByTeamId(teamId);
        } else if (competitionId != null) {
            competitionTeams = competitionTeamRepository.findByCompetitionId(competitionId);
        } else {
            competitionTeams = competitionTeamRepository.findAll();
        }
        return competitionTeams.stream()
                .map(this::toDto)
                .toList();
    }

    public CompetitionTeamDto findById(Long id) {
        return competitionTeamRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionMembership", id));
    }

    @Transactional
    public CompetitionTeamDto create(CompetitionTeamDto membershipDto) {
        Team team = findTeam(membershipDto.getTeamId());
        Competition competition = findCompetition(membershipDto.getCompetitionId());
        ensureNotMember(team.getId(), competition.getId(), null);
        // id and createdOn from the request are ignored; both are generated on insert
        CompetitionTeam competitionTeam = CompetitionTeam.builder()
                .team(team)
                .competition(competition)
                .build();
        return toDto(competitionTeamRepository.save(competitionTeam));
    }

    @Transactional
    public CompetitionTeamDto update(Long id, CompetitionTeamDto competitionTeamDto) {
        CompetitionTeam competitionTeam = competitionTeamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionMembership", id));
        Team team = findTeam(competitionTeamDto.getTeamId());
        Competition competition = findCompetition(competitionTeamDto.getCompetitionId());

        ensureNotMember(team.getId(), competition.getId(), id);
        // The path id wins over any id in the body; createdOn is never changed
        competitionTeam.setTeam(team);
        competitionTeam.setCompetition(competition);
        // Managed entity: changes are flushed on commit, no save() needed
        return toDto(competitionTeam);
    }

    @Transactional
    public void delete(Long id) {
        CompetitionTeam membership = competitionTeamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompetitionTeam", id));
        // The TippConfigs of this membership are removed via JPA cascade
        competitionTeamRepository.delete(membership);
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    }

    private Competition findCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", competitionId));
    }

    // A Community may take part in a Competition only once; ownId excludes the membership being updated
    private void ensureNotMember(Long teamId, Long competitionId, Long ownId) {
        competitionTeamRepository.findByTeamIdAndCompetitionId(teamId, competitionId)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Team " + teamId
                            + " already takes part in Competition " + competitionId);
                });
    }

    private CompetitionTeamDto toDto(CompetitionTeam competitionTeam) {
        return CompetitionTeamDto.builder()
                .id(competitionTeam.getId())
                // getId() on the lazy proxies does not trigger a load of community or competition

                .teamAcronym(competitionTeam.getTeam().getAcronym())
                .teamId(competitionTeam.getTeam().getId())
                .competitionId(competitionTeam.getCompetition().getId())
                .createdOn(competitionTeam.getCreatedOn())
                .build();
    }

    public List<List<CompetitionTeamDto>> findAllRegisteredAndNonRegisteredTeams(Long compId) {
        Competition competition = findCompetition(compId);
        final boolean hasClubs = competition.getCompetitionFamily().isHasClubs();
        List<List<CompetitionTeamDto>> result = new ArrayList<>();
        List<CompetitionTeamDto> registeredCompTeams = competitionTeamRepository.findByCompetitionId(compId).stream().map(this::toDto).toList();
        log.info("Competition teams list found: {}", registeredCompTeams.size());
        result.add(registeredCompTeams);
        List<Team> unregisteredTeams = competitionTeamRepository.findUnregisteredTeams(compId, hasClubs);

        List<CompetitionTeamDto> unregDtos = unregisteredTeams.stream().map(team -> new CompetitionTeam(competition, team)).map(this::toDto).toList();
        log.info("Unregstered Competition teams list found: {}", unregDtos.size());
        result.add(unregDtos);
        return result;
    }

}
