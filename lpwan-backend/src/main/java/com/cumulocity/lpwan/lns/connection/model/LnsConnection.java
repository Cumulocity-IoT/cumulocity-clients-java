/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2020-2022 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.cumulocity.lpwan.lns.connection.model;

import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.sdk.client.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringEscapeUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementers of LnsConnection class have to annotate the fields of the concrete class
 * to be part of either @JsonView(LnsConnection.PublicView.class) or @JsonView(LnsConnection.InternalView.class).
 * <p>
 * All the fields with sensitive data which should not be serialized and sent to the client
 * have to be marked with InternalView and rest with PublicView.
 */
@JsonDeserialize(using = LnsConnectionDeserializer.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class LnsConnection {

    @NotBlank
    @JsonView(LnsConnection.PublicView.class)
    private String name;

    @JsonView(LnsConnection.PublicView.class)
    private String description;

    // JSON View interface for tagging Publicly visible fields
    public interface PublicView {}

    // JSON View interface for tagging Internally visible fields
    public interface InternalView extends PublicView {}

    public void initializeWith(LnsConnection lnsConnection) {
        this.setName(lnsConnection.getName());
        this.setDescription(lnsConnection.getDescription());

        this.update(lnsConnection);
    }

    public void setName(String name) {
        if (StringUtils.isNotBlank(name)) {
            name = StringEscapeUtils.escapeHtml(name);
            this.name = name.trim().toLowerCase();
        } else {
            this.name = null;
        }
    }

    @JsonIgnore
    public boolean isValid() throws InputDataValidationException {
        List<String> missingFields = new ArrayList<>(1);

        if (StringUtils.isBlank(name)) {
            missingFields.add("'name'");
        }

        if (!missingFields.isEmpty()) {
            throw new InputDataValidationException(this.getClass().getSimpleName() + " is missing mandatory fields: " + String.join(", ", missingFields));
        }

        this.validate();

        return true;
    }

    /**
     * Invoked by isValid().
     * Implementors are expected validate the fields and throw InputDataValidationException
     * is the connection data is semantically invalid.
     *
     * @throws InputDataValidationException Thrown if the connection is semantically invalid.
     */
    protected abstract void validate() throws InputDataValidationException;

    /**
     * Invoked by initializeWith().
     * Implementors are expected to update this connection with the contents from the passed-in connection and also
     * ensure that the passwords and other sensitive data is updated only if the corresponding contents of the
     * passed-in connection are non null and not blank.
     *
     * @param lnsConnectionWithNewData LNS Connection with updated data
     */
    protected abstract void update(@NotNull LnsConnection lnsConnectionWithNewData);
}

