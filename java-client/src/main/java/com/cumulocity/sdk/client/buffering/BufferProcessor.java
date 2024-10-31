package com.cumulocity.sdk.client.buffering;

import static com.cumulocity.sdk.client.ResponseParser.NO_ERROR_REPRESENTATION;

import java.net.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;

public class BufferProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BufferProcessor.class);

    private ExecutorService executor;

    private PersistentProvider persistentProvider;

    private RestConnector restConnector;

    private BufferRequestService service;

    public BufferProcessor(PersistentProvider persistentProvider, BufferRequestService service, RestConnector restConnector) {
        this.persistentProvider = persistentProvider;
        this.service = service;
        this.restConnector = restConnector;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("buffering-process");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void startProcessing() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        ProcessingRequest processingRequest = persistentProvider.poll();
                        service.addResponse(processingRequest.getId(), sendRequest(processingRequest.getEntity()));

                    }
                } catch (Exception ex) {
                    Throwable cause = ex;

                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    if (cause instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    } else {
                        throw ex;
                    }
                }
            }

            private Result sendRequest(BufferedRequest httpPostRequest) {
                Result result = new Result();
                while (true) {
                    try {
                        Object response = doSendRequest(httpPostRequest);
                        result.setResponse(response);
                        return result;
                    } catch (SDKException e) {
                        if (e.getHttpStatus() <= 500 && !e.getMessage().contains(NO_ERROR_REPRESENTATION)) {
                            result.setException(e);
                            return result;
                        }
                        //platform is down
                        LOG.warn("Couldn't connect to platform. Waiting..." + e.getMessage());
                        waitForPlatform();
                    } catch (ProcessingException e) {
                        if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                            //lack of connection
                            LOG.warn("Couldn't connect to platform. Waiting..." + e.getMessage());
                            waitForConnection();
                        } else {
                            result.setException(new RuntimeException("Exception occurred while processing buffered request: ", e));
                            return result;
                        }
                    } catch (Exception e) {
                        result.setException(new RuntimeException("Exception occurred while processing buffered request: ", e));
                        return result;
                    }
                }
            }

            private void waitForPlatform() {
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("", e);
                }
            }

            private void waitForConnection() {
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("", e);
                }
            }

            private Object doSendRequest(BufferedRequest httpPostRequest) {
                String method = httpPostRequest.getMethod();
                if (HttpMethod.POST.equals(method)) {
                    return restConnector.post(httpPostRequest.getPath(), httpPostRequest.getMediaType(), httpPostRequest.getRepresentation());
                } else if (HttpMethod.PUT.equals(method)) {
                    return restConnector.put(httpPostRequest.getPath(), httpPostRequest.getMediaType(), httpPostRequest.getRepresentation());
                } else {
                    throw new IllegalArgumentException("This method is not supported in buffering processor: " + method);
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

}
