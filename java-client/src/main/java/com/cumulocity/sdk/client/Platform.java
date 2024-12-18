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

package com.cumulocity.sdk.client;

import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.audit.AuditRecordApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.event.EventBinaryApi;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.BinariesApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApi;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.option.SystemOptionApi;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.user.UserApi;

public interface Platform extends AutoCloseable {

    RestOperations rest();

    InventoryApi getInventoryApi() throws SDKException;

    IdentityApi getIdentityApi() throws SDKException;

    MeasurementApi getMeasurementApi() throws SDKException;

    DeviceControlApi getDeviceControlApi() throws SDKException;

    AlarmApi getAlarmApi() throws SDKException;

    EventApi getEventApi() throws SDKException;

    EventBinaryApi getEventBinaryApi() throws SDKException;

    AuditRecordApi getAuditRecordApi() throws SDKException;
    
    DeviceCredentialsApi getDeviceCredentialsApi() throws SDKException;
    
    BinariesApi getBinariesApi() throws SDKException;

    UserApi getUserApi() throws SDKException;

    TenantOptionApi getTenantOptionApi() throws SDKException;

    SystemOptionApi getSystemOptionApi() throws SDKException;

    TokenApi getTokenApi() throws SDKException;

    NotificationSubscriptionApi getNotificationSubscriptionApi() throws SDKException;

    void close();
}
