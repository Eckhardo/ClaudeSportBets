package org.ebi.sportbets.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TableGenerator;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.ebi.sportbets.enums.TippModusType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base of the table-per-class tipp mode hierarchy (tipp_modus_point, tipp_modus_result, tipp_modus_toto).
 * Ids must be unique across all three tables, so they come from the shared {@code hibernate_sequences} table.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class TippModus {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "tipp_modus_gen")
    @TableGenerator(name = "tipp_modus_gen", table = "hibernate_sequences",
            pkColumnName = "sequence_name", valueColumnName = "next_val",
            pkColumnValue = "default", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = TippModusTypeConverter.class)
    private TippModusType type;

    private Integer deadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_comm_id", nullable = false)
    private Community community;

    @Builder.Default
    @OneToMany(mappedBy = "tippModus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tipp> tipps = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tippModus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TippConfig> tippConfigs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
}
