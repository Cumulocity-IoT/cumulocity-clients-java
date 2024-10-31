package com.cumulocity.rest.representation.user;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.Size;

public class RoleRepresentation extends AbstractExtensibleRepresentation {

    //note that id is actually same as name, name is not used in model
    private String id;

    @Size(max = 50)
    @Deprecated
    // should not be used, there is only id in Authority which is also the name
    private String name;

    public void setId(String id) {
        this.id = id;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getName() {
        return name;
    }
}
