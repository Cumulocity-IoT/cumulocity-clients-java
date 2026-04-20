package com.cumulocity.rest.representation.role.inventory;

import com.cumulocity.rest.representation.BaseCollectionRepresentation;
import lombok.Data;
import org.svenson.JSONTypeHint;

import java.util.Iterator;
import java.util.List;

@Data
public class InventoryRoleCollectionRepresentation extends BaseCollectionRepresentation<InventoryRoleRepresentation> {

    private List<InventoryRoleRepresentation> roles;

    @Override
    public Iterator<InventoryRoleRepresentation> iterator() {
        return roles.iterator();
    }

    @JSONTypeHint(InventoryRoleRepresentation.class)
    public List<InventoryRoleRepresentation> getRoles() {
        return roles;
    }
}
