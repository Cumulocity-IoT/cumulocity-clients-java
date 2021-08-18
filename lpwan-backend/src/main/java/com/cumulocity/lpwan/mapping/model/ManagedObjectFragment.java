package com.cumulocity.lpwan.mapping.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ManagedObjectFragment {

    /**
     * A managed object's fragment can have multiple inner keys.
     */
    private String fragmentType;
    
    private Map<String, Object> innerObject = new HashMap<>();
    
    private Object innerField;
    
    public void putFragmentValue(String key, DecodedObject decodedObject) {
        innerObject.put(key, decodedObject.getFields());
    }
    
    public Object getFragmentValue(String key) {
        return innerObject.get(key);
    }

    public void putFragmentValue(DecodedObject decodedObject) {
        this.innerField = decodedObject.getFields();
        
    }    
}
