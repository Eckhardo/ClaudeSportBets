package org.ebi.sportbets.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Mirrors the single-table role hierarchy; {@code roleType} selects the subtype in JSON.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "roleType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommunityRoleDto.class, name = "COMM"),
        @JsonSubTypes.Type(value = CompetitionRoleDto.class, name = "COMP")
})
public abstract class RoleDto {

    private Long id;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    private LocalDateTime createdOn;
}
