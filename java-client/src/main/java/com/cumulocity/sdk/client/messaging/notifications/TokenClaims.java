package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.rest.representation.BaseResourceRepresentation;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.svenson.JSONProperty;

@NoArgsConstructor
@AllArgsConstructor
@Setter
public class TokenClaims extends BaseResourceRepresentation {

    private String subscriber;

    private String topic;

    private String jti;

    private long iat;

    private long exp;

    @JSONProperty(value = "sub", ignoreIfNull = true)
    public String getSubscriber() {
        return this.subscriber;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getTopic() {
        return this.topic;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getJti() {
        return this.jti;
    }

    @JSONProperty(ignoreIfNull = true)
    public long getIat() {
        return this.iat;
    }

    @JSONProperty(ignoreIfNull = true)
    public long getExp() {
        return this.exp;
    }
}