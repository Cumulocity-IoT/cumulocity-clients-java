package com.cumulocity.sdk.client;

import com.cumulocity.rest.representation.BulkRequestDetails;

/**
 * Thrown when a bulk request, for example {@code InventoryApi.createBulk} or {@code MeasurementApi.createBulk}, fails for one of its
 * elements.
 * <p>
 * For a bulk create of managed objects that failed while writing, part of the request may have been created. In this case
 * {@link #getFragmentType()} names the fragment that every created managed object carries, so that they can be found with
 * {@code InventoryFilter.byFragmentType(fragmentType)}. For measurements, {@link #getFragmentType()} is always {@code null}.
 */
public class BulkRequestException extends SDKException {

    private static final long serialVersionUID = 4821290137662719423L;

    private final Integer failedIndex;

    private final String fragmentType;

    public BulkRequestException(int httpStatusCode, String message, BulkRequestDetails details) {
        super(httpStatusCode, message);
        this.failedIndex = details.getFailedIndex();
        this.fragmentType = details.getFragmentType();
    }

    /**
     * @return the position of the failed element in the request, counted from 0
     */
    public Integer getFailedIndex() {
        return failedIndex;
    }

    /**
     * @return the batch marker fragment of the managed objects created by the request, or {@code null} if nothing was created
     */
    public String getFragmentType() {
        return fragmentType;
    }
}
