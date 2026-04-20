package com.cumulocity.rest.representation.user;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.role.inventory.InventoryRoleRepresentation;
import lombok.*;
import org.svenson.JSONTypeHint;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAssignmentRepresentation extends AbstractExtensibleRepresentation {

    private Long id;

    private String managedObject;

    @Singular
    private List<InventoryRoleRepresentation> roles = new ArrayList<>();

    @JSONTypeHint(InventoryRoleRepresentation.class)
    public List<InventoryRoleRepresentation> getRoles() {
        return roles;
    }

}
