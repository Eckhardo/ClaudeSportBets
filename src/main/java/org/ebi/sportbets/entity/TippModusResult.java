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
@Table(name = "tipp_modus_result",
        uniqueConstraints = @UniqueConstraint(name = "UniqueNameAndCommunityResult", columnNames = {"name", "fk_comm_id"}))
public class TippModusResult extends TippModus {

    @Column(name = "tendency_points")
    private Integer tendencyPoints;

    @Column(name = "bonus_points")
    private Integer bonusPoints;
}
