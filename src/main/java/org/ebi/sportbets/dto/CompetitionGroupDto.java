package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionGroupDto {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @Positive
    private Integer groupNumber;

    @NotNull
    private Long competitionRoundId;

    private LocalDateTime createdOn;
}
