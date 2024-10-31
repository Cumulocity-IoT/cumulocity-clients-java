package com.cumulocity.rest.representation.inventory;

import jakarta.ws.rs.core.MediaType;

import com.cumulocity.rest.representation.CumulocityMediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class InventoryMediaType extends CumulocityMediaType {

    public static final InventoryMediaType MANAGED_OBJECT = new InventoryMediaType("managedObject");

    public static final InventoryMediaType MANAGED_OBJECT_COLLECTION = new InventoryMediaType("managedObjectCollection");

    public static final InventoryMediaType MANAGED_OBJECT_REFERENCE = new InventoryMediaType("managedObjectReference");

    public static final InventoryMediaType MANAGED_OBJECT_REFERENCE_COLLECTION = new InventoryMediaType("managedObjectReferenceCollection");

    public static final InventoryMediaType INVENTORY_API = new InventoryMediaType("inventoryApi");

    public static final InventoryMediaType MANAGED_OBJECT_USER = new InventoryMediaType("managedObjectUser");

    public static final String MANAGED_OBJECT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObject+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String MANAGED_OBJECT_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String MANAGED_OBJECT_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY
            + "managedObjectReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    /**
     * MediaType used in requests to inventory
     */
    public static final String INVENTORY_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "inventoryApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String MANAGED_OBJECT_USER_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "managedObjectUser+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public InventoryMediaType(String string) {
        super(string);
    }
}
