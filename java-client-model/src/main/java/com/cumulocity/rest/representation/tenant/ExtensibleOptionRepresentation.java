package com.cumulocity.rest.representation.tenant;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.Size;

public class ExtensibleOptionRepresentation extends AbstractExtensibleRepresentation {

    @Size(max = 256)
    private String category;

    @Size(max = 256)
    private String key;

    @Size(max = 256)
    private String value;

    private Boolean editable;

    public static ExtensibleOptionRepresentation asExtensibleOptionRepresentation(String category, String key,
                                                                                  String value, boolean editable) {
        final ExtensibleOptionRepresentation optionRepresentation = new ExtensibleOptionRepresentation();
        optionRepresentation.setKey(key);
        optionRepresentation.setValue(value);
        optionRepresentation.setCategory(category);
        optionRepresentation.setEditable(editable);
        return optionRepresentation;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

}
