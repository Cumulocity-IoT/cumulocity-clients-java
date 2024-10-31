package com.cumulocity.sdk.client.buffering;

import static com.cumulocity.sdk.client.buffering.FileBasedPersistentProvider.NEW_REQUESTS_PATH;
import static com.cumulocity.sdk.client.buffering.FileBasedPersistentProvider.NEW_REQUESTS_TEMP_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.File;
import java.nio.file.Path;

import jakarta.ws.rs.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cumulocity.rest.representation.alarm.AlarmMediaType;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;

public class FileBasedPersistentProviderTest {

    @TempDir
    Path testFolder;

    String pathToTempFolder;
    FileBasedPersistentProvider persistentProvider;

    @BeforeEach
    public void setup() {
        pathToTempFolder = testFolder.toString();
        persistentProvider = new FileBasedPersistentProvider(pathToTempFolder);
    }

    @Test
    public void shouldInitializeFiles() {
        assertThat(new File(pathToTempFolder + NEW_REQUESTS_TEMP_PATH)).exists();
        assertThat(new File(pathToTempFolder + NEW_REQUESTS_PATH)).exists();
    }

    @Test
    public void shouldInitializeRequestIdCounter() throws Exception {
        new File(pathToTempFolder + NEW_REQUESTS_PATH + "13").createNewFile();

        persistentProvider = new FileBasedPersistentProvider(pathToTempFolder);

        assertThat(persistentProvider.counter.get()).isGreaterThan(13);
    }

    @Test
    public void shouldPersistRequestToFile() {
        BufferedRequest request = BufferedRequest.create(HttpMethod.POST, "test", AlarmMediaType.ALARM, new AlarmRepresentation());

        persistentProvider.offer(new ProcessingRequest(1 ,request));

        assertThat(new File(pathToTempFolder + NEW_REQUESTS_PATH + "1")).exists();
    }

    @Test
    public void shouldNotCreateAFileWhenQueueIsFull() throws Exception {
        BufferedRequest request = BufferedRequest.create(HttpMethod.POST, "test", AlarmMediaType.ALARM, new AlarmRepresentation());
        persistentProvider = new FileBasedPersistentProvider(1, pathToTempFolder);

        Throwable thrown = catchThrowable(() -> {
            persistentProvider.offer(new ProcessingRequest(1 ,request));
            persistentProvider.offer(new ProcessingRequest(2 ,request));
        });
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Queue is full");
    }

    @Test
    public void shouldReturnRequestFromQueue() {
        BufferedRequest request = BufferedRequest.create(HttpMethod.POST, "test", AlarmMediaType.ALARM, new AlarmRepresentation());
        persistentProvider.offer(new ProcessingRequest(1 ,request));

        ProcessingRequest result = persistentProvider.poll();

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getEntity().getRepresentation()).isInstanceOf(AlarmRepresentation.class);
    }
}
