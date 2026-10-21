package org.ebi.sportbets.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@DiscriminatorValue("COMM")
public class CommunityRole extends Role {

    // Nullable at DB level because the column is shared with CompetitionRole rows
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_comm_id")
    private Community community;
}
