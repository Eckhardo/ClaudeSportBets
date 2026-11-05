package org.ebi.sportbets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ebi.sportbets.enums.Country;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UmsInfoDto {

    private String username;
    private long userId;
    @Builder.Default
    private boolean loggedIn = false;
    private boolean commAdmin;
    private boolean compAdmin;
    private Long defaultCommunityId;
    private Long defaultCompetitionId;
    private Long defaultCommMembId;
    private Long defaultCompMembId;
    private Country defaultCountry;

    /**
     * a set of competitions where tipper has admin rights
     */
    @Builder.Default
    private Set<Long> adminCompetitions = new HashSet<>();

    /**
     * a set of communities where tipper has admin rights
     */
    @Builder.Default
    private Set<Long> adminCommunities = new HashSet<>();

    /**
     * a set of communities where tipper has abonnenment
     */
    @Builder.Default
    private Set<Long> tipperCommunities = new HashSet<>();
}
