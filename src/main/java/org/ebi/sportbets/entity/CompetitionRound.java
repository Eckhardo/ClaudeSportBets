package org.ebi.sportbets.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "competition_round")
public class CompetitionRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "first_matchday", nullable = false)
    private int firstMatchday;

    @Column(name = "matchdays_size", nullable = false)
    private int matchdaysSize;

    @Column(name = "teams_size", nullable = false)
    private int teamsSize;

    @Column(name = "has_groups", nullable = false)
    private boolean hasGroups;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_comp", nullable = false)
    private Competition competition;

    // Declared before groups so matches (which reference groups) are removed first
    @Builder.Default
    @OneToMany(mappedBy = "competitionRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Spieltag> spieltage = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "competitionRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionGroup> groups = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
}
