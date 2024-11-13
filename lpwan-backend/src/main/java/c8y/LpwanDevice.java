/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package c8y;

import lombok.Data;

@Data
public class LpwanDevice  {

    private boolean provisioned = false;
    private String type;
    private String errorMessage;
    private String serviceProvider;
    private String lpwanDeviceType;
    private TypeExternalId typeExternalId;
    private String lnsConnectionName;
}
