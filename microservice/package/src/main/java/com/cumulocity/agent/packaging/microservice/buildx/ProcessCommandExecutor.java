package com.cumulocity.agent.packaging.microservice.buildx;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j(topic = "BuildxClient")
public class ProcessCommandExecutor implements CommandExecutor {

    @Override
    public Result execute(List<String> command, Map<String, String> environment, int timeoutSeconds, boolean logOutput, OutputStream stdout) {
        // without a stdout target both streams are read as one output
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(stdout == null);
        Map<String, String> processEnvironment = processBuilder.environment();
        environment.forEach((key, value) -> {
            if (value == null) {
                processEnvironment.remove(key);
            } else {
                processEnvironment.put(key, value);
            }
        });

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            log.debug("Cannot start {}: {}", command, e.getMessage());
            return new Result(-1, e.getMessage());
        }

        StringBuilder output = new StringBuilder();
        Thread outputReader = start("docker-cli-output", () -> readLines(stdout == null ? process.getInputStream() : process.getErrorStream(), output, logOutput));
        AtomicReference<IOException> copyError = new AtomicReference<>();
        Thread stdoutCopier = stdout == null ? null : start("docker-cli-stdout", () -> {
            try (InputStream in = process.getInputStream()) {
                in.transferTo(stdout);
            } catch (IOException e) {
                copyError.set(e);
                process.destroyForcibly();
            }
        });

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                if (stdoutCopier != null) {
                    // the caller closes the stdout target after this method returns
                    stdoutCopier.join(TimeUnit.SECONDS.toMillis(5));
                }
                synchronized (output) {
                    return new Result(-1, output + "\nCommand timed out after " + timeoutSeconds + " seconds");
                }
            }
            outputReader.join();
            if (stdoutCopier != null) {
                stdoutCopier.join();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Result(-1, "Interrupted");
        }
        synchronized (output) {
            if (copyError.get() != null) {
                return new Result(-1, output + "\nCannot write the command output: " + copyError.get().getMessage());
            }
            return new Result(process.exitValue(), output.toString());
        }
    }

    private static Thread start(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void readLines(InputStream stream, StringBuilder output, boolean logOutput) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append('\n');
                }
                if (logOutput) {
                    log.info(line);
                } else {
                    log.debug(line);
                }
            }
        } catch (IOException e) {
            log.debug("Error reading process output: {}", e.getMessage());
        }
    }
}
