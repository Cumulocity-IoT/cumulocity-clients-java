/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.cumulocity.me.rest.convert.devicecontrol;

import c8y.Relay;
import c8y.Relay.RelayState;

import com.cumulocity.me.rest.convert.base.BaseRepresentationConverter;
import com.cumulocity.me.rest.json.JSONObject;

public class RelayConverter extends BaseRepresentationConverter {

    private static final String PROP_STATE = "state";
    
    public JSONObject toJson(Object representation) {
        JSONObject json = new JSONObject();
        Relay relayControl = (Relay) representation;
        putString(json, PROP_STATE, relayControl.getRelayState().name());
        return json;
    }

    public Object fromJson(JSONObject json) {
        Relay relayControl = new Relay();
        relayControl.setRelayState(RelayState.OPEN);
        return relayControl;
    }

    @SuppressWarnings("rawtypes")
	protected Class supportedRepresentationType() {
        return Relay.class;
    }

}
