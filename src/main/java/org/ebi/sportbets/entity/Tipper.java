package org.ebi.sportbets.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipper")
public class Tipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    private String email;

    @Column(nullable = false)
    private String passwort;

    @Column(name = "passwort_hint")
    private String passwortHint;

    @Column(name = "is_community_admin", nullable = false)
    private boolean communityAdmin;

    @Column(name = "is_competition_admin", nullable = false)
    private boolean competitionAdmin;

    // Plain ids: the schema defines no foreign key constraints for these columns
    @Column(name = "default_community_id")
    private Long defaultCommunityId;

    @Column(name = "default_competition_id")
    private Long defaultCompetitionId;

    @Builder.Default
    @OneToMany(mappedBy = "tipper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipperRole> tipperRoles = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tipper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityMembership> communityMemberships = new ArrayList<>();
}
