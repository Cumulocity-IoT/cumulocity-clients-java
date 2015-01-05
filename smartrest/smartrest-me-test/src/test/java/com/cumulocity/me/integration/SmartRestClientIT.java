package com.cumulocity.me.integration;

import java.io.IOException;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cumulocity.smartrest.client.SmartConnection;
import com.cumulocity.smartrest.client.SmartRequest;
import com.cumulocity.smartrest.client.SmartResponse;
import com.cumulocity.smartrest.client.SmartResponseEvaluator;
import com.cumulocity.smartrest.client.impl.SmartHttpConnection;
import com.cumulocity.smartrest.client.impl.SmartRequestImpl;
import com.cumulocity.smartrest.util.MeHttpConnectionFactory;

public class SmartRestClientIT {
    
    private static String templateString =  "10,100,GET,/inventory/managedObjects/&&,,,&&,,\n" +
                                              "10,101,GET,/inventory/managedObjects/,,,,,\n" +
                                              "10,200,POST,/inventory/managedObjects/,application/vnd.com.nsn.cumulocity.managedObject+json,application/vnd.com.nsn.cumulocity.managedObject+json,&&,,\"{\"\"c8y_J2METestFragment\"\":{},\"\"name\"\":\"\"&&\"\",\"\"c8y_IsDevice\"\":{},\"\"c8y_SupportedMeasurements\"\":[\"\"c8y_TemperatureMeasurement\"\"]}\"\n" +
                                              "10,201,POST,/measurement/measurements/,application/vnd.com.nsn.cumulocity.measurement+json,application/vnd.com.nsn.cumulocity.measurement+json,&&,,\"{\"\"time\"\":\"\"2013-06-22T17:03:14.000+02:00\"\",\"\"source\"\":{\"\"id\"\":\"\"&&\"\"},\"\"type\"\":\"\"c8y_TemperatureMeasurement\"\",\"\"c8y_TemperatureMeasurement\"\":{\"\"T\"\":{\"\"value\"\":&&,\"\"unit\"\":\"\"C\"\"}}}\"\n" +
                                              "10,999,DELETE,/inventory/managedObjects/&&,,,&&,,\n" +
                                              "11,300,,$.c8y_J2METestFragment,\"$.id\"\n" +
                                              "11,311,managedObjects,,\"$.name\",\"$.id\"\n" +
                                              "11,312,,$.c8y_TemperatureMeasurement,\"$.id\"";
                                              
    private static SmartConnection con = null;
    
    private static String xid = null;
    
    private static String deviceId = null;
    
    
    @BeforeClass
    public static void createConnection() throws IOException {
        Properties cumulocityProps = new Properties();
        cumulocityProps.load(SmartRestClientIT.class.getClassLoader().getResourceAsStream("cumulocity-test.properties"));
        SystemPropertiesOverrider p = new SystemPropertiesOverrider(cumulocityProps);
        // Create Connection
        con =  new SmartHttpConnection(
                p.get("cumulocity.host"),
                p.get("cumulocity.tenant"),
                p.get("cumulocity.user"),
                p.get("cumulocity.password"),
                p.get("cumulocity.xid"),
                new MeHttpConnectionFactory()
                );
        
        Assert.assertNotNull(con);
        
        // Register Templates
        xid = con.templateRegistration(templateString);
        
        Assert.assertNotNull(xid);
        System.out.println(xid);
        
        SmartResponse response = con.executeRequest(new SmartRequestImpl("200,j2meTestDevice"));
        deviceId = response.getDataRows()[0].getData()[0];
        System.out.println(deviceId);
        
        Assert.assertNotNull(deviceId);
        
    }
    
    @Test
    public void testRequest() {
        String body = "100,"+xid;
        SmartRequest request = new SmartRequestImpl(body);
        SmartResponse resp = con.executeRequest(request);
        Assert.assertThat(resp.getDataRows().length, Matchers.is(Matchers.equalTo(0)));
    }
    
    @Test
    public void testAsyncRequest() throws InterruptedException {
        SmartResponseEvaluator eval = new SmartResponseEvaluator() {
            public void evaluate(SmartResponse response) {
                Assert.assertThat(response.getDataRows().length, Matchers.is(Matchers.equalTo(0)));
            }
        };
        String body = "100,"+xid;
        SmartRequest request = new SmartRequestImpl(body);
        con.executeRequestAsync(request, eval);
        Thread.sleep(2000);
    }
    
    @Test
    public void createMeasurements() {
        String body1 = "201,"+deviceId+",23";
        String body2 = "201,"+deviceId+",24";
        String body3 = "201,"+deviceId+",25";
        
        SmartResponse resp1 = con.executeRequest(new SmartRequestImpl(body1));
        SmartResponse resp2 = con.executeRequest(new SmartRequestImpl(body2));
        SmartResponse resp3 = con.executeRequest(new SmartRequestImpl(body3));
        
        Assert.assertThat(resp1.getRow(0).getMessageId(), Matchers.is(Matchers.equalTo(312)));
        Assert.assertThat(resp2.getRow(0).getMessageId(), Matchers.is(Matchers.equalTo(312)));
        Assert.assertThat(resp3.getRow(0).getMessageId(), Matchers.is(Matchers.equalTo(312)));
    }
    
    @AfterClass
    public static void cleanUp() {

        // Delete device (includes deleting everything connected to it)
        con.executeRequest(new SmartRequestImpl("999," + deviceId));
        // Delete SmartREST template
        con.executeRequest(new SmartRequestImpl("999," + xid));
    }

}
