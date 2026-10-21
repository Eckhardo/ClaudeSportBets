package org.ebi.sportbets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "tipp_modus_point",
        uniqueConstraints = @UniqueConstraint(name = "UniqueNameAndCommunity", columnNames = {"name", "fk_comm_id"}))
public class TippModusPoint extends TippModus {

    @Column(name = "total_points")
    private Integer totalPoints;
}
