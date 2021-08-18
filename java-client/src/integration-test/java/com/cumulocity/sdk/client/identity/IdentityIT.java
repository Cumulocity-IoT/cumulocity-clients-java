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
package com.cumulocity.sdk.client.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.idtype.XtId;
import com.cumulocity.rest.representation.identity.ExternalIDCollectionRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;

import static org.assertj.core.api.Assertions.assertThat;

//TODO inline step definitions (see AlarmIT or InventoryIT)
public class IdentityIT extends JavaSdkITBase {

    private IdentityApi identity;

    private static final int NOT_FOUND = 404;

    private List<ExternalIDRepresentation> input;
    private List<ExternalIDRepresentation> result1;
    private ExternalIDCollectionRepresentation collection1;

    private boolean notFound;

    @BeforeEach
    public void setup() throws Exception {
        identity = platform.getIdentityApi();
        input = new ArrayList<>();
        result1 = new ArrayList<>();
        notFound = false;
    }

    @AfterEach
    public void tearDown() throws SDKException {
        for (ExternalIDRepresentation e : input) {
            try {
                identity.deleteExternalId(e);
            } catch (SDKException e1) {
                if (e1.getHttpStatus() != 404) {
                    throw e1;
                }
            }
        }
    }

    @Test
    public void createExternalIdAndGetAllForTheGlobalId() {
        // given
        iHaveManagedObject(100, "DN-1", "com.nsn.DN");
        // when
        iCallCreate();
        // then
        shouldGetBackTheExternalId();
    }

    @Test
    public void createMultipleExternalIdsAndGetAllForTheGlobalId() {
        // given
        iHaveManagedObject(200, "com.type1", "1002");
        iHaveManagedObject(200, "com.dn", "DN-1");
        // when
        iCreateAll();
        // then
        shouldGetBackAllTheIds();

    }

    @Test
    public void createOneExternalIdAndGetTheExternalId() {
        // given
        iHaveManagedObject(100, "DN-1", "com.nsn.DN");
        // when
        iCallCreate();
        iGetTheExternalId();
        // then
        shouldGetBackTheExternalId();
    }

    @Test
    public void createOneExternalIdAndDeleteIdAndGetTheExternalId() {
        // given
        iHaveManagedObject(100, "DN-1", "com.nsn.DN");
        // when
        iCallCreate();
        iDeleteTheExternalId();
        iGetTheExternalId();
        // then
        shouldNotBeFound();
    }

    // ------------------------------------------------------------------------
    // Given
    // ------------------------------------------------------------------------
    private void iHaveManagedObject(long globalId, String extId, String type) {
        ExternalIDRepresentation rep = new ExternalIDRepresentation();
        rep.setExternalId(extId);
        rep.setType(type);
        ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
        GId gId = new GId();
        gId.setValue(Long.toString(globalId));
        mo.setId(gId);
        rep.setManagedObject(mo);
        input.add(rep);
    }

    // ------------------------------------------------------------------------
    // When
    // ------------------------------------------------------------------------
    private void iCallCreate() throws SDKException {
        result1.add(identity.create(input.get(0)));
    }

    private void iCreateAll() throws SDKException {
        for (ExternalIDRepresentation rep : input) {
            result1.add(identity.create(rep));
        }
    }

    private void iGetTheExternalId() throws SDKException {
        result1.clear();
        try {
            XtId id = new XtId(input.get(0).getExternalId());
            id.setType(input.get(0).getType());
            result1.add(identity.getExternalId(id));
        } catch (SDKException e) {
            notFound = (NOT_FOUND == e.getHttpStatus());
        }
    }

    private void iDeleteTheExternalId() throws SDKException {
        ExternalIDRepresentation extIdRep = new ExternalIDRepresentation();
        extIdRep.setExternalId(input.get(0).getExternalId());
        extIdRep.setType(input.get(0).getType());
        identity.deleteExternalId(extIdRep);
    }

    // ------------------------------------------------------------------------
    // Then
    // ------------------------------------------------------------------------
    private void shouldGetBackTheExternalId() throws SDKException {
        collection1 = identity.getExternalIdsOfGlobalId(input.get(0).getManagedObject().getId()).get();
        assertThat(collection1.getExternalIds()).hasSize(1);
        assertThat(collection1.getExternalIds().get(0).getExternalId()).isEqualTo(input.get(0).getExternalId());
        assertThat(collection1.getExternalIds().get(0).getType()).isEqualTo(input.get(0).getType());
        assertThat(collection1.getExternalIds().get(0).getManagedObject().getId().getValue())
                .isEqualTo(input.get(0).getManagedObject().getId().getValue());
    }

    private void shouldGetBackAllTheIds() throws SDKException {
        collection1 = identity.getExternalIdsOfGlobalId(input.get(0).getManagedObject().getId()).get();
        assertThat(collection1.getExternalIds()).hasSameSizeAs(input);

        Map<String, ExternalIDRepresentation> result = new HashMap<String, ExternalIDRepresentation>();

        for (int index = 0; index < input.size(); index++) {
            result.put(collection1.getExternalIds().get(index).getExternalId(), collection1.getExternalIds().get(index));
        }

        for (int index = 0; index < input.size(); index++) {
            ExternalIDRepresentation rep = result.get(input.get(index).getExternalId());
            assertThat(rep).isNotNull();
            assertThat(rep.getType()).isEqualTo(input.get(index).getType());
            assertThat(rep.getManagedObject().getId().getValue()).isEqualTo(input.get(index).getManagedObject().getId().getValue());
        }
    }

    private void shouldNotBeFound() {
        assertThat(notFound).isTrue();
    }

}
