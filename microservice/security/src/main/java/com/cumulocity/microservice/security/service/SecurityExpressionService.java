package com.cumulocity.microservice.security.service;

public interface SecurityExpressionService {

    /**
     * Spring bean used to reference this interface methods in security expressions.
     * See <a href="https://docs.spring.io/spring-security/reference/5.8/migration/servlet/authorization.html#_use_a_custom_bean_instead_of_subclassing_defaultmethodsecurityexpressionhandler">
     *     Use a Custom @Bean instead of subclassing DefaultMethodSecurityExpressionHandler</a>
     */
    String BEAN_NAME = "c8yAuthz";

    boolean isFeatureEnabled(String featureName);

    boolean isServiceUser(String service);

    boolean isCurrentTenantManagement();
}
