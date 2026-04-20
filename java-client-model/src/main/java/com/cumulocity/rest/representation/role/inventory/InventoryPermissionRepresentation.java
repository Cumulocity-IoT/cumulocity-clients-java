package com.cumulocity.rest.representation.role.inventory;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryPermissionRepresentation extends AbstractExtensibleRepresentation {

    private Long id;

    private String type;

    private String scope;

    private String permission;

}
