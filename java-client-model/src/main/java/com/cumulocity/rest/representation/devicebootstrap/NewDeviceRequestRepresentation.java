package com.cumulocity.rest.representation.devicebootstrap;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.rest.representation.CustomPropertiesMapRepresentation;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

@Getter
@Setter
public class NewDeviceRequestRepresentation extends CustomPropertiesMapRepresentation {

    private String id;

    private String status;

    private String tenantId;
    
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String groupId;

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String type;

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String owner;

    @Getter(onMethod_ = {@JSONProperty(ignoreIfNull = true), @JSONConverter(type = DateTimeConverter.class)})
    private DateTime creationTime;

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String securityToken;

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String enrollmentToken;

}
