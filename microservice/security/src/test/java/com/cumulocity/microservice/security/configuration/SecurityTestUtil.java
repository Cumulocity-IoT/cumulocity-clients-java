package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.filter.util.HttpRequestUtils;
import com.cumulocity.microservice.security.service.SecurityUserDetails;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityTestUtil {

    public static final SecurityUserDetails fromCumuloUsername(String tenantAndUser) {
        String [] parts = HttpRequestUtils.splitUsername(tenantAndUser);
        return SecurityUserDetails
                .activeUser(parts[0], parts[1], "secret");
    }

}
