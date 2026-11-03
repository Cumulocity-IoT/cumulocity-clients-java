package com.cumulocity.sdk.client;

/**
 * Thrown when a bulk request fails for one of its elements. For a bulk create that failed while writing, part of the request may have been
 * created. In this case {@link #getFragmentType()} names the fragment that every created managed object carries, so that they can be found
 * with {@code InventoryFilter.byFragmentType(fragmentType)}.
 */
public class BulkRequestException extends SDKException {

    private static final long serialVersionUID = 4821290137662719423L;

    private final Integer failedIndex;

    private final String fragmentType;

    public BulkRequestException(int httpStatusCode, String message, Integer failedIndex, String fragmentType) {
        super(httpStatusCode, message);
        this.failedIndex = failedIndex;
        this.fragmentType = fragmentType;
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
