package com.cumulocity.sdk.client.inventory;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import org.apache.commons.codec.Resources;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class BinariesApiIT extends JavaSdkITBase {

    final BinariesApi binariesApi = platform.getBinariesApi();

    //smoke test
    @Test
    public void shouldUploadFile() {
        // given
        ManagedObjectRepresentation container = new ManagedObjectRepresentation();
        configureMockedContainer(container, "java-sdk_sample_binary");

        InputStream file = getFileInputStream("sampleTestFile.txt");
        // when
        ManagedObjectRepresentation uploaded = binariesApi.uploadFile(container, file);
        // then
        assertThat(uploaded).isNotNull();
        assertThat(uploaded.get("contentType")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    public void shouldUploadFileWithCustomMimeType() {
        // given
        ManagedObjectRepresentation container = new ManagedObjectRepresentation();
        configureMockedContainer(container, "java-sdk_sample_binary");

        InputStream file = getFileInputStream("sampleTestFile.txt");
        // when
        ManagedObjectRepresentation uploaded = binariesApi.uploadFile(container, file, MediaType.TEXT_PLAIN_TYPE);
        // then
        assertThat(uploaded).isNotNull();
        assertThat(uploaded.get("contentType")).isEqualTo(MediaType.TEXT_PLAIN);
    }

    @Test
    public void shouldUploadBinaryData() {
        // given
        byte[] binaryData = {0, 1, 2, 4, 5, 6};
        ManagedObjectRepresentation container = new ManagedObjectRepresentation();
        configureMockedContainer(container, "java-sdk_binary_upload");
        // when
        ManagedObjectRepresentation uploaded = binariesApi.uploadFile(container, binaryData);
        // then
        assertThat(uploaded).isNotNull();
        assertThat(uploaded.getId()).isNotNull();
        assertThat(uploaded.getType()).isEqualTo(container.getType());
        assertThat(uploaded.getName()).isEqualTo(container.getName());
        assertThat(uploaded.get("length")).isEqualTo((long) binaryData.length);
        assertThat(uploaded.get("domain")).isEqualTo("iot.cumulocity.com");
        assertThat(uploaded.get("contentType")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    public void shouldReplaceFile() throws IOException {
        // given
        InputStream inputStream = getFileInputStream("sampleTestFile.txt");
        InputStream replaceStream = getFileInputStream("sampleTestFileSecond.txt");
        GId gId = uploadBinaryFile(inputStream);
        // when
        ManagedObjectRepresentation replaced = binariesApi.replaceFile(gId, MediaType.APPLICATION_OCTET_STREAM, replaceStream);
        // then
        assertThat(replaced).isNotNull();
        assertThat(replaced.get("contentType")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(replaced.get("length")).isEqualTo((long) getFileInputStream("sampleTestFileSecond.txt").available());
    }

    @Test
    public void shouldNotReplaceNonExistingFile() {
        // given
        InputStream replaceStream = getFileInputStream("sampleTestFile.txt");
        GId gId = GId.asGId("non-existing");
        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> binariesApi.replaceFile(gId, MediaType.APPLICATION_OCTET_STREAM, replaceStream));
        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    @Test
    public void shouldDeleteFile() {
        // given
        InputStream inputStream = getFileInputStream("sampleTestFile.txt");
        GId gId = uploadBinaryFile(inputStream);
        // when
        binariesApi.deleteFile(gId);
        //then
        SDKException sdkException = assertThrows(SDKException.class, () -> binariesApi.downloadFile(gId));
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    @Test
    public void shouldNotRemoveNonExistingFile() {
        // given
        GId gId = GId.asGId("not-existing");
        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> binariesApi.deleteFile(gId));
        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    @Test
    public void shouldDownloadFile() {
        // given
        InputStream inputStream = getFileInputStream("sampleTestFile.txt");
        GId gId = uploadBinaryFile(inputStream);
        // when
        InputStream downloaded = binariesApi.downloadFile(gId);
        // then
        assertThat(downloaded).hasSameContentAs(getFileInputStream("sampleTestFile.txt"));
    }

    @Test
    public void shouldNotDownloadNonExistingFile() {
        // given
        GId gId = GId.asGId("Non-existing");
        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> binariesApi.downloadFile(gId));
        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    private GId uploadBinaryFile(InputStream inputStream) {
        ManagedObjectRepresentation container = new ManagedObjectRepresentation();
        configureMockedContainer(container, "java-sdk_sample_binary");
        return binariesApi.uploadFile(container, inputStream).getId();
    }


    private static void configureMockedContainer(ManagedObjectRepresentation container, String name) {
        container.setName(name);
        container.setType("cumulocity_Binary");
        container.set("iot.cumulocity.com", "domain");
    }

    private InputStream getFileInputStream(String fileName){
        return Resources.getInputStream("binaries/" + fileName);
    }
}
