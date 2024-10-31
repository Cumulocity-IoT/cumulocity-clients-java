package com.cumulocity.model.application.microservice.validation;

import lombok.*;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Probe {
    @Valid
    private ExecAction exec;
    @Valid
    private TCPSocketAction tcpSocket;
    @Valid
    private HTTPGetAction httpGet;

    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Builder.Default
    private Integer initialDelaySeconds = 0;

    @Min(value = 1)
    @Builder.Default
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Integer periodSeconds = 10;

    @Min(value = 1)
    @Builder.Default
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Integer successThreshold = 1;

    @Min(value = 1)
    @Builder.Default
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Integer timeoutSeconds = 5;

    @Min(value = 1)
    @Builder.Default
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Integer failureThreshold = 3;


    @Data
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TCPSocketAction {
        @Min(1)
        @Max(65535)
        private Integer port = 80;
        @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
        private String host;

    }

    @Data
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HTTPGetAction {
        private String host;
        @Size(min = 1)
        @NotNull
        private String path;
        @Min(1)
        @Max(65535)
        private Integer port = 80;
        @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
        @Pattern(regexp = "(HTTP|HTTPS)")
        private String scheme;
        @Singular
        @Setter(onMethod_ = @JSONTypeHint(HttpHeader.class))
        private List<HttpHeader> headers;
    }

    @Data
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HttpHeader {
        private String name;
        private String value;

        public static HttpHeader of(String name, String value) {
            return new HttpHeader(name, value);
        }
    }

    @Data
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ExecAction {
        @NotNull
        @Size(min = 1)
        private List<String> command;
    }
}
