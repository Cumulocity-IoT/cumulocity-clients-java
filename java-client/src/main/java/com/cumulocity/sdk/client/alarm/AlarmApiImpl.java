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

package com.cumulocity.sdk.client.alarm;

import java.util.Map;
import java.util.Objects;

import com.cumulocity.model.cep.ProcessingMode;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmMediaType;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.alarm.AlarmsApiRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.UrlProcessor;
import com.cumulocity.sdk.client.buffering.Future;
import com.cumulocity.sdk.client.interceptor.service.ProcessingModeService;

public class AlarmApiImpl extends ProcessingModeService implements AlarmApi {

    private final RestConnector restConnector;

    private final int pageSize;

    private AlarmsApiRepresentation alarmsApiRepresentation;
    
    private UrlProcessor urlProcessor;

    public AlarmApiImpl(RestConnector restConnector, UrlProcessor urlProcessor, AlarmsApiRepresentation alarmsApiRepresentation, int pageSize) {
        super(restConnector);
        this.restConnector = restConnector;
        this.urlProcessor = urlProcessor;
        this.alarmsApiRepresentation = alarmsApiRepresentation;
        this.pageSize = pageSize;
    }

    private AlarmsApiRepresentation getAlarmsApiRepresentation() throws SDKException {
        return alarmsApiRepresentation;
    }
    
    @Override
    public AlarmRepresentation getAlarm(GId alarmId) throws SDKException {
        String url = getSelfUri() + "/" + alarmId.getValue();
        return restConnector.get(url, AlarmMediaType.ALARM, AlarmRepresentation.class);
    }

    @Override
    @Deprecated
    public AlarmRepresentation updateAlarm(AlarmRepresentation alarmToUpdate) throws SDKException {
        return update(alarmToUpdate);
    }
    
    @Override
    public AlarmRepresentation update(AlarmRepresentation alarmToUpdate) throws SDKException {
        String url = getSelfUri() + "/" + alarmToUpdate.getId().getValue();
        return restConnector.put(url, AlarmMediaType.ALARM, prepareForUpdate(alarmToUpdate));
    }

    private AlarmRepresentation prepareForUpdate(AlarmRepresentation alarm) {
        AlarmRepresentation updatable = new AlarmRepresentation();
        updatable.setStatus(alarm.getStatus());
        updatable.setSeverity(alarm.getSeverity());
        updatable.setSource(alarm.getSource());
        updatable.setText(alarm.getText());
        updatable.setAttrs(alarm.getAttrs());
        return updatable;
    }

    @Override
    public AlarmCollection getAlarms() throws SDKException {
        String url = getSelfUri();
        return new AlarmCollectionImpl(restConnector, url, pageSize);
    }

    private String getSelfUri() throws SDKException {
        return getAlarmsApiRepresentation().getAlarms().getSelf();
    }

    @Override
    public AlarmRepresentation create(AlarmRepresentation representation) throws SDKException {
        return restConnector.post(getSelfUri(), AlarmMediaType.ALARM, representation);
    }
    
    @Override
    public Future createAsync(AlarmRepresentation representation) throws SDKException {
        return restConnector.postAsync(getSelfUri(), AlarmMediaType.ALARM, representation);
    }

    @Override
    public AlarmCollection getAlarmsByFilter(AlarmFilter filter) throws SDKException {
        if (filter == null) {
            return getAlarms();
        }
        Map<String, String> params = filter.getQueryParams();
        return new AlarmCollectionImpl(restConnector, urlProcessor.replaceOrAddQueryParam(getSelfUri(), params), pageSize);
    }

    @Override
    public void deleteAlarmsByFilter(AlarmFilter filter) throws IllegalArgumentException, SDKException {

        if (filter == null) {
            throw new IllegalArgumentException("Alarm filter is null");
        } else {
            Map<String, String> params = filter.getQueryParams();
            restConnector.delete(urlProcessor.replaceOrAddQueryParam(getSelfUri(), params));
        }
    }

    @Override
    protected boolean isSupportedProcessingMode(ProcessingMode processingMode) {
        if (Objects.isNull(processingMode)) {
            return false;
        }
        if (ProcessingMode.TRANSIENT.equals(processingMode)) {
            return true;
        }
        return false;
    }
}
