package com.cumulocity.agent.packaging.microservice.buildx;

import lombok.Value;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Runs external commands. Abstracted to allow mocking the docker CLI in tests.
 */
public interface CommandExecutor {

    /**
     * Runs the command; stdout and stderr are returned together as {@link Result#getOutput()}.
     *
     * @param command        command with arguments
     * @param environment    environment overrides; a {@code null} value removes the variable from the inherited environment
     * @param timeoutSeconds maximum execution time
     * @param logOutput      whether output lines should be logged at info level (otherwise debug)
     */
    default Result execute(List<String> command, Map<String, String> environment, int timeoutSeconds, boolean logOutput) {
        return execute(command, environment, timeoutSeconds, logOutput, null);
    }

    /**
     * Runs the command and copies its stdout to {@code stdout}, e.g. an image exported with {@code dest=-}.
     * Only stderr is returned as {@link Result#getOutput()}. With {@code stdout == null} it is the same as
     * {@link #execute(List, Map, int, boolean)}.
     */
    Result execute(List<String> command, Map<String, String> environment, int timeoutSeconds, boolean logOutput, OutputStream stdout);

    @Value
    class Result {
        int exitCode;
        String output;

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
