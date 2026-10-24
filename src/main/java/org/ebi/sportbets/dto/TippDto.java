package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TippDto {

    private Long id;

    @NotNull
    private Long communityMembershipId;

    @NotNull
    private Long spielId;

    @NotNull
    private Long tippModusId;

    @PositiveOrZero
    private Integer heimTipp;

    @PositiveOrZero
    private Integer gastTipp;

    @PositiveOrZero
    private Integer remisTipp;

    @PositiveOrZero
    private Integer winPoints;

    private LocalDateTime createdOn;
}
