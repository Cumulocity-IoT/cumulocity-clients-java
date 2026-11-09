package com.cumulocity.agent.packaging.microservice.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.Sets;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MicroserviceDockerClientImplTest {

    private final int TEST_IMAGE_SIZE = 100 * FileUtils.ONE_MB;

    @Mock
    DockerClient dockerClientMock;

    @InjectMocks
    MicroserviceDockerClientImpl dockerClient;

    @TempDir
    File temporaryDirectory;

    @Mock
    BuildImageCmd buildImageCmd;

    private String dockerDir;
    private String networkMode;
    private Map<String, String> buildArgs;
    private Set<String> tags;

    @Test
    void buildDockerImageWithoutError() {

        //When I mock the Docker java client

        prepareDockerClientMockForBuild();
        pepareBuildCommandMock(false);

        //and I build the image with our client
        dockerClient.buildDockerImage(dockerDir, tags, buildArgs, "amd64", networkMode, 60, new AuthConfigurations(), false);

        //then docker was instructed to build the file and parameters are passed into the build command.
        verify(dockerClientMock, times(1)).buildImageCmd(eq(new File(dockerDir)));
        verify(buildImageCmd,times(1)).withPlatform("amd64");
        verify(buildImageCmd).withTags(tags);
        verify(buildImageCmd).withNetworkMode(networkMode);
        for (Map.Entry<String, String> entry: buildArgs.entrySet()) {
            verify(buildImageCmd).withBuildArg(entry.getKey(), entry.getValue());
        }
    }

    @Test
    void buildDockerImageWithoutCache() {
        prepareDockerClientMockForBuild();
        pepareBuildCommandMock(false);
        when(buildImageCmd.withNoCache(true)).thenReturn(buildImageCmd);
        when(buildImageCmd.withPull(true)).thenReturn(buildImageCmd);

        dockerClient.buildDockerImage(dockerDir, tags, buildArgs, "amd64", networkMode, 60, new AuthConfigurations(), true);

        verify(buildImageCmd).withNoCache(true);
        verify(buildImageCmd).withPull(true);
    }

    @Test
    void buildDockerImageWithCache() {
        prepareDockerClientMockForBuild();
        pepareBuildCommandMock(false);

        dockerClient.buildDockerImage(dockerDir, tags, buildArgs, "amd64", networkMode, 60, new AuthConfigurations(), false);

        verify(buildImageCmd, never()).withNoCache(anyBoolean());
        verify(buildImageCmd, never()).withPull(anyBoolean());
    }

    @Test
    void buildDockerImageWithError() {

        //When I mock the Docker java client

        prepareDockerClientMockForBuild();
        pepareBuildCommandMock(true);

        //and I build the image with our client, then the docker error is propagated

        assertThrows(RuntimeException.class, () -> dockerClient.buildDockerImage(dockerDir, tags, buildArgs, "amd64", networkMode, 60, new AuthConfigurations(), false));

        verify(dockerClientMock, times(1)).buildImageCmd(eq(new File(dockerDir)));
    }

    private void pepareBuildCommandMock(boolean generateError) {
        //mock a three step build process, possibly with an error in the end.
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenAnswer(mockInvocation -> {

            BuildImageResultCallback imageResultCallback = mockInvocation.getArgument(0);

            imageResultCallback.onNext(mockBuildProgressItem("Progress 1"));
            imageResultCallback.onNext(mockBuildProgressItem("Progress 2"));
            imageResultCallback.onNext(mockBuildProgressItem("Progress 3"));
            if (!generateError) {
                imageResultCallback.onNext(mockCompleteProgressItem());
            } else {
                imageResultCallback.onNext(mockErrorItem());
            }
            return null;


        });
        when(dockerClientMock.buildImageCmd(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withPlatform(any())).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any())).thenReturn(buildImageCmd);
        when(buildImageCmd.withBuildArg(any(),any())).thenReturn(buildImageCmd);
        when(buildImageCmd.withBuildAuthConfigs(any())).thenReturn(buildImageCmd);
    }

    private void prepareDockerClientMockForBuild() {
        dockerDir = "/foo/bar";
        tags = Sets.newHashSet("tag1", "tag2", "tag3");
        buildArgs = Maps.newHashMap();
        buildArgs.put("IMAGEARCH", "amd64");
        buildArgs.put("hey", "yolo");
        networkMode = "none";

        when(buildImageCmd.withNetworkMode(any())).thenReturn(buildImageCmd);
    }

    private BuildResponseItem mockBuildProgressItem(String text) {
        BuildResponseItem buildResponseItem = mock(BuildResponseItem.class);
        when(buildResponseItem.getStream()).thenReturn(text);
        return buildResponseItem;
    }

    private BuildResponseItem mockCompleteProgressItem() {
        BuildResponseItem buildResponseItem = mock(BuildResponseItem.class);
        when(buildResponseItem.getStream()).thenReturn("Finished");
        when(buildResponseItem.isBuildSuccessIndicated()).thenReturn(true);
        when(buildResponseItem.getImageId()).thenReturn("ABCDEF4");
        return buildResponseItem;
    }


    private BuildResponseItem mockErrorItem() {
        BuildResponseItem buildResponseItem = mock(BuildResponseItem.class);
        when(buildResponseItem.getStream()).thenReturn("Simulated Docker Error");
        when(buildResponseItem.isErrorIndicated()).thenReturn(true);
        ResponseItem.ErrorDetail errorDetail = mock(ResponseItem.ErrorDetail.class);
        when(errorDetail.getMessage()).thenReturn("Simulated Docker Error");
        when(errorDetail.getCode()).thenReturn(777);
        when(buildResponseItem.getErrorDetail()).thenReturn(errorDetail);

        return buildResponseItem;
    }

    @SneakyThrows
    @Test
    void saveDockerImage() {

        //When I mock a docker image in memory
        byte[] mockImage = new byte[TEST_IMAGE_SIZE];
        Random random = new Random();
        random.nextBytes(mockImage);
        String sourceImageDigest = Hex.encodeHexString(DigestUtils.getSha256Digest().digest(mockImage));
        log.info("Created mock image in memory with size {}, digest {}", mockImage.length, sourceImageDigest);

        //and I tell the mocked docker client to present this as an output stream for the saved image
        SaveImageCmd saveImageCmd = mock(SaveImageCmd.class);
        when(saveImageCmd.exec()).thenReturn(new ByteArrayInputStream(mockImage));
        when(dockerClientMock.saveImageCmd("test-image")).thenReturn(saveImageCmd);

        File targetFile = new File(temporaryDirectory.getAbsoluteFile(),"test-image.bin");

        //and I tell our implementation client to save this image to a temporary target file.
        dockerClient.saveDockerImage("test-image", new FileOutputStream(targetFile));

        //then the target file contains the exact bytes and the hashes are the same.
        String targetImageDigest = Hex.encodeHexString(DigestUtils.digest(DigestUtils.getSha256Digest(), targetFile));

        Assertions.assertEquals(sourceImageDigest, targetImageDigest, "Image data corrupted by save method!");


    }

    @Test
    void testDeleteAll() {

        //When I have a image called testimage mocked
        Image image = getMockedImage();
        RemoveImageCmd removeImageCmd = mockAndGetRemoveImageCmd();

        ListImagesCmd listImagesCmd = mock(ListImagesCmd.class);
        when(listImagesCmd.getFilters()).thenReturn(Maps.newHashMap());
        when(listImagesCmd.exec()).thenReturn(Lists.newArrayList(image));
        when(dockerClientMock.listImagesCmd()).thenReturn(listImagesCmd);

        //and I delete it
        dockerClient.deleteAll("testimage", true);
        //then our client calls the corresponding remove command.
        verify(removeImageCmd, times(1)).exec();


    }

    private RemoveImageCmd mockAndGetRemoveImageCmd() {
        RemoveImageCmd removeImageCmd = mock(RemoveImageCmd.class);
        when(removeImageCmd.getImageId()).thenReturn("testimage");
        when(removeImageCmd.withForce(any())).thenReturn(removeImageCmd);
        when(dockerClientMock.removeImageCmd("testimage")).thenReturn(removeImageCmd);
        return removeImageCmd;
    }

    private Image getMockedImage() {
        Image image = mock(Image.class);
        when(image.getId()).thenReturn("testimage");
        when(image.getRepoTags()).thenReturn(new String[]{"A", "B", "C"});
        return image;
    }

    @Test
    void tagImage() {

        TagImageCmd tagImageCmd = mock(TagImageCmd.class);
        when(dockerClientMock.tagImageCmd(any(), any(), any())).thenAnswer(invocationOnMock -> tagImageCmd);

        //make sure our docker client passes the tagging command to docker and executes it
        dockerClient.tagImage("hello", "bla/hello", "world");
        verify(tagImageCmd, times(1)).exec();

    }

    @Test
    void pushImageWithoutError() {

        //when I mock a push process
        PushImageCmd pushImageCmd = mock(PushImageCmd.class);
        when(pushImageCmd.exec(any())).thenAnswer(invocationOnMock -> {
            ResultCallback resultCallback = invocationOnMock.getArgument(0);
            resultCallback.onNext(getMockedPushResponseItem("Pushing"));
            resultCallback.onNext(getMockedPushResponseItem("Still pushing"));
            resultCallback.onComplete();
            return null;
        });

        when(dockerClientMock.pushImageCmd("testimage2")).thenReturn(pushImageCmd);

        //and I then ask our client to push this image
        dockerClient.pushImage("testimage2");
        //then our push image command is executed on the docker side
        verify(pushImageCmd, times(1)).exec(any());

    }

    @Test
    void pushImageWithError() {
        //when I mock a push process
        PushImageCmd pushImageCmd = mock(PushImageCmd.class);
        when(pushImageCmd.exec(any())).thenAnswer(invocationOnMock -> {
            ResultCallback resultCallback = invocationOnMock.getArgument(0);
            resultCallback.onError(new RuntimeException("Push failed"));
            return null;
        });

        when(dockerClientMock.pushImageCmd("testimage2")).thenReturn(pushImageCmd);

        //and I then ask our client to push this image
        assertThrows(RuntimeException.class, () -> dockerClient.pushImage("testimage2"));
        //then our push image command is executed on the docker side
        verify(pushImageCmd, times(1)).exec(any());

    }

    private PushResponseItem getMockedPushResponseItem(String stream) {
        PushResponseItem pushResponseItem = mock(PushResponseItem.class);
        when(pushResponseItem.getStream()).thenReturn(stream);
        return pushResponseItem;
    }


}