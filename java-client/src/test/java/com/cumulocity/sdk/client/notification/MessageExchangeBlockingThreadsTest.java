package com.cumulocity.sdk.client.notification;

import com.cumulocity.rest.providers.CumulocityJSONMessageBodyReader;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.sdk.client.notification.MessageExchange.ResponseHandler;
import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.async.FutureListener;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;
import org.cometd.bayeux.Message;
import org.cometd.client.transport.TransportListener;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.Callables.returning;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MessageExchangeBlockingThreadsTest {

    private ScheduledExecutorService executorService = newScheduledThreadPool(1);

    @Test(timeout = 2000)
    public void shouldNotBlockedThreadWhenTryingToReadResponse() throws Exception {
        //given
        final Client client = mock(Client.class);
        final TransportListener listener = mock(TransportListener.class);
        final MessageExchange messageExchange = new MessageExchange(mock(CumulocityLongPollingTransport.class), client, executorService, listener, mock(ConnectionHeartBeatWatcher.class), mock(UnauthorizedConnectionWatcher.class));

        givenUnfinishedClientResponse(client);

        //when
        messageExchange.execute("", "");
        messageExchange.cancel();

        //then
        executorShouldHaveFreeThread();
        verify(listener).onConnectException(any(Throwable.class), any(Message[].class));
        verify(listener, never()).onException(any(Throwable.class), any(Message[].class));
    }

    private void givenUnfinishedClientResponse(final Client client) throws Exception {
        final AsyncWebResource asyncWebResource = mock(AsyncWebResource.class);
        final InputStream blockOnRead = new InputStream() {
            @Override
            public synchronized int read() throws IOException {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                return 0;
            }

            @Override
            public synchronized void close() throws IOException {
            }
        };

        given(client.asyncResource(anyString())).willReturn(asyncWebResource);
        given(asyncWebResource.handle(any(ClientRequest.class), any(FutureListener.class))).will(new Answer<Future<ClientResponse>>() {
            @Override
            public Future<ClientResponse> answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final MessageBodyWorkers workers = mock(MessageBodyWorkers.class);
                final MessageBodyReader messageBodyReader = mock(MessageBodyReader.class);
                
                given(messageBodyReader.readFrom(any(Class.class), any(Type.class), any(Annotation[].class), any(MediaType.class), any(MultivaluedMap.class), any(InputStream.class))).willAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        final CumulocityJSONMessageBodyReader reader = new CumulocityJSONMessageBodyReader();
                        return reader.readFrom(BaseResourceRepresentation.class, null, null, null, null,blockOnRead);
                    }
                });
                given(workers.getMessageBodyReader(any(Class.class), any(Type.class), any(Annotation[].class), any(MediaType.class))).willReturn(messageBodyReader);
                
                final ClientResponse response = new ClientResponse(OK.getStatusCode(), new InBoundHeaders(), blockOnRead, workers);
                final FutureTask<ClientResponse> futureTask = new FutureTask<ClientResponse>(returning(response)) {
                    protected void done() {
                        try {
                            final ResponseHandler responseHandler = (ResponseHandler) invocationOnMock.getArguments()[1];
                            responseHandler.onComplete(this);
                        } catch (InterruptedException e) {
                            propagate(e);
                        }
                    }
                };
                executorService.schedule(futureTask, 0, MILLISECONDS).get();
                return futureTask;
            }
        });
    }

    private void executorShouldHaveFreeThread() throws Exception {
        final Future<String> task = executorService.submit(new Callable<String>() {
            @Override
            public String call() {
                return "OK";
            }
        });
        Assertions.assertThat(task.get(500, MILLISECONDS)).isEqualTo("OK");
    }

}