/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.sample.connection.model;

import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.lpwan.lns.connection.model.LnsConnection;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;


@JsonDeserialize(using = JsonDeserializer.None.class) // Important to override the base class' usage of LnsConnectionDeserializer which produces an infinite loop
@Getter
@Setter
@NoArgsConstructor
public class SampleConnection extends LnsConnection {

    @NotBlank
    @JsonView(LnsConnection.PublicView.class)
    private String user;

    @JsonView(LnsConnection.InternalView.class)
    private String password;



    // BELOW CONSTANTS ARE USED IN LnsConnectionTest.java and LnsConnectionControllerTest.java
    public static final SampleConnection VALID_SAMPLE_CONNECTION = SampleConnection.builder()
                                                                            .name("Sample Connection Name")
                                                                            .description("Sample Connection Description")
                                                                            .user("USER NAME")
                                                                            .password("**********")
                                                                            .build();

    public static final SampleConnection VALID_SAMPLE_CONNECTION_SPECIAL_CHARACTERS_1 = SampleConnection.builder()
            .name("a!cd~^ (76 5$)")
            .description("Sample Connection Description")
            .user("USER NAME")
            .password("**********")
            .build();

    public static final SampleConnection VALID_SAMPLE_CONNECTION_SPECIAL_CHARACTERS_2 = SampleConnection.builder()
            .name("(null null !@$^::~`),.")
            .description("Sample Connection Description")
            .user("USER NAME")
            .password("**********")
            .build();

    // Use the below commented code to generate the Json for INTERNAL VIEW (which includes "password" field)
    // System.out.println(new ObjectMapper().writerWithView(LnsConnection.InternalView.class).writeValueAsString(VALID_SAMPLE_CONNECTION));
    //    {
    //        "name": "Sample Connection Name",
    //        "description": "Sample Connection Description",
    //        "user": "USER NAME",
    //        "password": "**********"
    //    }
    public static final String VALID_SAMPLE_CONNECTION_JSON_INTERNAL_VIEW = "{\"name\":\"Sample Connection Name\",\"description\":\"Sample Connection Description\",\"user\":\"USER NAME\",\"password\":\"**********\"}";

    // Use the below commented code to generate the Json for PUBLIC VIEW (which excludes "password" field)
    // System.out.println(new ObjectMapper().writerWithView(LnsConnection.PublicView.class).writeValueAsString(VALID_SAMPLE_CONNECTION));
    //    {
    //        "name": "Sample Connection Name",
    //        "description": "Sample Connection Description",
    //        "user": "USER NAME"
    //    }
    public static final String VALID_SAMPLE_CONNECTION_JSON_PUBLIC_VIEW = "{\"name\":\"Sample Connection Name\",\"description\":\"Sample Connection Description\",\"user\":\"USER NAME\"}";



    @Builder
    public SampleConnection(String name, String description, String user, String password) {
        super(name, description);

        this.user = user;
        this.password = password;
    }

    @Override
    protected void validate() throws InputDataValidationException {
        List<String> missingFields = new ArrayList<>(3);

        if (StringUtils.isBlank(user)) {
            missingFields.add("'user'");
        }

        if(!missingFields.isEmpty()) {
            throw new InputDataValidationException("SampleConnection is missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    @Override
    protected void update(@NotNull LnsConnection lnsConnectionWithNewData) {
        SampleConnection sampleConnectionWithNewData = (SampleConnection) lnsConnectionWithNewData;
        this.setUser(sampleConnectionWithNewData.getUser());
        if(!StringUtils.isBlank(sampleConnectionWithNewData.getPassword())) {
            this.setPassword(sampleConnectionWithNewData.getPassword());
        }
    }

    @Override
    public boolean checkConnectionStatus() {
        SampleConnection sampleConnectionWithNewData = new SampleConnection();
        sampleConnectionWithNewData.setConnectionReachable(true);
        return true;
    }
}
