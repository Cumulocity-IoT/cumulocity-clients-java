/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.cumulocity.sdk.client.inventory;

import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.BulkRequestException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;

public class BulkInventoryIT extends JavaSdkITBase {

    private static final int NOT_FOUND = 404;

    private static final int CONFLICT = 409;

    private static final int UNPROCESSABLE_ENTITY = 422;

    private static final String EXTERNAL_ID_TYPE = "bulk_test_external_id";

    private InventoryApi inventory;

    @BeforeEach
    public void setUp() throws Exception {
        inventory = platform.getInventoryApi();
        platform.setRequireResponseBody(true);
    }

    @Test
    @DisplayName("creates managed objects in bulk")
    public void createsInBulk() {
        // Given
        ManagedObjectCollectionRepresentation toCreate = new ManagedObjectCollectionRepresentation();
        toCreate.getManagedObjects().add(aSampleMo().withName("bulk1").build());
        toCreate.getManagedObjects().add(aSampleMo().withName("bulk2").build());

        // When
        ManagedObjectCollectionRepresentation created = inventory.createBulk(toCreate);

        // Then
        assertThat(created.getManagedObjects()).hasSize(2);
        assertThat(created.getManagedObjects()).extracting(ManagedObjectRepresentation::getName).containsExactlyInAnyOrder("bulk1", "bulk2");
        assertThat(created.getManagedObjects()).allSatisfy(mo -> assertThat(mo.getId()).isNotNull());
    }

    @Test
    @DisplayName("updates managed objects in bulk")
    public void updatesInBulk() {
        // Given
        ManagedObjectRepresentation mo1 = inventory.create(aSampleMo().withName("bulk1").build());
        ManagedObjectRepresentation mo2 = inventory.create(aSampleMo().withName("bulk2").build());
        ManagedObjectCollectionRepresentation toUpdate = new ManagedObjectCollectionRepresentation();
        toUpdate.getManagedObjects().add(aSampleMo().withID(mo1.getId()).withName("bulk1-updated").build());
        toUpdate.getManagedObjects().add(aSampleMo().withID(mo2.getId()).withName("bulk2-updated").build());

        // When
        ManagedObjectCollectionRepresentation updated = inventory.updateBulk(toUpdate);

        // Then
        assertThat(updated.getManagedObjects()).extracting(ManagedObjectRepresentation::getName)
                .containsExactlyInAnyOrder("bulk1-updated", "bulk2-updated");
        assertThat(inventory.get(mo1.getId()).getName()).isEqualTo("bulk1-updated");
        assertThat(inventory.get(mo2.getId()).getName()).isEqualTo("bulk2-updated");
    }

    @Test
    @DisplayName("bulk update names the failed element if a managed object does not exist")
    public void updateFailsForMissingManagedObject() {
        // Given
        ManagedObjectRepresentation existing = inventory.create(aSampleMo().withName("bulk1").build());
        ManagedObjectCollectionRepresentation toUpdate = new ManagedObjectCollectionRepresentation();
        toUpdate.getManagedObjects().add(aSampleMo().withID(existing.getId()).withName("bulk1-updated").build());
        toUpdate.getManagedObjects().add(aSampleMo().withID(new GId("1")).withName("missing").build());

        // When
        Throwable thrown = catchThrowable(() -> inventory.updateBulk(toUpdate));

        // Then
        assertThat(thrown).isInstanceOfSatisfying(BulkRequestException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(NOT_FOUND);
            assertThat(e.getFailedIndex()).isEqualTo(1);
        });
        assertThat(inventory.get(existing.getId()).getName()).isEqualTo("bulk1");
    }

    @Test
    @DisplayName("bulk create creates nothing if an external id was already bound before the request")
    public void createFailsForBoundExternalId() {
        // Given
        String externalIdValue = "bound-before-request-" + UUID.randomUUID();
        ManagedObjectRepresentation existing = inventory.create(aSampleMo().build());
        platform.getIdentityApi().create(anExternalId(EXTERNAL_ID_TYPE, externalIdValue, existing));
        String type = "bulk_create_conflict";
        ManagedObjectCollectionRepresentation toCreate = new ManagedObjectCollectionRepresentation();
        toCreate.getManagedObjects().add(aSampleMo().withName("bulk1").withType(type).build());
        toCreate.getManagedObjects().add(withExternalId(aSampleMo().withName("bulk2").withType(type).build(), externalIdValue));

        // When
        Throwable thrown = catchThrowable(() -> inventory.createBulk(toCreate));

        // Then
        assertThat(thrown).isInstanceOfSatisfying(BulkRequestException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(CONFLICT);
            assertThat(e.getFailedIndex()).isEqualTo(1);
            assertThat(e.getFragmentType()).isNull();
        });
        assertThat(inventory.countManagedObjectsByFilter(new InventoryFilter().byType(type))).isZero();
    }

    @Test
    @DisplayName("bulk update rejects external ids")
    public void updateFailsForExternalIds() {
        // Given
        ManagedObjectRepresentation existing = inventory.create(aSampleMo().withName("bulk1").build());
        ManagedObjectCollectionRepresentation toUpdate = new ManagedObjectCollectionRepresentation();
        toUpdate.getManagedObjects()
                .add(withExternalId(aSampleMo().withID(existing.getId()).withName("bulk1-updated").build(), "not-supported"));

        // When
        Throwable thrown = catchThrowable(() -> inventory.updateBulk(toUpdate));

        // Then
        assertThat(thrown).isInstanceOfSatisfying(BulkRequestException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(UNPROCESSABLE_ENTITY);
            assertThat(e.getFailedIndex()).isEqualTo(0);
            assertThat(e.getFragmentType()).isNull();
        });
        assertThat(inventory.get(existing.getId()).getName()).isEqualTo("bulk1");
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    private static ManagedObjectRepresentation withExternalId(ManagedObjectRepresentation mo, String externalId) {
        mo.setProperty("externalIds", List.of(Map.of("type", EXTERNAL_ID_TYPE, "externalId", externalId)));
        return mo;
    }

    private static ExternalIDRepresentation anExternalId(String type, String externalId, ManagedObjectRepresentation mo) {
        ExternalIDRepresentation rep = new ExternalIDRepresentation();
        rep.setType(type);
        rep.setExternalId(externalId);
        rep.setManagedObject(mo);
        return rep;
    }
}
