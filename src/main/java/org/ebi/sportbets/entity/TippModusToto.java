package org.ebi.sportbets.entity;

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
@Table(name = "tipp_modus_toto",
        uniqueConstraints = @UniqueConstraint(name = "UniqueNameAndCommunity", columnNames = {"name", "fk_comm_id"}))
public class TippModusToto extends TippModus {
}
