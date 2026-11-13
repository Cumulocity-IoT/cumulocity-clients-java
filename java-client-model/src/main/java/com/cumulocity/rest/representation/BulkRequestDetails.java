package com.cumulocity.rest.representation;

import org.svenson.JSONProperty;

/**
 * Describes which element of a bulk request an error belongs to. Part of the error response of the bulk create and update operations.
 */
public class BulkRequestDetails {

    // Position of the failed element in the request, counted from 0.
    private Integer failedIndex;

    // Batch marker fragment carried by the managed objects that were created. Only present if part of a bulk create may have been created.
    private String fragmentType;

    @JSONProperty(ignoreIfNull = true)
    public Integer getFailedIndex() {
        return failedIndex;
    }

    public void setFailedIndex(Integer failedIndex) {
        this.failedIndex = failedIndex;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getFragmentType() {
        return fragmentType;
    }

    public void setFragmentType(String fragmentType) {
        this.fragmentType = fragmentType;
    }

    public String toString() {
        return "{failedIndex=" + failedIndex + ",fragmentType=\"" + fragmentType + "\"}";
    }
}
