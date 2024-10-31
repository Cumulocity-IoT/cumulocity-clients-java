package com.cumulocity.sdk.client.notification;

import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.client.transport.TransportListener;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.Response;

import org.cometd.common.TransportException;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageExchangeTest {

    private static final String URL = "";

    private static final String MESSAGES = "";

    @Mock
    private CumulocityLongPollingTransport transport;

    @Mock
    private ConnectionHeartBeatWatcher watcher;

    @Mock
    private UnauthorizedConnectionWatcher unauthorizedConnectionWatcher;

    @Mock
    private Mutable message;

    @Mock
    private TransportListener listener;

    @Mock
    private Response response;

    @Mock
    private ScheduledExecutorService executorService;

    @Mock
    private Client client;

    @Mock
    private WebTarget resource;

    @Mock
    private AsyncInvoker asyncInvoker;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Future<Response> request;

    @Captor
    private ArgumentCaptor<InvocationCallback<Response>> responseHandler;

    @Captor
    private ArgumentCaptor<Runnable> task;

    private MessageExchange exchange;

    @BeforeEach
    public void setup() {
        exchange = new MessageExchange(transport, client, executorService, listener, watcher, unauthorizedConnectionWatcher, Arrays.asList(message));
        exchange.reconnectionWaitingTime = SECONDS.toMillis(1);
        when(client.target(any(String.class))).thenReturn(resource);
        when(resource.request(APPLICATION_JSON_TYPE)).thenReturn(builder);
        when(builder.async()).thenReturn(asyncInvoker);
        when(asyncInvoker.post(any(Entity.class), responseHandler.capture())).thenReturn(request);
    }

    @AfterEach
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void shouldStopWatcherAndNotifyWhenCancel() {
        //Given
        when(request.cancel(anyBoolean())).thenReturn(true);
        //When
        exchange.execute(URL, MESSAGES);
        exchange.cancel();
        //Then
        verify(listener).onFailure(any(RuntimeException.class), eq(Arrays.asList(message)));
        verifyNoMoreInteractions(listener);
        verify(watcher).stop();
    }

    @Test
    public void shouldScheduleTaskWhenReceiveResponse() throws InterruptedException, ExecutionException {

        //When
        receivedSuccessfulResponse();
        //Then
        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotifyAboutFailWhenRequestFailed() {

        //When
        receivedErrorResponse();
        //Then
        verify(listener).onFailure(any(Exception.class), eq(Arrays.asList(message)));
        verify(watcher).stop();
    }

    @Test
    public void shouldNotifyAboutFailWhenResponseStatusIsNotOk() throws InterruptedException, ExecutionException {
        //Given
        when(response.getStatusInfo()).thenReturn(Response.Status.FORBIDDEN);
        //When
        receivedSuccessfulResponse();
        responseConsumed();
        //Then
        verify(listener).onFailure(any(TransportException.class), eq(Arrays.asList(message)));
        verifyNoMoreInteractions(listener);
        verify(watcher).stop();
    }

    @Test
    public void shouldNotifyAboutHeartBeatsWhenReceivedFromServer() throws InterruptedException, ExecutionException {
        //Given
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.getEntity()).thenReturn(new ByteArrayInputStream(new byte[] { Ascii.SPACE, Ascii.SPACE, Ascii.BEL }));
        //When
        receivedSuccessfulResponse();
        responseConsumed();
        //Then
        verify(watcher, times(2)).heartBeat();
        verify(watcher).stop();
    }

    @Test
    public void shouldNotPassReceivedContentAsMessagesToListenerWhenResponseIsEmptyString() throws InterruptedException, ExecutionException {
        //Given
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.getEntity()).thenReturn(new ByteArrayInputStream(new byte[] { Ascii.SPACE, Ascii.SPACE, Ascii.BEL }));
        when(response.readEntity(String.class)).thenReturn("");
        //When
        receivedSuccessfulResponse();
        responseConsumed();
        //Then
        verify(listener, never()).onMessages(Mockito.anyList());
        verify(listener).onFailure(any(TransportException.class), eq(Arrays.asList(message)));
        verifyNoMoreInteractions(listener);
        verify(watcher).stop();
    }

    @Test
    public void shouldPassReceivedContentAsMessagesToListenerWhenResponseIsNotEmptyString() throws InterruptedException, ExecutionException, ParseException {
        //Given
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.getEntity()).thenReturn(new ByteArrayInputStream(new byte[] { Ascii.SPACE, Ascii.SPACE, Ascii.BEL }));
        when(response.readEntity(String.class)).thenReturn("non_empty_content");
        final ImmutableList<Mutable> messages = ImmutableList.of(mock(Mutable.class));
        when(transport.parseMessages("non_empty_content")).thenReturn(messages);
        //When
        receivedSuccessfulResponse();
        responseConsumed();
        //Then
        verify(listener).onMessages(messages);
        verifyNoMoreInteractions(listener);
        verify(watcher).stop();
    }

    @Test
    public void shouldRetryParsingMessageWhenSingleJsonElementIsBroken() throws ExecutionException, InterruptedException, ParseException {
        //Given
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.getEntity()).thenReturn(new ByteArrayInputStream(new byte[] { Ascii.SPACE, Ascii.SPACE, Ascii.BEL }));
        String invalidMessage = "[{\"data\":{\"creationTime\":\"2019-11-07T10:44:07.287Z\",\"deviceId\":\"564\",\"deviceName\":\"Linux MAC CA55B6FBB6F5\",\"self\":\"http://cumulocity.default.svc.cluster.local/devicecontrol/operations/713\",\"id\":\"713\",\n" +
                "\"status\":\"PENDING\",\"description\":\"Opening remote access tunnel to 'SSH1'\",\"c8y_Availability\": { \"status\": \"xxx\"}},\n" +
                "\"channel\":\"/564\",\"id\":\"462\"},{\"ext\":{\"ack\":3},\"channel\":\"/meta/connect\",\"id\":\"5\",\"successful\":true}]";
        when(response.readEntity(String.class)).thenReturn(invalidMessage);
        when(transport.parseMessages(any(String.class))).thenAnswer(new Answer<List<Mutable>>() {
            @Override
            public List<Mutable> answer(InvocationOnMock invocation) throws Throwable {
                String content = invocation.getArgument(0);
                return Arrays.asList(new ClientSvensonJSONContext(OperationRepresentation.class).parse(content));
            }
        });
        //When
        receivedSuccessfulResponse();
        responseConsumed();
        //Then
        verify(listener).onMessages((List<Mutable>) MockitoHamcrest.argThat(IsIterableWithSize.<Mutable>iterableWithSize(1)));
        verifyNoMoreInteractions(listener);
        verify(watcher).stop();
    }

    private void responseConsumed() {
        verify(executorService).submit(task.capture());
        task.getValue().run();
    }

    private void receivedResponse() {
        exchange.execute(URL, MESSAGES);
        verify(asyncInvoker).post(any(Entity.class), responseHandler.capture());
    }

    private void receivedSuccessfulResponse() {
        receivedResponse();
        responseHandler.getValue().completed(response);
    }

    private void receivedErrorResponse() {
        receivedResponse();
        responseHandler.getValue().failed(new RuntimeException());
    }
}
