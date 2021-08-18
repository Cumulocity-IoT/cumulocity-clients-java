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
package com.cumulocity.sdk.client.cep;

import com.cumulocity.rest.representation.cep.CepModuleRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.cep.notification.CepCustomNotificationsSubscriber;

import java.io.InputStream;

/**
 * API for integration with Custom Event Processing modules from the platform.
 *
 */
public interface CepApi {

    /**
     * Gets the notifications subscriber, which allows to receive notifications sent from cep.
     * <pre>
     * <code>
     * Example:
     *
     *  Subscriber&lt;String, Object&gt; subscriber = deviceControlApi.getNotificationsSubscriber();
     *
     *  subscriber.subscribe( "channelId" , new SubscriptionListener&lt;String, Object&gt;() {
     *
     *      {@literal @}Override
     *      public void onNotification(Subscription&lt;GId&gt; subscription, Object operation) {
     *             //process notification from cep module
     *      }
     *
     *      {@literal @}Override
     *      public void onError(Subscription&lt;GId&gt; subscription, Throwable ex) {
     *          // handle subscribe error
     *      }
     *  });
     *  </code>
     *  </pre>
     *
     * @return subscriber
     * @throws SDKException when subscriber creation fail
     */
    CepCustomNotificationsSubscriber getCustomNotificationsSubscriber();

    /**
     * Gets an cep module by id
     *
     * @param id of the cep module to search for
     * @return the cep module with the given id
     * @throws SDKException if the cep module is not found or if the query failed
     */
    CepModuleRepresentation get(String id);

    /**
     * Gets a cep module text by id
     *
     * @param id of the cep module to search for
     * @return the cep module text
     * @throws SDKException if the cep module is not found or if the query failed
     */
    String getText(String id);

    /**
     * Creates an cep module in the platform.
     *
     * @param content input stream to resource with cep module definition
     * @return the created cep module with the generated id
     * @throws SDKException if the cep module could not be created
     */
    @Deprecated
    CepModuleRepresentation create(InputStream content);

    /**
     * Creates an cep module in the platform.
     *
     * @param content of cep module definition
     * @return the created cep module with the generated id
     * @throws SDKException if the cep module could not be created
     */
    CepModuleRepresentation create(String content);
    /**
     * Updates an cep module in the platform.
     * The cep module to be updated is identified by the id.
     *
     * @param id of cep module to update
     * @param content input stream to resource with cep module definition
     * @return the updated cep module
     * @throws SDKException if the cep module could not be updated
     */
    CepModuleRepresentation update(String id, InputStream content);

    CepModuleRepresentation update(String id, String content);

    CepModuleRepresentation update(CepModuleRepresentation module);

    /**
     * Gets all cep modules from the platform
     *
     * @return collection of cep modules with paging functionality
     * @throws SDKException if the query failed
     */
    CepModuleCollection getModules();

    /**
     * Deletes the cep module from the platform.
     *
     * @param module cep module to delete
     * @throws SDKException when delete of cep module fail
     */
    void delete(CepModuleRepresentation module);

    /**
     * Deletes the cep module from the platform.
     *
     * @param id identifier of cep module to delete
     * @throws SDKException when delete of cep module fail
     */
    void delete(String id);

    /**
     * Checks state of cep microservice.
     * @param clazz expected class result
     * @param <T> generic type of class result
     * @return the cep health status object
     */
    <T> T health(Class<T> clazz);
}
