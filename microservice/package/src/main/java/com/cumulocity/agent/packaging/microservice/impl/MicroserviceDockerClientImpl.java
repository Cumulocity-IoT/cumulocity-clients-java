package com.cumulocity.agent.packaging.microservice.impl;

import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * This class implements glue code between our package plugin logic and the Docker Java plugin.
 * Please note: The methods have been marked as synchronized to prevent multiple maven threads
 * to issue tasks to the Docker daemon in parallel.
 *
 * Both the Docker daemon and this plugin are actually capable of doing so. However, uncoordinated
 * parallel builds with multiple threads have been reported to overload build environments.
 * By making these methods synchronized in here, only one maven thread at a time is able to build and image or save one.
 * On shared build environments this reduces the parallel load on Docker.
 */
@Component(role = MicroserviceDockerClient.class)
@Slf4j(topic = "MicroserviceDockerClient")
public class MicroserviceDockerClientImpl extends AbstractLogEnabled implements MicroserviceDockerClient, Startable {

    DockerClient dockerClient;
    private final static ExecutorService executorService = Executors.newSingleThreadExecutor();

    @SneakyThrows
    synchronized public void buildDockerImage(String dockerDirectory, Set<String> tags, Map<String, String> buildArgs, String platform, String networkMode, Integer dockerBuildTimeout, AuthConfigurations pullConfiguration) {

        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(new File(dockerDirectory)).withTags(tags).withPlatform(platform).withBuildAuthConfigs(pullConfiguration);
        for (Map.Entry<String, String> buildArgument : buildArgs.entrySet()) {
            buildImageCmd = buildImageCmd.withBuildArg(buildArgument.getKey(), buildArgument.getValue());
        }

        if (!Strings.isNullOrEmpty(networkMode)) {
            buildImageCmd = buildImageCmd.withNetworkMode(networkMode);
        }

        log.info("Building Docker image. Docker dir={},tags={}, build arguments={}, platform={}", dockerDirectory, tags, buildArgs, platform);
        ImageBuildCompletionWaiter imageBuildCompletionWaiter = new ImageBuildCompletionWaiter();
        buildImageCmd.exec(imageBuildCompletionWaiter);
        log.info("Waiting for image build to complete (timeout={}s)", dockerBuildTimeout);
        imageBuildCompletionWaiter.awaitWithTimeout(dockerBuildTimeout);

        log.info("Image build success");


    }

    protected static class ImageBuildCompletionWaiter extends BuildImageResultCallback {

        String completedImageId;
        boolean buildFailed;
        private Future f;

        public void awaitWithTimeout(int seconds) throws MavenExecutionException {

            try {

                f = executorService.submit(() -> {

                    while (Objects.isNull(completedImageId)) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            log.error("Waiting for Docker completion was interrupted");
                        }
                    }
                    log.info("Build successful, imageId: {}", completedImageId);
                });

                f.get(seconds, TimeUnit.SECONDS);

            } catch (TimeoutException timeoutException) {
                throw new MavenExecutionException("Docker build timed out", timeoutException);
            } catch (ExecutionException e) {
                throw new MavenExecutionException("Docker build execution failed", e);
            } catch (CancellationException c) {
                throw new MavenExecutionException("Docker image build failed. See logs above", c);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MavenExecutionException("Docker build execution was interrupted", e);
            }

        }

        public void onNext(BuildResponseItem item) {

            if (Objects.nonNull(item.getStream())) {
                log.info(item.getStream());
            }

            if (item.isBuildSuccessIndicated()) {
                this.completedImageId = item.getImageId();
            } else if (item.isErrorIndicated()) {
                this.buildFailed = true;
                String errorMessage = String.format("Docker build error: %s %s", item.getErrorDetail().getCode(), item.getErrorDetail().getMessage());
                log.error(errorMessage);
                f.cancel(true);
            }

        }
    }

    synchronized public void saveDockerImage(final String image,  final OutputStream outputStream) throws MavenExecutionException {

        log.info("Saving image {} to output stream", image);

        try {
            SaveImageCmd saveImageCmd = dockerClient.saveImageCmd(image);

            final InputStream inputStream = saveImageCmd.exec();
            IOUtils.copyLarge(inputStream, outputStream);
        } catch (Exception e) {
            log.error("Save image failed, reason was: {}", e.getMessage());
            throw new MavenExecutionException("Docker save failed", e);
        }
    }

    private static DockerClient newDockerClient() {

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        log.debug("Docker Configuration used: {}", config);

        DockerHttpClient httpClient = new OkDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    synchronized public void deleteAll(String imageName, boolean withForce) {
        log.info("Cleaning up all docker images for {} ", imageName);

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        listImagesCmd.getFilters().putAll(getImageNameFilter(imageName));

        List<Image> images = listImagesCmd.exec();
        log.info("Found the following images for deletion {}", images);

        for (Image image : images) {
            log.info("Removing {} {}", image.getId(), image.getRepoTags());

            try {
                RemoveImageCmd removeImageCmd = dockerClient.removeImageCmd(image.getId()).withForce(withForce);
                removeImageCmd.exec();
                log.info(" -> Successfully removed image {} ", removeImageCmd.getImageId());
            } catch (Exception e) {
                log.error("Failed to remove image {}, reason: ", e);
            }
        }
    }


    @Override
    synchronized public void tagImage(String image, String imageNameWithRepository, String tag) {
        log.info("Tagging image {} image / {} with tag {}", image, imageNameWithRepository, tag);
        TagImageCmd tagImageCmd = dockerClient.tagImageCmd(image, imageNameWithRepository, tag);
        tagImageCmd.exec();
    }

    @Override
    synchronized public void pushImage(String name) {
        log.info("Pushing docker image to registry");
        PushImageCmd pushImageCmd = dockerClient.pushImageCmd(name);

        PushImageResponseCallBack callBack = new PushImageResponseCallBack();
        pushImageCmd.exec(callBack);
        callBack.waitForCompletionOrFailure();
    }

    @SneakyThrows
    @Override
    public void waitForImageInRegistry(String image, int timeOutSeconds) {

        log.info("Waiting for image {} to appear in registry, timeout={}s",image,timeOutSeconds);
        Map<String, List<String>> nameFilter = getImageNameFilter(image);

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        listImagesCmd.getFilters().putAll(nameFilter);

        List<Image> imagesFound = listImagesCmd.exec();
        long timeOutTimeStamp=System.currentTimeMillis()+ 1000L *timeOutSeconds;
        while (imagesFound.isEmpty()) {
            if (System.currentTimeMillis()>timeOutTimeStamp) {
                throw new RuntimeException("Waiting for image timed out after "+timeOutSeconds+"seconds");
            }
            Thread.sleep(2000);
            log.info("Image {} not found in registry, retrying...", image);
            imagesFound = listImagesCmd.exec();
        }
        log.info("Image appeared in registry.");
    }


    private static class PushImageResponseCallBack implements ResultCallback<PushResponseItem> {

        AtomicBoolean pushProcessDone = new AtomicBoolean(false);

        @SneakyThrows
        private void waitForCompletionOrFailure() {
            while (!pushProcessDone.get()) {
                Thread.sleep(250);
            }
        }

        @Override
        public void onStart(Closeable closeable) {
            log.info("Started pushing image to registry");
        }

        @Override
        public void onNext(PushResponseItem object) {

            if (object.isErrorIndicated()) {
                ResponseItem.ErrorDetail errorDetail = object.getErrorDetail();
                if (Objects.nonNull(errorDetail.getMessage())) {
                    throw new RuntimeException(errorDetail.getMessage());
                } else {
                    throw new RuntimeException(errorDetail.toString());
                }
            }

            if (Objects.nonNull(object.getStream())) {
                log.info(object.getStream());
            }
        }

        @SneakyThrows
        @Override
        public void onError(Throwable throwable) {
            pushProcessDone.set(true);
            log.error("Could not push image");
            throw throwable;
        }

        @Override
        public void onComplete() {
            pushProcessDone.set(true);
            log.info("Successfully pushed image to registry");
        }

        @Override
        public void close() {
            log.info("Push closed");
        }
    }


    private Map<String, List<String>> getImageNameFilter(String imageName) {
        Map<String, List<String>> imageFilters = Maps.newHashMap();
        imageFilters.put("reference", Lists.newArrayList(imageName));
        return imageFilters;
    }

    @Override
    public void start() throws StartingException {
        getLogger().debug("Starting docker client ");
        if (dockerClient == null) {
            try {
                dockerClient = newDockerClient();
            } catch (Exception e) {
                throw new StartingException("Can't initialize docker client" + e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {
        getLogger().debug("Stopping docker client ");
        if (Objects.nonNull(dockerClient)) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                log.error("Can't close docker client. Reason: {}", e);
            }
        }
    }

}
