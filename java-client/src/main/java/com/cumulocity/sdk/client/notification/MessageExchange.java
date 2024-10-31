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
package com.cumulocity.sdk.client.notification;

import lombok.Synchronized;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.client.transport.TransportListener;
import org.cometd.common.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.svenson.JSON;
import org.svenson.JSONParser;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

class MessageExchange {

    private static final int ASCII_SPACE = 0x20;

    /* wait time in millis before trying to reconnect again */
    long reconnectionWaitingTime = SECONDS.toMillis(30);

    private final Logger log = LoggerFactory.getLogger(MessageExchange.class);

    private final CumulocityLongPollingTransport transport;

    private final TransportListener listener;

    private final List<Mutable> messages;

    private volatile Future<Response> request;

    private final ConnectionHeartBeatWatcher watcher;

    private final UnauthorizedConnectionWatcher unauthorizedConnectionWatcher;

    private final ScheduledExecutorService executorService;

    private final Client client;

    private final List<MessageExchangeListener> listeners = new LinkedList<>();

    private volatile Future<?> consumer;

    MessageExchange(CumulocityLongPollingTransport transport, Client client, ScheduledExecutorService executorService,
                    TransportListener listener, ConnectionHeartBeatWatcher watcher,
                    UnauthorizedConnectionWatcher unauthorizedConnectionWatcher, List<Mutable> messages) {
        this.transport = transport;
        this.client = client;
        this.executorService = executorService;
        this.listener = listener;
        this.messages = messages;
        this.watcher = watcher;
        this.unauthorizedConnectionWatcher = unauthorizedConnectionWatcher;
    }

    public void execute(String url, String content) {
        startWatcher();
        request = client.target(url)
                .request(APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(content, APPLICATION_JSON), new ResponseHandler());
    }

    private void startWatcher() {
        log.debug("starting heartbeat watcher {}", messages);
        watcher.start();
    }

    @Synchronized("messages")
    public void cancel() {
        log.debug("canceling {}", messages);


        if (request.cancel(true)) {
            listener.onFailure(new RuntimeException("request cancelled"), messages);
        } else {
            if (consumer != null) {
                consumer.cancel(true);
            }

            try {
                final Response response = request.get();
                if (response != null) {
                    response.close();
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("canceling failed ", e);
            }
        }
        onFinish();
    }

    private void onFinish() {
        for (MessageExchangeListener listener : listeners) {
            listener.onFinish();
        }

        log.debug("stopping heartbeat watcher {}", messages);
        watcher.stop();
    }

    public void addListener(MessageExchangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageExchangeListener listener) {
        listeners.remove(listener);
    }

    final class ResponseConsumer implements Runnable {

        private final Response response;

        public ResponseConsumer(Response response) {
            this.response = response;
        }

        @Override
        public void run() {
            try {
                heartBeatWatch(response);
                getMessagesFromResponse(response);
            } catch (Exception e) {
                onConnectionFailed(e);
            } finally {
                try {
                    onFinish();
                } finally {
                    response.close();
                }
            }
        }

        private void heartBeatWatch(Response clientResponse) throws IOException {
            if (isOk(clientResponse)) {
                InputStream responseStream = (InputStream) clientResponse.getEntity();
                log.debug("getting heartbeats  {}", clientResponse);
                getHeartBeats(responseStream);
            }
        }

        private boolean isOk(Response clientResponse) {
            return clientResponse.getStatusInfo().toEnum() == Response.Status.OK;
        }

        private void getHeartBeats(final InputStream entityInputStream) throws IOException {
            entityInputStream.mark(MAX_VALUE);
            int value = -1;
            while ((value = entityInputStream.read()) >= 0) {
                if (isHeartBeat(value)) {
                    log.debug("received heartbeat");
                    watcher.heartBeat();
                    entityInputStream.mark(MAX_VALUE);
                } else {
                    log.debug("new messages received");
                    entityInputStream.reset();
                    break;
                }
            }
        }

        private boolean isNullOrEmpty(String content) {
            return content == null || content.length() == 0;
        }

        private boolean isHeartBeat(int value) {
            return value == ASCII_SPACE;
        }

        private void getMessagesFromResponse(Response clientResponse) {
            if (isOk(clientResponse)) {
                String content = clientResponse.readEntity(String.class);
                if (!isNullOrEmpty(content)) {
                    try {
                        handleContent(content);
                    } catch (ParseException | IllegalArgumentException x) {
                        log.debug("Failed to parse message: {}, will retry.", content);
                        if (!retryHandleContent(content)) {
                            onException(x);
                        }
                    }
                } else {
                    onTransportException(204);
                }
            } else {
                onTransportException(clientResponse.getStatus());
            }
        }

        private void onException(Exception x) {
            log.debug("request failed ", x);
            waitBeforeAnotherReconnect();
            listener.onFailure(x, messages);
        }

        private void onException(final int code) {
            Map<String, Object> failure = new HashMap<>(2);
            failure.put("httpCode", code);
            onException(new TransportException(failure));
        }

        private void waitBeforeAnotherReconnect() {
            try {
                Thread.sleep(reconnectionWaitingTime);
            } catch (InterruptedException e) {
                log.error("Problem occurred while waiting for another bayeux reconnect");
            }
        }

        private void onTransportException(int code) {
            log.debug("request failed with code {}", code);
            if (code == 401) {
                unauthorizedConnectionWatcher.unauthorizedAccess();
                if (unauthorizedConnectionWatcher.shouldRetry()) {
                    onException(code);
                }
            } else {
                onException(code);
            }
        }

        private void onConnectionFailed(Exception e) {
            log.error("connection failed " + e.getMessage(), e);

            unauthorizedConnectionWatcher.resetCounter();
            listener.onFailure(e, messages);
        }

        private void handleContent(String content) throws ParseException {
            List<Mutable> messages = transport.parseMessages(content);
            log.debug("Received messages {}", messages);
            listener.onMessages(messages);
        }

        /**
         * try parse each message from json array separately
         * continue when single message parsing fail
         */
        private boolean retryHandleContent(String content) {
            List<?> jsonArray = JSONParser.defaultJSONParser().parse(List.class, content);
            List<Mutable> messages = new ArrayList<>(jsonArray.size());
            for (Object jsonObject : jsonArray) {
                try {
                    messages.addAll(transport.parseMessages(JSON.defaultJSON().forValue(jsonObject)));
                } catch (ParseException | IllegalArgumentException e) {
                    log.debug("Failed to retry parse json message: {}", e.getMessage());
                }
            }
            log.debug("Messages recovered after failure content handle: {}", messages);
            if (messages.isEmpty()) {
                return false;
            }
            listener.onMessages(messages);
            return true;
        }
    }

    final class ResponseHandler implements InvocationCallback<Response> {

        @Override
        public void completed(Response clientResponse) {
            try {
                synchronized (messages) {
                    log.debug("received response headers {} ", messages);
                    consumer = executorService.submit(new ResponseConsumer(clientResponse));
                }
            } catch (Exception e) {
                handleException(e);
            }
        }

        @Override
        public void failed(Throwable throwable) {
            handleException(throwable);
        }

        private void handleException(Throwable e) {
            log.debug("connection failed", e);
            unauthorizedConnectionWatcher.resetCounter();
            listener.onFailure(e, messages);
            onFinish();
        }
    }

}
