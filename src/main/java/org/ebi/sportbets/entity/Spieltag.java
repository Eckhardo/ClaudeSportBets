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
@Table(name = "spieltag")
public class Spieltag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spieltag_number", nullable = false)
    private int spieltagNumber;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_comp_round_id", nullable = false)
    private CompetitionRound competitionRound;

    @Builder.Default
    @OneToMany(mappedBy = "spieltag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Spiel> spiele = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "spieltag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TippConfig> tippConfigs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
}
