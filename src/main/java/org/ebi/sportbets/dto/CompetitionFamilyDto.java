package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CompetitionFamilyDto {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String country;

    @NotNull
    private Boolean hasClubs;

    @NotNull
    private Boolean hasLigaModus;

    private LocalDateTime createdOn;
}
