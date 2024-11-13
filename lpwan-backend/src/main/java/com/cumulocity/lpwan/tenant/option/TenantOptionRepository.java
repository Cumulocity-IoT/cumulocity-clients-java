/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.tenant.option;

import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;


public interface TenantOptionRepository {

    String getAndDecryptOptionValue(OptionPK optionPK) throws DecryptFailedException, TenantOptionNotFoundException;

    void encryptAndSetOption(OptionRepresentation option);

    String getOptionValue(OptionPK optionPK) throws TenantOptionNotFoundException;

    void setOption(OptionRepresentation option);
}
