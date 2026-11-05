package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.LoginRequestDto;
import org.ebi.sportbets.dto.UmsInfoDto;
import org.ebi.sportbets.entity.Community;
import org.ebi.sportbets.entity.CommunityMembership;
import org.ebi.sportbets.entity.CommunityRole;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.entity.CompetitionMembership;
import org.ebi.sportbets.entity.CompetitionRole;
import org.ebi.sportbets.entity.Tipper;
import org.ebi.sportbets.entity.TipperRole;
import org.ebi.sportbets.enums.Country;
import org.ebi.sportbets.exception.AuthenticationFailedException;
import org.ebi.sportbets.repository.CommunityMembershipRepository;
import org.ebi.sportbets.repository.CompetitionMembershipRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.TipperRepository;
import org.ebi.sportbets.repository.TipperRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TipperRepository tipperRepository;

    @Mock
    private CommunityMembershipRepository communityMembershipRepository;

    @Mock
    private CompetitionMembershipRepository competitionMembershipRepository;

    @Mock
    private TipperRoleRepository tipperRoleRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private AuthService authService;

    private static Tipper erika() {
        return Tipper.builder()
                .id(3L)
                .username("erika")
                .firstname("Erika")
                .lastname("Mustermann")
                .passwort("geheim")
                .communityAdmin(true)
                .competitionAdmin(false)
                .build();
    }

    // The membership id is derived from the community id (community 2 -> membership 20) to keep the tests readable
    private static CommunityMembership membershipIn(Long communityId) {
        return CommunityMembership.builder()
                .id(communityId * 10)
                .community(Community.builder().id(communityId).build())
                .build();
    }

    private static LoginRequestDto login(String username, String passwort) {
        return LoginRequestDto.builder().username(username).passwort(passwort).build();
    }

    @Test
    void should_returnUmsInfoWithTipperData_when_credentialsAreValid() {
        // Arrange
        Tipper tipper = erika();
        tipper.setDefaultCommunityId(2L);
        tipper.setDefaultCompetitionId(4L);
        Competition competition = Competition.builder()
                .id(4L)
                .competitionFamily(CompetitionFamily.builder().country("EN").build())
                .build();
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of(membershipIn(2L), membershipIn(6L)));
        given(tipperRoleRepository.findWithRoleByTipperId(3L)).willReturn(List.of());
        given(competitionRepository.findById(4L)).willReturn(Optional.of(competition));
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L))
                .willReturn(Optional.of(CompetitionMembership.builder().id(40L).build()));

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result).isEqualTo(UmsInfoDto.builder()
                .userId(3L)
                .username("erika")
                .loggedIn(true)
                .commAdmin(true)
                .compAdmin(false)
                .defaultCommunityId(2L)
                .defaultCompetitionId(4L)
                .defaultCommMembId(20L)
                .defaultCompMembId(40L)
                .defaultCountry(Country.ENGLAND)
                .tipperCommunities(Set.of(2L, 6L))
                .build());
    }

    @Test
    void should_useCommunityAndMembershipOfFirstMembership_when_tipperHasNoDefaultCommunity() {
        // Arrange
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(erika()));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of(membershipIn(6L), membershipIn(2L)));
        given(tipperRoleRepository.findWithRoleByTipperId(3L)).willReturn(List.of());

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result)
                .extracting(UmsInfoDto::getDefaultCommunityId, UmsInfoDto::getDefaultCommMembId)
                .containsExactly(6L, 60L);
    }

    @Test
    void should_leaveMembershipIdsNull_when_tipperIsNoMemberOfDefaultCommunity() {
        // Arrange
        Tipper tipper = erika();
        tipper.setDefaultCommunityId(2L);
        tipper.setDefaultCompetitionId(4L);
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of(membershipIn(6L)));
        given(tipperRoleRepository.findWithRoleByTipperId(3L)).willReturn(List.of());
        given(competitionRepository.findById(4L)).willReturn(Optional.empty());
        given(competitionMembershipRepository.findByCommunityIdAndCompetitionId(2L, 4L)).willReturn(Optional.empty());

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result)
                .extracting(UmsInfoDto::getDefaultCommMembId, UmsInfoDto::getDefaultCompMembId)
                .containsExactly(null, null);
    }

    @Test
    void should_collectAdminCommunitiesAndCompetitions_when_tipperHasRoles() {
        // Arrange
        Tipper tipper = erika();
        TipperRole communityAdmin = TipperRole.builder()
                .role(CommunityRole.builder().community(Community.builder().id(2L).build()).build())
                .build();
        TipperRole competitionAdmin = TipperRole.builder()
                .role(CompetitionRole.builder().competition(Competition.builder().id(4L).build()).build())
                .build();
        TipperRole roleWithoutTarget = TipperRole.builder().role(CommunityRole.builder().build()).build();
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of());
        given(tipperRoleRepository.findWithRoleByTipperId(3L))
                .willReturn(List.of(communityAdmin, competitionAdmin, roleWithoutTarget));

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result.getAdminCommunities()).containsExactly(2L);
        assertThat(result.getAdminCompetitions()).containsExactly(4L);
    }

    @Test
    void should_fallBackToGermany_when_defaultCompetitionDoesNotExist() {
        // Arrange
        Tipper tipper = erika();
        tipper.setDefaultCompetitionId(99L);
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of());
        given(tipperRoleRepository.findWithRoleByTipperId(3L)).willReturn(List.of());
        given(competitionRepository.findById(99L)).willReturn(Optional.empty());

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result.getDefaultCountry()).isEqualTo(Country.GERMANY);
    }

    @Test
    void should_fallBackToGermany_when_countryCodeIsUnknown() {
        // Arrange
        Tipper tipper = erika();
        tipper.setDefaultCompetitionId(4L);
        Competition competition = Competition.builder()
                .id(4L)
                .competitionFamily(CompetitionFamily.builder().country("Deutschland").build())
                .build();
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(tipper));
        given(communityMembershipRepository.findByTipperId(3L)).willReturn(List.of());
        given(tipperRoleRepository.findWithRoleByTipperId(3L)).willReturn(List.of());
        given(competitionRepository.findById(4L)).willReturn(Optional.of(competition));

        // Act
        UmsInfoDto result = authService.login(login("erika", "geheim"));

        // Assert
        assertThat(result.getDefaultCountry()).isEqualTo(Country.GERMANY);
    }

    @Test
    void should_throwAuthenticationFailed_when_passwortIsWrong() {
        // Arrange
        given(tipperRepository.findByUsername("erika")).willReturn(Optional.of(erika()));

        // Act
        Throwable thrown = catchThrowable(() -> authService.login(login("erika", "falsch")));

        // Assert
        assertThat(thrown)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or passwort");
    }

    @Test
    void should_throwAuthenticationFailed_when_usernameIsUnknown() {
        // Arrange
        given(tipperRepository.findByUsername("niemand")).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> authService.login(login("niemand", "geheim")));

        // Assert
        assertThat(thrown)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or passwort");
    }
}
