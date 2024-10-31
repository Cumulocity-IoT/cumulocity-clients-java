package com.cumulocity.rest.representation.application;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import lombok.*;

import jakarta.annotation.Nonnull;

@Data
@ToString(exclude = "password")
@Builder(builderMethodName = "applicationUserRepresentation")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ApplicationUserRepresentation extends AbstractExtensibleRepresentation {
    @Nonnull
    private String tenant;

    @Nonnull
    private String name;

    @Nonnull
    private String password;
}
