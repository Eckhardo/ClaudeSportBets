package org.ebi.sportbets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "spiel_formula")
public class SpielFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_spiel_id", nullable = false)
    private Spiel spiel;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "team_name_acronym")
    private String teamNameAcronym;

    @Column(name = "is_heim_team", nullable = false)
    private boolean heimTeam;

    @Column(name = "heim_tore", nullable = false)
    private int heimTore;

    @Column(name = "gast_tore", nullable = false)
    private int gastTore;

    @Column(name = "diff_tore", nullable = false)
    private int diffTore;

    @Column(nullable = false)
    private int won;

    @Column(nullable = false)
    private int remis;

    @Column(nullable = false)
    private int lost;

    @Column(nullable = false)
    private int points;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
}
