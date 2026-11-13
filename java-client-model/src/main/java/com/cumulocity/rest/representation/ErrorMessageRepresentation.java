package com.cumulocity.rest.representation;

import org.svenson.JSONProperty;

public class ErrorMessageRepresentation extends AbstractExtensibleRepresentation {

    // Application level error code
    private String error;
    // Short text description of the error
    private String message;
    // URL to error description on the Internet.
    private String info;

    // details     JSON-Object with ErrorDetails. Only available if in DEBUG mode.
    private ErrorDetails details;

    // Only available for errors of bulk requests.
    private BulkRequestDetails bulkRequest;

    @JSONProperty(ignoreIfNull = true)
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @JSONProperty(ignoreIfNull = true)
    public ErrorDetails getDetails() {
        return details;
    }

    public void setDetails(ErrorDetails details) {
        this.details = details;
    }

    @JSONProperty(ignoreIfNull = true)
    public BulkRequestDetails getBulkRequest() {
        return bulkRequest;
    }

    public void setBulkRequest(BulkRequestDetails bulkRequest) {
        this.bulkRequest = bulkRequest;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("error=").append('"').append(error).append('"').append(',');
        sb.append("message=").append('"').append(message).append('"').append(',');
        sb.append("info=").append('"').append(info).append('"').append(',');
        sb.append("details=").append('"').append(details).append('"');
        if (bulkRequest != null) {
            sb.append(',').append("bulkRequest=").append(bulkRequest);
        }
        sb.append('}');
        return sb.toString();
    }
}
