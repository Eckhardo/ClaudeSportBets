package org.ebi.sportbets.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.UmsInfoDto;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.CommunityRole;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.CompetitionRole;
import org.ebi.sportbets.entity.Role;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.enums.Country;
import org.ebi.sportbets.exception.AuthenticationFailedException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.ebi.sportbets.repository.TipperRoleRepository;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // Same message for unknown username and wrong passwort, so existing usernames cannot be probed
    private static final String INVALID_CREDENTIALS = "Invalid username or passwort";

    private static final Country FALLBACK_COUNTRY = Country.GERMANY;

    private final TipperRepository tipperRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final CompetitionMembershipRepository competitionMembershipRepository;
    private final TipperRoleRepository tipperRoleRepository;
    private final CompetitionRepository competitionRepository;
    private final PasswordEncoder passwordEncoder;

    // Not read-only: a legacy plain text passwort is re-encoded on a successful login
    @Transactional
    public UmsInfoDto login(LoginRequestDto loginRequest) {
        Tipper tipper = tipperRepository.findByUsername(loginRequest.getUserName())
                .filter(candidate -> candidate.getPasswort() != null
                        && passwordEncoder.matches(loginRequest.getPassword(), candidate.getPasswort()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        if (passwordEncoder.upgradeEncoding(tipper.getPasswort())) {
            // Managed entity: the new hash is flushed on commit, no save() needed
            tipper.setPasswort(passwordEncoder.encode(loginRequest.getPassword()));
        }
        return toUmsInfo(tipper);
    }

    private UmsInfoDto toUmsInfo(Tipper tipper) {
        List<CommunityMembership> memberships = communityMembershipRepository.findByTipperId(tipper.getId());
        Long defaultCommunityId = defaultCommunityId(tipper, memberships);
        UmsInfoDto umsInfo = UmsInfoDto.builder()
                .userId(tipper.getId())
                .username(tipper.getUsername())
                .loggedIn(true)
                .commAdmin(tipper.isCommunityAdmin())
                .compAdmin(tipper.isCompetitionAdmin())
                .defaultCommunityId(defaultCommunityId)
                .defaultCompetitionId(tipper.getDefaultCompetitionId())
                .defaultCommMembId(defaultCommMembId(memberships, defaultCommunityId))
                .defaultCompMembId(defaultCompMembId(defaultCommunityId, tipper.getDefaultCompetitionId()))
                .defaultCountry(defaultCountry(tipper.getDefaultCompetitionId()))
                .build();
        // getId() on the lazy proxies does not trigger a load of the communities
        memberships.forEach(membership -> umsInfo.getTipperCommunities().add(membership.getCommunity().getId()));
        addAdminRights(umsInfo, tipper.getId());
        log.info("umsInfo: {}", umsInfo);
        return umsInfo;
    }

    // Without a stored default, the community of the first membership is used; the tipper itself is not changed
    private static Long defaultCommunityId(Tipper tipper, List<CommunityMembership> memberships) {
        if (tipper.getDefaultCommunityId() != null || memberships.isEmpty()) {
            return tipper.getDefaultCommunityId();
        }
        return memberships.getFirst().getCommunity().getId();
    }

    // The tipper's own membership in the default community; null if the tipper is not (or no longer) a member
    private static Long defaultCommMembId(List<CommunityMembership> memberships, Long defaultCommunityId) {
        if (defaultCommunityId == null) {
            return null;
        }
        return memberships.stream()
                .filter(membership -> defaultCommunityId.equals(membership.getCommunity().getId()))
                .map(CommunityMembership::getId)
                .findFirst()
                .orElse(null);
    }

    // The default community's participation in the default competition; null if it does not take part
    private Long defaultCompMembId(Long defaultCommunityId, Long defaultCompetitionId) {
        if (defaultCommunityId == null || defaultCompetitionId == null) {
            return null;
        }
        return competitionMembershipRepository.findByCommunityIdAndCompetitionId(defaultCommunityId, defaultCompetitionId)
                .map(CompetitionMembership::getId)
                .orElse(null);
    }

    // default_competition_id has no FK, so a missing competition or an unknown country code falls back instead of failing the login
    private Country defaultCountry(Long defaultCompetitionId) {
        if (defaultCompetitionId == null) {
            return FALLBACK_COUNTRY;
        }
        return competitionRepository.findById(defaultCompetitionId)
                .map(competition -> Country.fromIsoCode(competition.getCompetitionFamily().getCountry()))
                .orElse(FALLBACK_COUNTRY);
    }

    private void addAdminRights(UmsInfoDto umsInfo, Long tipperId) {
        for (TipperRole tipperRole : tipperRoleRepository.findWithRoleByTipperId(tipperId)) {
            // unproxy, because a proxy of the abstract Role would never match a subclass
            Role role = (Role) Hibernate.unproxy(tipperRole.getRole());
            switch (role) {
                case CommunityRole communityRole when communityRole.getCommunity() != null ->
                        umsInfo.getAdminCommunities().add(communityRole.getCommunity().getId());
                case CompetitionRole competitionRole when competitionRole.getCompetition() != null ->
                        umsInfo.getAdminCompetitions().add(competitionRole.getCompetition().getId());
                // The FK columns are nullable in the legacy schema; such a role grants nothing
                case CommunityRole _, CompetitionRole _ -> { }
                default -> throw new IllegalStateException("Unknown role type " + role.getClass().getSimpleName());
            }
        }
    }
}
