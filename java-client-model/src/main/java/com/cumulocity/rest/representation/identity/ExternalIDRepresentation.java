package com.cumulocity.rest.representation.identity;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import lombok.EqualsAndHashCode;
import org.svenson.JSONProperty;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

import jakarta.validation.constraints.Size;

@EqualsAndHashCode(callSuper = false)
public class ExternalIDRepresentation extends AbstractExtensibleRepresentation {

    @Size(min = 1, message = "field cannot be empty")
    private String externalId;

    @Size(min = 1, message = "field cannot be empty")
    private String type;

    @EqualsAndHashCode.Exclude
    private ManagedObjectRepresentation managedObject;

    @JSONProperty(ignoreIfNull = true)
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JSONProperty(ignoreIfNull = true)
    public ManagedObjectRepresentation getManagedObject() {
        return managedObject;
    }

    public void setManagedObject(ManagedObjectRepresentation managedObject) {
        this.managedObject = managedObject;
    }

    @EqualsAndHashCode.Include
    GId getManagedObjectId() {
        return managedObject == null ? null : managedObject.getId();
    }

}
