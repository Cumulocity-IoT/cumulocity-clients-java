# Writing your own LPWAN Custom Codec Microservice using lpwan-custom-codec library

## Introduction

Cumulocity IoT has the ability to integrate LPWAN devices via LPWAN agents, for a list of all supported LPWAN agents see the [Protocol integration guide] (https://cumulocity.com/guides/protocol-integration/overview/).

The following section explains how to implement a custom codec microservice for implementing LPWAN device specific codec for decoding and encoding the device payload.

## Codec Workflow

First you need to deploy the codec microservice into Cumulocity IoT where the codec microservice creates a set of unique device protocols based on the device manufacturer and model information.

#### The REST endpoint: /decode

LPWAN codec microservice automatically exposes one REST endpoint using the path */decode*.

#### Request JSON body format

Below you see the request JSON input accepted by the /decode endpoint:

```
{
    "sourceDeviceId":"<<device Id>>",
   	"value":"<<The value to be decoded (hex string)",
   	"args": {
   		"deviceModel": "<<device model>>",
   		"deviceManufacturer": "<<device manufacturer>>",
   		"sourceDeviceEui": "<<device external Id>>",
   		"fport": <<the target fport>>
   	}
}
```

The LPWAN agent passes in the following fragments to the codec microservice:

* *args* - Meta information that is required by codec microservice to know the model and manufacturer of the device, along with the EUI of the device and the target fport.
* *sourceDeviceId* - The ID of the source device in the Cumulocity IoT inventory.
* *value* - The actual value to be decoded. The value is a series of bytes encoded as a hexadecimal string.

**Example:**

```
{
    "sourceDeviceId": "1025"
    "value": "202355251812984589",
    "args": {
        "deviceModel": "Asset Tracker",
        "deviceManufacturer": "LANSITEC",
        "sourceDeviceEui": "AA02030405060708",
        "fport": 100
    }
}
```

#### Response JSON body format

Following is the response JSON format emitted by the /decode endpoint:

```
{
  "alarms": [<<Array of alarms to be created>>],
  "alarmTypesToUpdate": [<<Array of alarm types to be updated>>],
  "events": [<<Array of events to be created >>],
  "dataFragments" : [Map <<fragment-path>,<Value>>],
  "success": true || false,
  "measurements": [<<Array of measurements to be created>>]
}
```

The fragments above are used as follows:

* *alarms* - A list of alarms to be created by the LPWAN agent. The alarms have to be given in the ordinary [Cumulocity IoT alarm JSON format](https://cumulocity.com/guides/reference/alarms/).
* *events* - A list of events to be created by the LPWAN agent. The events have to be given in the ordinary [Cumulocity IoT event JSON format](https://cumulocity.com/guides/reference/events/).
* *alarmTypesToUpdate* - A list of alaram types to be updated by LPWAN agent.
* *dataFragments* - The data fragments can be used by a decoder to hand over a set of fragment updates.
* *success* - An informative boolean flag (true or false) that indicates if decoding by the microservice was successful.
* *measurements* - A list of measurements to be created by the LPWAN agent. The syntax here follows an own DTO format. See the example below:

**Full decoder response sample**

```json
 {
  "events": [
    {
      "source": {
        "id": "3626"
      },
      "type": "Tracker status",
      "time": "2022-04-14T06:33:53.559Z",
      "text": "GPSSTATE: NO_SIGNAL\nVIBSTATE: 0\nCHGSTATE: POWER_CABLE_DISCONNECTED"
    }
  ],
  "measurements": [
    {
      "series": "c8y_Battery",
      "values": [
        {
          "value": 100,
          "unit": "%",
          "seriesName": "level"
        }
      ],
      "additionalProperties": {},
      "time": "2022-04-14T06:33:53.560Z",
      "type": "c8y_Battery",
      "includeDeviceName": false
    },
    {
      "series": "Tracker Signal Strength",
      "values": [
        {
          "value": 0,
          "unit": "dBm",
          "seriesName": "rssi"
        }
      ],
      "additionalProperties": {},
      "time": "2022-02-08T07:15:23.433Z",
      "type": "Tracker Signal Strength",
      "includeDeviceName": false
    },
    {
      "series": "Tracker Signal Strength",
      "values": [
        {
          "value": 0,
          "unit": "dBm",
          "seriesName": "snr"
        }
      ],
      "additionalProperties": {},
      "time": "2022-02-08T07:15:23.433Z",
      "type": "Tracker Signal Strength",
      "includeDeviceName": false
    }
  ]
}
```

#### The REST endpoint: /encode

LPWAN codec microservice automatically exposes one REST endpoint using the path */encode*.

#### Request JSON body format

Following is the request JSON input accepted by the /encode endpoint:

```
{
    "sourceDeviceId":"<<device Id>>",
    "commandName":"<<name of the command to be encoded>>",
    "commandData":"<<text of the command to be encoded>>",
    "args":{
        "deviceModel": "<<device model>>",
   		"deviceManufacturer": "<<device manufacturer>>",
   		"sourceDeviceEui": "<<device external Id>>",
   		"fport": <<the target fport>>
    }
}
```

The LPWAN agent populates the following fragments while invoking the codec microservice:

* *args* - Meta information that is required by codec microservice to know the model and manufacturer of the device, along with the EUI of the device and the target fport.
* *sourceDeviceId* - The ID of the source device in the Cumulocity IoT inventory.
* *commandName* - The name of the command to be encoded.
* *commandData* - The text of the command to be encoded.

**Example:**

```
{
      "commandName": "position request",
      "commandData" : {position request -latitude 10.25 -longitude -5.67},
      "sourceDeviceId" : 26413,
      "args" : {
        "deviceModel": "Asset Tracker",
        "deviceManufacturer": "LANSITEC",
        "sourceDeviceEui": "AABB03AABB030000",
        "fport": 100
      }
}
```

#### Response JSON body format

Following is the response JSON format emitted by the /encode endpoint:

```
{
    "encodedCommand": <<the encoded hexadecimal command>>,
    "fport": <<the target fport>>
}
```

The fragments above are used as follows:

* *encodedCommand* - The hexadecimal command obtained post encode, which will be executed as an operation.
* *fport* - The target fport.

```json
{
  "encodedCommand": "9F000000",
  "properties": {
    "fport": "100"
  },
  "message": "Successfully encoded the payload",
  "success": true
}
```

## Steps to implement LPWAN codec microservice:

The codec microservice can be easily built on top of [Cumulocity IoT Microservices](http://www.cumulocity.com/guides/microservice-sdk/java).
In order to serve as a LPWAN codec microservice, two requirements have to be met:

1. The codec microservice Main class needs to be annotated as:
   
```java
@CodecMicroserviceApplication `com.cumulocity.microservice.lpwan.codec.annotation.CodecMicroserviceApplication`
```   

2. The microservice needs to provide implementation for the following interfaces.

 ```java
package com.cumulocity.microservice.lpwan.codec;
 
public interface Codec {
    @NotNull @NotEmpty Set<DeviceInfo> supportsDevices();
}
```

```java
package com.cumulocity.microservice.customdecoders.api.service;

public interface DecoderService {
    DecoderResult decode(String inputData, GId deviceId, Map<String, String> args) throws DecoderServiceException;
}

```

```java
package com.cumulocity.microservice.customdecoders.api.service;

public interface EncoderService {
    EncoderResult encode(EncoderInputData encoderInputData) throws EncoderServiceException;
}
```

## Sample codec microservice implementation details

In this repository, you'll find a very straightforward codec example, the lansitec codec (`lora-codec-lansitec`). It is implemented using Spring Boot.

Follow the steps below while implementing the microservice:

1. Annotate the Main class with `@CodecMicroserviceApplication`.

```java
@CodecMicroserviceApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
```

2. Implement the `Codec` interface and supply the list of supported devices.

```java
@Component
public class LansitecCodec implements Codec {
    public Set<DeviceInfo> supportsDevices() {
        
        DeviceCommand positionRequestCommand = new DeviceCommand(LansitecEncoder.POSITION_REQUEST, "Device Config", LansitecEncoder.POSITION_REQUEST);
        DeviceCommand deviceRequestCommand = new DeviceCommand(LansitecEncoder.DEVICE_REQUEST, "Device Config", LansitecEncoder.DEVICE_REQUEST);
        DeviceCommand registerRequestCommand = new DeviceCommand(LansitecEncoder.REGISTER_REQUEST, "Device Config", LansitecEncoder.REGISTER_REQUEST);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode deviceOperationElements = mapper.createObjectNode();
        deviceOperationElements.put("breakpoint", Boolean.TRUE);
        deviceOperationElements.put("selfadapt", Boolean.TRUE);
        deviceOperationElements.put("oneoff", Boolean.TRUE);
        deviceOperationElements.put("alreport", Boolean.TRUE);
        deviceOperationElements.put("pos", 0);
        deviceOperationElements.put("hb", 0);
        String json = null;
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(deviceOperationElements);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        DeviceCommand setConfigCommand = new DeviceCommand(LansitecEncoder.SET_CONFIG, "Device Config", json);

        Set<DeviceCommand> deviceCommands = new HashSet<>();
        deviceCommands.add(positionRequestCommand);
        deviceCommands.add(deviceRequestCommand);
        deviceCommands.add(registerRequestCommand);
        deviceCommands.add(setConfigCommand);

        DeviceInfo deviceInfo_Lansitec_Asset_Tracker = new DeviceInfo("LANSITEC", "Asset Tracker", deviceCommands);
        DeviceInfo deviceInfo_Lansitec_Temperature_Sensor = new DeviceInfo("LANSITEC", "Temperature Sensor");

        return Stream.of(deviceInfo_Lansitec_Asset_Tracker, deviceInfo_Lansitec_Temperature_Sensor).collect(Collectors.toCollection(HashSet::new));
    }
}
```

3. Implement `DecoderService` interface.

```java
@Component
public class LansitecDecoder implements DecoderService {
    @Override
    public DecoderResult decode(String inputData, GId deviceId, Map<String, String> args) throws DecoderServiceException {

        // Create an LpwanDecodeInputData object to get selected device information like manufacturer and model
        LpwanDecoderInputData decoderInput = new LpwanDecoderInputData(inputData, deviceId, args);

        // Sample decoding logic
        try {
            // DecoderResult will contain the list of measurements, events, alarms and/or alarmTypes to Update.
            DecoderResult decoderResult =  process(decoderInputData);
        } catch(Exception e) {
            // Create an alarm on the device, so the decoder issue is shown as an alarm
            DecoderResult decoderResult = new DecoderResult();
            AlarmRepresentation alarm = new AlarmRepresentation();
            alarm.setSource(ManagedObjects.asManagedObject(deviceId));
            alarm.setType("DecoderError");
            alarm.setSeverity(CumulocitySeverities.CRITICAL.name());
            alarm.setText(e.getMessage());
            alarm.setDateTime(DateTime.now());
            decoderResult.addAlarm(alarm, true);

            throw new DecoderServiceException(e, e.getMessage(), decoderResult);
        }
        return decoderResult;

    }
}
```

A flexible option named `success` is provided in the DecoderResult which represents whether the `decode` operation is successful or not.

4. Implement `EncoderService` interface.

```java
@Component
public class LansitecEncoder implements EncoderService {
    public static final String SET_CONFIG = "set config";
    public static final String DEVICE_REQUEST = "device request";
    public static final String REGISTER_REQUEST = "register request";
    public static final String POSITION_REQUEST = "position request";

    @Override
    public EncoderResult encode(EncoderInputData encoderInputData) throws EncoderServiceException {
        LpwanEncoderInputData lpwanEncoderInputData = new LpwanEncoderInputData(GId.asGId(encoderInputData.getSourceDeviceId()),
                encoderInputData.getCommandName(),
                encoderInputData.getCommandData(),
                encoderInputData.getArgs());

        LpwanEncoderResult encoderResult = null;
        if (lpwanEncoderInputData.getSourceDeviceInfo().getManufacturer().equalsIgnoreCase("Lansitec") && lpwanEncoderInputData.getSourceDeviceInfo().getModel().equals("Asset Tracker")) {
            ObjectMapper mapper = new ObjectMapper();
            String payload = null;
            try {
                if (lpwanEncoderInputData.getCommandName().equals(POSITION_REQUEST)) {
                    payload = "A1FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(REGISTER_REQUEST)) {
                    payload = "A2FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(DEVICE_REQUEST)) {
                    payload = "A3FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(SET_CONFIG)) {
                    JsonNode params = mapper.readTree(lpwanEncoderInputData.getCommandData());
                    ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                    byte breakpoint = params.get("breakpoint").asBoolean() ? (byte) 8 : 0;
                    byte selfadapt = params.get("selfadapt").asBoolean() ? (byte) 4 : 0;
                    byte oneoff = params.get("oneoff").asBoolean() ? (byte) 2 : 0;
                    byte alreport = params.get("alreport").asBoolean() ? (byte) 1 : 0;
                    buffer.put((byte) ((byte) 0x90 | (byte) breakpoint | (byte) selfadapt | (byte) oneoff | (byte) alreport));
                    buffer.putShort((short) params.get("pos").asInt());
                    buffer.put((byte) params.get("hb").asInt());
                    payload = BaseEncoding.base16().encode(buffer.array());
                }

                encoderResult = new LpwanEncoderResult(payload, 20);
                encoderResult.setSuccess(true);
                encoderResult.setMessage("Successfully encoded the payload");
            } catch (IOException e) {
                e.printStackTrace();
                encoderResult = new LpwanEncoderResult();
                encoderResult.setSuccess(false);
                encoderResult.setMessage("Encoding Payload Failed");
            }
        }
        return encoderResult;
    }
}
```

5. Add the following permissions in the cumulocity.json file. Here's how the cumulocity.json looks like

```json
    {
        "apiVersion":"1",
        "version":"1.0-SNAPSHOT",
        "contextPath": "lora-codec-lansitec",
        "provider": {
            "name":"Cumulocity GmbH"
        },
        "isolation":"MULTI_TENANT",
        "requiredRoles": [
            "ROLE_INVENTORY_ADMIN",
            "ROLE_INVENTORY_READ"
        ]
    }
```

## Deploying the example codec microservice

1. Clone the https://github.com/Cumulocity-IoT/cumulocity-examples.git repository.
2. Build the microservice using `mvn clean install`. This creates a ZIP file of the lanitec codec microservice.
3. Deploy the microservice by uploading the ZIP file using the Cumulocity IoT Administration UI.
4. Open the Device Management application. Under **Device protocols**, you can now see the device types with type "lpwan" created by the lansitec codec microservice.

## Developing a microservice without lpwan-custom-codec

If the user does not use the lpwan-custom-codec, then the custom microservice developed by the user must adhere to the prerequisite task performed by the lpwan-custom-codec, that is, the user must create the device types and predefined commands to use the custom codec feature seamlessly. 
