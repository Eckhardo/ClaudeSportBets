package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TippConfigDto {

    private Long id;

    @NotNull
    private Long competitionMembershipId;

    @NotNull
    private Long spieltagId;

    @NotNull
    private Long tippModusId;

    private LocalDateTime createdOn;
}
