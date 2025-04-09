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

import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRefRepresentationLike;
import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectReferenceRepresentation.MO_REF_REPRESENTATION;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MOChildrenOrderingIT extends JavaSdkITBase {

    private InventoryApi inventory;

    private ManagedObject parentMo;

    @BeforeEach
    public void setUp() throws Exception {
        inventory = platform.getInventoryApi();

        ManagedObjectRepresentation parentRep = inventory.create(aSampleMo().withName("parentRep").build());
        parentMo = inventory.getManagedObject(parentRep.getId());
    }

    @Test
    public void shouldKeepChildDevicesOrder() throws Exception {
        shouldKeepChildrenInOrder(new ChildDeviceAdder(), new ChildDevicesQuery());
    }

    @Test
    public void shouldKeepChildAssetsOrder() {
        shouldKeepChildrenInOrder(new ChildAssetAdder(), new ChildAssetsQuery());
    }

    @Test
    public void shouldKeepChildAdditionsOrder() {
        shouldKeepChildrenInOrder(new ChildAdditionAdder(), new ChildAdditionsQuery());
    }

    @Test
    public void addingSameChildDeviceTwiceHasNoEffect() {
        addingSameItemTwiceHasNoEffect(new ChildDeviceAdder(), new ChildDevicesQuery());
    }

    @Test
    public void addingSameChildAssetTwiceHasNoEffect() {
        addingSameItemTwiceHasNoEffect(new ChildAssetAdder(), new ChildAssetsQuery());
    }

    @Test
    public void addingSameChildAdditionTwiceHasNoEffect() {
        addingSameItemTwiceHasNoEffect(new ChildAdditionAdder(), new ChildAdditionsQuery());
    }

    private void addingSameItemTwiceHasNoEffect(ChildAdder childAdder, ChildrenQuery childrenQuery)
            throws SDKException {

        // Given
        ManagedObjectRepresentation ch1 = inventory.create(aSampleMo().build());
        ManagedObjectRepresentation ch2 = inventory.create(aSampleMo().build());
        ManagedObjectReferenceRepresentation childRef1 = createChildRef(ch1);
        ManagedObjectReferenceRepresentation childRef2 = createChildRef(ch2);

        // When
        childAdder.addChild(childRef1);
        childAdder.addChild(childRef2);
        childAdder.addChild(childRef1);

        // Then
        List<GId> childIds = getIdsOfChildren(childrenQuery);
        assertThat(childIds).contains(ch1.getId(), ch2.getId());
    }

    private void shouldKeepChildrenInOrder(ChildAdder childrenAdder, ChildrenQuery childrenQuery) throws SDKException {
        // When
        List<GId> childDeviceIdsInCreationOrder = addChildrenToParent(childrenAdder);

        // Then
        List<GId> childIds = getIdsOfChildren(childrenQuery);
        assertThat(childIds).containsExactlyInAnyOrderElementsOf(childDeviceIdsInCreationOrder);
    }

    private List<GId> getIdsOfChildren(ChildrenQuery childrenQuery) throws SDKException {
        ManagedObjectReferenceCollectionRepresentation refCollection = childrenQuery.getChildren();
        return extractIDs(refCollection);
    }

    private List<GId> addChildrenToParent(ChildAdder childrenAdder) throws SDKException {
        List<GId> creationOrder = new LinkedList<GId>();
        for (int i = 1; i <= 20; i++) {
            ManagedObjectRepresentation child = inventory.create(aSampleMo().build());
            creationOrder.add(child.getId());
            ManagedObjectReferenceRepresentation childRef = createChildRef(child);

            childrenAdder.addChild(childRef);
        }
        return creationOrder;
    }

    private ManagedObjectReferenceRepresentation createChildRef(ManagedObjectRepresentation child) {
        return anMoRefRepresentationLike(MO_REF_REPRESENTATION).withMo(child).build();
    }

    private List<GId> extractIDs(ManagedObjectReferenceCollectionRepresentation refCollection) {
        List<ManagedObjectReferenceRepresentation> refs = refCollection.getReferences();
        List<GId> children = new LinkedList<GId>();
        for (ManagedObjectReferenceRepresentation ref : refs) {
            children.add(ref.getManagedObject().getId());
        }
        return children;
    }

    private void deleteMOs(List<ManagedObjectRepresentation> mosOn1stPage) throws SDKException {
        for (ManagedObjectRepresentation mo : mosOn1stPage) {
            inventory.getManagedObject(mo.getId()).delete();
        }
    }

    private List<ManagedObjectRepresentation> getMOsFrom1stPage() throws SDKException {
        return inventory.getManagedObjects().get().getManagedObjects();
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    private interface ChildAdder {
        void addChild(ManagedObjectReferenceRepresentation ref) throws SDKException;
    }

    private interface ChildrenQuery {
        ManagedObjectReferenceCollectionRepresentation getChildren() throws SDKException;
    }

    private class ChildDeviceAdder implements ChildAdder {
        @Override
        public void addChild(ManagedObjectReferenceRepresentation ref) throws SDKException {
            parentMo.addChildDevice(ref);
        }
    }

    private class ChildAssetAdder implements ChildAdder {
        @Override
        public void addChild(ManagedObjectReferenceRepresentation ref) throws SDKException {
            parentMo.addChildAssets(ref);
        }
    }

    private class ChildAdditionAdder implements ChildAdder {
        @Override
        public void addChild(ManagedObjectReferenceRepresentation ref) throws SDKException {
            parentMo.addChildAdditions(ref);
        }
    }

    private class ChildDevicesQuery implements ChildrenQuery {
        @Override
        public ManagedObjectReferenceCollectionRepresentation getChildren() throws SDKException {
            return parentMo.get().getChildDevices();
        }
    }

    private class ChildAssetsQuery implements ChildrenQuery {
        @Override
        public ManagedObjectReferenceCollectionRepresentation getChildren() throws SDKException {
            return parentMo.get().getChildAssets();
        }
    }

    private class ChildAdditionsQuery implements ChildrenQuery {
        @Override
        public ManagedObjectReferenceCollectionRepresentation getChildren() throws SDKException {
            return parentMo.get().getChildAdditions();
        }
    }
}
