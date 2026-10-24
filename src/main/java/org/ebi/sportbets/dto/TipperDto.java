package org.ebi.sportbets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipperDto {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String firstname;

    @NotBlank
    @Size(max = 255)
    private String lastname;

    @Email
    @Size(max = 255)
    private String email;

    // Accepted on input, never serialized into responses or logs
    @NotBlank
    @Size(max = 255)
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwort;

    @Size(max = 255)
    private String passwortHint;

    @NotNull
    private Boolean communityAdmin;

    @NotNull
    private Boolean competitionAdmin;

    private Long defaultCommunityId;

    private Long defaultCompetitionId;
}
