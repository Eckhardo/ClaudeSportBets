package org.ebi.sportbets.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TippModusResultDto extends TippModusDto {

    @PositiveOrZero
    private Integer tendencyPoints;

    @PositiveOrZero
    private Integer bonusPoints;
}
