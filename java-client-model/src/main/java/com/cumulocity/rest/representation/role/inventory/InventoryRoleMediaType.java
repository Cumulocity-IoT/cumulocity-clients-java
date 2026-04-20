package com.cumulocity.rest.representation.role.inventory;

import lombok.experimental.UtilityClass;

import static com.cumulocity.rest.representation.CumulocityMediaType.APPLICATION_VND_COM_NSN_CUMULOCITY;
import static com.cumulocity.rest.representation.CumulocityMediaType.VND_COM_NSN_CUMULOCITY_PARAMS;

@UtilityClass
public class InventoryRoleMediaType {
    public static final String INVENTORY_ROLE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "inventoryRole+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
    public static final String INVENTORY_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "inventoryRoleCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
}
