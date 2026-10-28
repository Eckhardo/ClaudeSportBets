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
@Table(name = "spiel")
public class Spiel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spiel_number", nullable = false)
    private int spielNumber;

    @Column(name = "anpfiffdate")
    private LocalDateTime anpfiffDate;

    @Column(name = "heim_tore", nullable = false)
    private int heimTore;

    @Column(name = "gast_tore", nullable = false)
    private int gastTore;

    @Column(nullable = false)
    private boolean stattgefunden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_heim_team_id")
    private Team heimTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gast_team_id")
    private Team gastTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_spieltag_id")
    private Spieltag spieltag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_group_id")
    private CompetitionGroup competitionGroup;

    @Builder.Default
    @OneToMany(mappedBy = "spiel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tipp> tipps = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "spiel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpielFormula> spielFormulas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
}
