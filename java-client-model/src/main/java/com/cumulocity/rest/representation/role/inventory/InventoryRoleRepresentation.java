package com.cumulocity.rest.representation.role.inventory;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.svenson.JSONTypeHint;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRoleRepresentation extends AbstractExtensibleRepresentation {

    private Long id;

    @Size(max = 50)
    private String name;

    private String description;

    @Singular
    private List<InventoryPermissionRepresentation> permissions;

    @JSONTypeHint(InventoryPermissionRepresentation.class)
    public List<InventoryPermissionRepresentation> getPermissions() {
        return permissions;
    }

}
