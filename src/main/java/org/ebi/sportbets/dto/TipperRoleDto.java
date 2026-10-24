package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipperRoleDto {

    private Long id;

    @NotNull
    private Long tipperId;

    @NotNull
    private Long roleId;
}
