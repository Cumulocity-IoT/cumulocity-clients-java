package com.cumulocity.rest.representation.user;

import com.cumulocity.rest.representation.BaseCollectionRepresentation;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

import java.util.Iterator;
import java.util.List;

public class InventoryAssignmentCollectionRepresentation extends BaseCollectionRepresentation<InventoryAssignmentRepresentation> {

    private List<InventoryAssignmentRepresentation> inventoryAssignments;

    public List<InventoryAssignmentRepresentation> getInventoryAssignments() {
        return inventoryAssignments;
    }

    @JSONTypeHint(InventoryAssignmentRepresentation.class)
    public void setInventoryAssignments(List<InventoryAssignmentRepresentation> inventoryAssignments) {
        this.inventoryAssignments = inventoryAssignments;
    }

    @Override
    @JSONProperty(ignore = true)
    public Iterator<InventoryAssignmentRepresentation> iterator() {
        return inventoryAssignments.iterator();
    }
}
