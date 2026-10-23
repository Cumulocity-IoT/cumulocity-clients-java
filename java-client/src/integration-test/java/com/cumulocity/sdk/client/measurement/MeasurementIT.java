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
package com.cumulocity.sdk.client.measurement;

import com.cumulocity.model.DateConverter;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementCollectionRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.cumulocity.model.DateConverter.string2Date;
import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.awaitility.Awaitility.await;


//TODO inline step definitions (see AlarmIT or InventoryIT)
@Slf4j
public class MeasurementIT extends JavaSdkITBase {

    private static List<ManagedObjectRepresentation> managedObjects = new ArrayList<>();

    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int UNPROCESSABLE = 422;
    // Measurement indexing can occasionally lag more than 10s under load on the test backend,
    // so allow a longer window than the other eventual-consistency checks in this class.
    private static final Duration MEASUREMENT_INDEXING_TIMEOUT = Duration.ofSeconds(30);

    private List<MeasurementRepresentation> input;
    private List<MeasurementRepresentation> result1;
    private List<MeasurementRepresentation> result2;
    private MeasurementCollectionRepresentation collection1;
    private MeasurementApi measurementApi;
    private TenantOptionApi tenantOptionApi;
    private int status;
    private boolean removeTimeseriesOption;

    @BeforeEach
    public void setup() throws Exception {
        measurementApi = platform.getMeasurementApi();
        tenantOptionApi = platform.getTenantOptionApi();
        input = new ArrayList<>();
        result1 = new ArrayList<>();
        result2 = new ArrayList<>();
        status = OK;
        removeTimeseriesOption = false;
    }

    @AfterEach
    public void deleteTenantOptionWhenPresent() {
        if(removeTimeseriesOption) {
            try {
                tenantOptionApi.delete(new OptionPK("configuration", "timeseries.mongodb.collections.mode"));
            } catch (SDKException e) {
                log.error("Timeseries tenant option deletion failed with status code: {} and reason: {}", e.getHttpStatus(), e.getMessage());
            }
        }
    }

    @AfterEach
    public void clearManagedObjects() {
        for(ManagedObjectRepresentation mo : managedObjects) {
            platform.getInventoryApi().delete(mo.getId());
        }
        managedObjects = new ArrayList<>();
    }

    @Test
    public void createMeasurements() {
        // given
        createSingleMOWithName("1MO");
        iHaveMeasurements(2, "com.type1");
        // when
        iCreateAllMeasurements();
        // then
        allShouldBeCreated();
    }

    @Test
    public void createMeasurementsWithoutTime() {
        // given
        createSingleMOWithName("2MO");
        iHaveAMeasurementWithNoTime("com.type2");
        // when
        iCreateAllMeasurements();
        // then
        shouldBeBadRequest();
    }

    @Test
    public void getMeasurementCollectionByFragmentType() throws Exception {
        // given
        createSingleMOWithName("3MO");
        iHaveMeasurementsWithFragments(2, "com.cumulocity.sdk.client.measurement.FragmentOne");
        iHaveMeasurementsWithFragments(3, "com.cumulocity.sdk.client.measurement.FragmentTwo");
        // when
        iCreateAllMeasurements();
        iQueryAllByFragmentType("com.cumulocity.sdk.client.measurement.FragmentOne");
        // then
        iShouldGetNumberOfMeasurements(2);
        // when
        iQueryAllByFragmentType("com.cumulocity.sdk.client.measurement.FragmentTwo");
        // then
        iShouldGetNumberOfMeasurements(3);
        // when
        iQueryAllByFragmentType("com.cumulocity.sdk.client.measurement.FragmentThree");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurementCollectionBySource() {
        // given
        createMOWithPrefix("4MO", 3);
        iHaveMeasurementsForSource(1, "4MO0");
        iHaveMeasurementsForSource(2, "4MO1");
        // when
        iCreateAllMeasurements();
        allShouldBeCreated();
        // then
        iShouldEventuallyGetNumberOfMeasurements(1, "4MO0");
        iShouldEventuallyGetNumberOfMeasurements(2, "4MO1");
        iShouldEventuallyGetNumberOfMeasurements(0, "4MO2");
    }

    @Test
    public void getMeasurementCollectionByTime() throws Exception {
        // given
        createSingleMOWithName("5MO");
        iHaveAMeasurementWithTypeAndTime("2010-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "5MO");
        iHaveAMeasurementWithTypeAndTime("2010-11-03T11:05:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "5MO");
        // when
        iCreateAllMeasurements();
        iQueryAllByTime("2010-11-03T11:00:00.000+05:30", "2010-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(2);
        // when
        iQueryAllByTime("2010-11-03T10:00:00.000+05:30", "2010-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurementCollectionBySourceAndTime() throws Exception {
        // given
        createMOWithPrefix("6MO", 2);
        iHaveAMeasurementWithTypeAndTime("2011-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "6MO0");
        iHaveAMeasurementWithTypeAndTime("2011-11-03T11:05:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "6MO1");
        // when
        iCreateAllMeasurements();
        iQueryAllBySourceAndTime("6MO0", "2011-11-03T11:00:00.000+05:30", "2011-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceAndTime("6MO1", "2011-11-03T11:00:00.000+05:30", "2011-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceAndTime("6MO0", "2011-11-03T10:00:00.000+05:30", "2011-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceAndTime("6MO1", "2011-11-03T10:00:00.000+05:30", "2011-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurementCollectionBySourceAndFragmentType() throws Exception {
        // given
        createMOWithPrefix("7MO", 2);
        iHaveAMeasurementWithTypeAndTime("2012-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "7MO0");
        iHaveAMeasurementWithTypeAndTime("2012-11-03T11:05:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "7MO1");
        // when
        iCreateAllMeasurements();
        iQueryAllBySourceAndType("7MO0", "com.cumulocity.sdk.client.measurement.FragmentOne");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceAndType("7MO1", "com.cumulocity.sdk.client.measurement.FragmentOne");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceAndType("7MO0", "com.cumulocity.sdk.client.measurement.FragmentTwo");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceAndType("7MO1", "com.cumulocity.sdk.client.measurement.FragmentTwo");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurementCollectionByFragmentTypeAndTime() throws Exception {
        // given
        createMOWithPrefix("8MO", 2);
        iHaveAMeasurementWithTypeAndTime("2013-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "8MO0");
        iHaveAMeasurementWithTypeAndTime("2013-11-03T11:05:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "8MO1");
        // when
        iCreateAllMeasurements();
        iQueryAllByTypeAndTime("com.cumulocity.sdk.client.measurement.FragmentOne", "2013-11-03T11:00:00.000+05:30",
                "2013-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(2);
        // when
        iQueryAllByTypeAndTime("com.cumulocity.sdk.client.measurement.FragmentTwo", "2013-11-03T11:00:00.000+05:30",
                "2013-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllByTypeAndTime("com.cumulocity.sdk.client.measurement.FragmentOne", "2013-11-03T10:00:00.000+05:30",
                "2013-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurementCollectionBySourceAndFragmentTypeAndTime() throws Exception {
        // given
        createMOWithPrefix("9MO", 3);
        iHaveAMeasurementWithTypeAndTime("2014-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "9MO0");
        iHaveAMeasurementWithTypeAndTime("2014-11-03T11:05:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "9MO1");
        // when
        iCreateAllMeasurements();
        iQueryAllBySourceTypeAndTime("9MO0", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceTypeAndTime("9MO0", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO0", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO0", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO1", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(1);
        // when
        iQueryAllBySourceTypeAndTime("9MO1", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO1", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO1", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO2", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO2", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T11:00:00.000+05:30",
                "2014-11-03T11:10:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO2", "com.cumulocity.sdk.client.measurement.FragmentOne", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
        // when
        iQueryAllBySourceTypeAndTime("9MO2", "com.cumulocity.sdk.client.measurement.FragmentTwo", "2014-11-03T10:00:00.000+05:30",
                "2014-11-03T11:00:00.000+05:30");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void getMeasurement() throws Exception {
        // given
        createSingleMOWithName("10MO");
        iHaveAMeasurementWithTypeAndTime("2011-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "10MO");
        // when
        iCreateAllMeasurements();
        iGetMeasurementWithCreatedId();
        // then
        shouldGetTheMeasurement();
    }

    @Test
    public void deleteMeasurement() throws Exception {
        // given
        createSingleMOWithName("11MO");
        disableTimeseries();
        iHaveAMeasurementWithTypeAndTime("2011-11-03T11:01:00.000+05:30", "com.cumulocity.sdk.client.measurement.FragmentOne", "11MO");
        // when
        iCreateAllMeasurements();
        iDeleteMeasurementWithCreatedId();
        iGetMeasurementWithCreatedId();
        // then
        shouldNotBeFound();
    }

    @Test
    public void deleteMeasurementCollectionByEmptyFilter() {
        // given
        createSingleMOWithName("12MO");
        iHaveMeasurements(3, "com.type3");
        iHaveMeasurements(2, "com.type4");
        iCreateAllMeasurements();
        allShouldBeCreated();
        iShouldEventuallyGetNumberOfMeasurements(5, "12MO");
        // when
        iDeleteMeasurementCollection();
        // then
        iShouldEventuallyGetNumberOfMeasurements(0, "12MO");
    }

    @Test
    public void deleteMeasurementsByTypeFilter() {
        // given
        createSingleMOWithName("13MO");
        iHaveMeasurements(3, "com.type5");
        iHaveMeasurements(2, "com.type6");
        iCreateAllMeasurements();
        allShouldBeCreated();
        iShouldEventuallyGetNumberOfMeasurements(5, "13MO");
        // when
        iDeleteMeasurementsByType("com.type6");
        // then
        iShouldEventuallyGetNumberOfMeasurements(3, "13MO");
        // when
        iQueryAllByType("com.type5");
        // then
        iShouldGetNumberOfMeasurements(3);
        // when
        iQueryAllByType("com.type6");
        // then
        iShouldGetNumberOfMeasurements(0);
    }

    @Test
    public void createMeasurementsInBulk() {
        // given
        createSingleMOWithName("14MO");
        iHaveMeasurements(3, "com.type7");
        // when
        iCreateAllBulk();
        // then
        allShouldBeCreated();
    }

    @Test
    public void getMeasurementCollectionByDefaultPageSettings() {
        // given
        createSingleMOWithName("15MO");
        for (int i = 0; i < 12; i++) {
            MeasurementRepresentation rep = aSampleMeasurement(getMoWithName("15MO"));
            measurementApi.create(rep);
        }
        // when
        MeasurementCollectionRepresentation measurements = measurementApi.getMeasurements().get();
        // then
        assertThat(measurements.getMeasurements()).hasSize(5);
        // when
        MeasurementCollectionRepresentation page1st = measurementApi.getMeasurements().getPage(measurements, 1);
        // then
        assertThat(page1st.getMeasurements()).hasSize(5);
        // when
        MeasurementCollectionRepresentation page2nd = measurementApi.getMeasurements().getPage(measurements, 2);
        // then
        assertThat(page2nd.getMeasurements()).hasSize(5);
    }

    // ------------------------------------------------------------------------
    // given
    // ------------------------------------------------------------------------

    private MeasurementRepresentation aSampleMeasurement(ManagedObjectRepresentation source) {
        MeasurementRepresentation rep = new MeasurementRepresentation();
        rep.setDateTime(new DateTime());
        rep.setType("com.type1");
        rep.setSource(source);

        rep.set(new FragmentOne());
        return rep;
    }

    private void iHaveMeasurements(int n, String type) {
        for (int i = 0; i < n; i++) {
            MeasurementRepresentation rep = new MeasurementRepresentation();
            rep.setType(type);
            rep.setDateTime(new DateTime());
            rep.setSource(managedObjects.get(0));
            input.add(rep);
        }
    }

    private void iHaveMeasurementsWithFragments(int n, String fragmentType)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        for (int i = 0; i < n; i++) {
            MeasurementRepresentation rep = new MeasurementRepresentation();
            rep.setDateTime(new DateTime());
            rep.setType("com.type1");
            rep.setSource(managedObjects.get(0));

            // Set fragment
            Class<?> cls = Class.forName(fragmentType);
            Object fragment = cls.newInstance();
            rep.set(fragment);

            input.add(rep);
        }
    }

    private ManagedObjectRepresentation getMoWithName(String moName) {
        return managedObjects.stream().filter(moRep -> moRep.getName().equalsIgnoreCase(moName)).findFirst().get();
    }

    private void iHaveMeasurementsForSource(int measurementNumber, String moName) {
        for (int i = 0; i < measurementNumber; i++) {
            MeasurementRepresentation rep = new MeasurementRepresentation();
            rep.setDateTime(new DateTime());
            rep.setType("com.type1");
            rep.setSource(getMoWithName(moName));
            input.add(rep);
        }
    }

    private void iHaveAMeasurementWithNoTime(String type) {
        MeasurementRepresentation rep = new MeasurementRepresentation();
        rep.setType(type);
        rep.setSource(managedObjects.get(0));
        input.add(rep);
    }

    private void iHaveAMeasurementWithTypeAndTime(String time, String fragmentType, String moName)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        MeasurementRepresentation rep = new MeasurementRepresentation();
        rep.setType("com.type1");
        rep.setDateTime(DateTime.parse(time));
        rep.setSource(getMoWithName(moName));

        // Set fragment
        Class<?> cls = Class.forName(fragmentType);
        Object fragment = cls.newInstance();
        rep.set(fragment);

        input.add(rep);
    }

    private void disableTimeseries() {
        OptionRepresentation disableTimeseries = OptionRepresentation.asOptionRepresentation("configuration", "timeseries.mongodb.collections.mode", "DISABLED");
        tenantOptionApi.save(disableTimeseries);
        removeTimeseriesOption = true;
    }

    // ------------------------------------------------------------------------
    // when
    // ------------------------------------------------------------------------

    private void iCreateAllMeasurements() throws SDKException {
        try {
            for (MeasurementRepresentation rep : input) {
                result1.add(measurementApi.create(rep));
            }
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
            log.error("Measurement creation failed with status code: {}, msg: {}", status, ex.getMessage());
        }
    }

    private void iCreateAllBulk() throws SDKException {
        try {
            MeasurementCollectionRepresentation collection = new MeasurementCollectionRepresentation();
            collection.setMeasurements(input);
            result1.addAll(measurementApi.createBulk(collection).getMeasurements());
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iGetMeasurementWithCreatedId() throws SDKException {
        try {
            result2.add(measurementApi.getMeasurement(result1.get(0).getId()));
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iDeleteMeasurementWithCreatedId() throws SDKException {
        try {
            measurementApi.delete(result1.get(0));
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iDeleteMeasurementCollection() throws SDKException {
        try {
            measurementApi.deleteMeasurementsByFilter(new MeasurementFilter().byFromDate(string2Date("2000-01-01T00:00:00.000+00:00")));
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iDeleteMeasurementsByType(String type) throws SDKException {
        try {
            MeasurementFilter typeFilter = new MeasurementFilter().byType(type).byFromDate(string2Date("2000-01-01T00:00:00.000+00:00"));
            measurementApi.deleteMeasurementsByFilter(typeFilter);
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllByType(String type) throws SDKException {
        try {
            MeasurementFilter typeFilter = new MeasurementFilter().byType(type);
            collection1 = measurementApi.getMeasurementsByFilter(typeFilter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllByFragmentType(String fragmentType) throws SDKException, ClassNotFoundException {
        try {
            Class<?> fragmentClass = Class.forName(fragmentType);
            MeasurementFilter filter = new MeasurementFilter().byFragmentType(fragmentClass);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllByTime(String from, String to) throws SDKException {
        try {
            Date fromDate = DateConverter.string2Date(from);
            Date toDate = DateConverter.string2Date(to);
            MeasurementFilter filter = new MeasurementFilter().byDate(fromDate, toDate);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllBySourceAndTime(String moName, String from, String to) throws SDKException {
        try {
            GId sourceId = getMoWithName(moName).getId();
            Date fromDate = DateConverter.string2Date(from);
            Date toDate = DateConverter.string2Date(to);
            MeasurementFilter filter = new MeasurementFilter().byDate(fromDate, toDate).bySource(sourceId);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllBySourceAndType(String moName, String fragmentType) throws SDKException, ClassNotFoundException {
        try {
            Class<?> fragmentClass = Class.forName(fragmentType);
            GId sourceId = getMoWithName(moName).getId();
            MeasurementFilter filter = new MeasurementFilter().byFragmentType(fragmentClass).bySource(sourceId);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllByTypeAndTime(String fragmentType, String from, String to) throws SDKException, ClassNotFoundException {
        try {
            Class<?> fragmentClass = Class.forName(fragmentType);
            Date fromDate = DateConverter.string2Date(from);
            Date toDate = DateConverter.string2Date(to);
            MeasurementFilter filter = new MeasurementFilter().byDate(fromDate, toDate).byFragmentType(fragmentClass);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    private void iQueryAllBySourceTypeAndTime(String moName, String fragmentType, String from, String to)
            throws SDKException, ClassNotFoundException {
        try {
            Class<?> fragmentClass = Class.forName(fragmentType);
            GId sourceId = getMoWithName(moName).getId();
            Date fromDate = DateConverter.string2Date(from);
            Date toDate = DateConverter.string2Date(to);
            MeasurementFilter filter = new MeasurementFilter().bySource(sourceId).byDate(fromDate, toDate).byFragmentType(fragmentClass);
            collection1 = measurementApi.getMeasurementsByFilter(filter).get();
        } catch (SDKException ex) {
            status = ex.getHttpStatus();
        }
    }

    // ------------------------------------------------------------------------
    // then
    // ------------------------------------------------------------------------
    private void allShouldBeCreated() {
        assertThat(result1).hasSameSizeAs(input);
        for (MeasurementRepresentation rep : result1) {
            assertThat(rep.getId()).isNotNull();
        }
    }

    private void iShouldGetNumberOfMeasurements(int count) {
        assertThat(collection1.getMeasurements()).hasSize(count);
    }

    private void iShouldEventuallyGetNumberOfMeasurements(int count, String moName) {
        GId sourceId = getMoWithName(moName).getId();
        MeasurementFilter filter = new MeasurementFilter().bySource(sourceId);
        await().atMost(MEASUREMENT_INDEXING_TIMEOUT).untilAsserted(() ->
                assertThat(measurementApi.getMeasurementsByFilter(filter).get().getMeasurements()).hasSize(count));
    }

    private void shouldBeBadRequest() {
        assertThat(status)
                .describedAs("HTTP status is 422 Unprocessable Content")
                .isEqualTo(UNPROCESSABLE);
    }

    private void shouldGetTheMeasurement() {
        assertThat(result1)
                .describedAs("has first element ID equal to %s", result2.get(0).getId())
                .satisfies(
                        representation -> assertThat(representation.getId()).isEqualTo(result2.get(0).getId()),
                        atIndex(0));
    }

    private void shouldNotBeFound() {
        assertThat(status)
                .describedAs("HTTP status is 404 Not Found")
                .isEqualTo(NOT_FOUND);
    }

    private void createMOWithPrefix(String moPrefix, int moNumber) {
        for (int i = 0; i < moNumber; ++i) {
            createSingleMOWithName(moPrefix + i);
        }
    }

    private void createSingleMOWithName(String moName) {
        ManagedObjectRepresentation mo = platform.getInventoryApi().create(aSampleMo().withName(moName).build());
        managedObjects.add(mo);
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }
}
