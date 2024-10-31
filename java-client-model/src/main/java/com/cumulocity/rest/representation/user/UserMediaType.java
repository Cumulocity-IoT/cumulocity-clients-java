package com.cumulocity.rest.representation.user;

import com.cumulocity.rest.representation.CumulocityMediaType;

import jakarta.ws.rs.core.MediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class UserMediaType extends CumulocityMediaType {
    
    public static final UserMediaType USER = new UserMediaType("user");
    
    public static final String USER_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "user+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType CURRENT_USER = new UserMediaType("currentUser");
    
    public static final String CURRENT_USER_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "currentUser+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType CURRENT_TENANT = new UserMediaType("currentTenant");

    public static final String CURRENT_TENANT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "currentTenant+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType USER_COLLECTION = new UserMediaType("userCollection");

    public static final String USER_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType USER_API = new UserMediaType("userApi");

    public static final String USER_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
        
    public static final UserMediaType USER_REFERENCE = new UserMediaType("userReference");
    
    public static final String USER_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType USER_REFERENCE_COLLECTION = new UserMediaType("userReferenceCollection");

    public static final String USER_REFERENCE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
    
    public static final UserMediaType GROUP = new UserMediaType("group");
    
    public static final String GROUP_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "group+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType GROUP_COLLECTION = new UserMediaType("groupCollection");

    public static final String GROUP_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "groupCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType GROUP_REFERENCE = new UserMediaType("groupReference");

    public static final String GROUP_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "groupReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType GROUP_REFERENCE_COLLECTION = new UserMediaType("groupReferenceCollection");

    public static final String GROUP_REFERENCE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "groupReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
    
    public static final UserMediaType ROLE = new UserMediaType("role");
    
    public static final String ROLE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "role+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

	public static final UserMediaType ROLE_COLLECTION = new UserMediaType("roleCollection");

	public static final String ROLE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "roleCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

	public static final UserMediaType ROLE_REFERENCE = new UserMediaType("roleReference");
    
    public static final String ROLE_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "roleReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType ROLE_REFERENCE_COLLECTION = new UserMediaType("roleReferenceCollection");

    public static final String ROLE_REFERENCE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "roleReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType INVENTORY_ASSIGNMENT = new UserMediaType("inventoryAssignment");

    public static final String INVENTORY_ASSIGNMENT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "inventoryAssignment+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType INVENTORY_ASSIGNMENT_COLLECTION = new UserMediaType("inventoryAssignmentCollection");

    public static final String INVENTORY_ASSIGNMENT_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "inventoryAssignmentCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType USER_OWNER_REFERENCE = new UserMediaType("userOwnerReference");

    public static final String USER_OWNER_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userOwnerReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final UserMediaType USER_DELEGATED_BY_REFERENCE = new UserMediaType("userDelegatedByReference");

    public static final String USER_DELEGATED_BY_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "userDelegatedByReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;


    public UserMediaType(String entity) {
        super("application", VND_COM_NSN_CUMULOCITY + entity + "+json;" + VND_COM_NSN_CUMULOCITY_PARAMS);
    }
}
