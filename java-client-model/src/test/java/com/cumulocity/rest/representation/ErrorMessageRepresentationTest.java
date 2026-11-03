package com.cumulocity.rest.representation;

import org.junit.jupiter.api.Test;
import org.svenson.JSONParser;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageRepresentationTest {

    private final JSONParser jsonParser = new JSONParser();

    @Test
    public final void shouldDeserializeBulkRequest() {
        //given
        String json = "{\"error\":\"identity/Duplicate\",\"info\":\"https://cumulocity.com/api/core/\","
                + "\"bulkRequest\":{\"failedIndex\":3,\"fragmentType\":\"c8y_BatchId_7c8e1e0a\"}}";

        //when
        ErrorMessageRepresentation error = jsonParser.parse(ErrorMessageRepresentation.class, json);

        //then
        assertThat(error.getError()).isEqualTo("identity/Duplicate");
        assertThat(error.getBulkRequest()).isNotNull();
        assertThat(error.getBulkRequest().getFailedIndex()).isEqualTo(3);
        assertThat(error.getBulkRequest().getFragmentType()).isEqualTo("c8y_BatchId_7c8e1e0a");
    }

    @Test
    public final void shouldDeserializePartiallyFailedBulkCreateResponseOfPlatform() {
        //given: response of a bulk create where another request bound the external id of the last element while the request was processed
        String json = "{\"error\":\"identity/Duplicate\","
                + "\"message\":\"ID [type=scratch_race_extid, value=6c0bac20-raw] was already bound to different Global ID.\","
                + "\"info\":\"https://cumulocity.com/api/core/\","
                + "\"bulkRequest\":{\"failedIndex\":49,\"fragmentType\":\"c8y_BatchId_53c5de82-6944-42ea-b075-d2b7f085e75f\"}}";

        //when
        ErrorMessageRepresentation error = jsonParser.parse(ErrorMessageRepresentation.class, json);

        //then
        assertThat(error.getError()).isEqualTo("identity/Duplicate");
        assertThat(error.getMessage())
                .isEqualTo("ID [type=scratch_race_extid, value=6c0bac20-raw] was already bound to different Global ID.");
        assertThat(error.getInfo()).isEqualTo("https://cumulocity.com/api/core/");
        assertThat(error.getBulkRequest().getFailedIndex()).isEqualTo(49);
        assertThat(error.getBulkRequest().getFragmentType()).isEqualTo("c8y_BatchId_53c5de82-6944-42ea-b075-d2b7f085e75f");
    }

    @Test
    public final void shouldDeserializeBulkRequestWithoutFragmentType() {
        //given
        String json = "{\"error\":\"inventory/Not Found\",\"bulkRequest\":{\"failedIndex\":1}}";

        //when
        ErrorMessageRepresentation error = jsonParser.parse(ErrorMessageRepresentation.class, json);

        //then
        assertThat(error.getBulkRequest().getFailedIndex()).isEqualTo(1);
        assertThat(error.getBulkRequest().getFragmentType()).isNull();
    }

    @Test
    public final void shouldDeserializeErrorWithoutBulkRequest() {
        //when
        ErrorMessageRepresentation error = jsonParser.parse(ErrorMessageRepresentation.class, "{\"error\":\"general/internalError\"}");

        //then
        assertThat(error.getBulkRequest()).isNull();
    }
}
