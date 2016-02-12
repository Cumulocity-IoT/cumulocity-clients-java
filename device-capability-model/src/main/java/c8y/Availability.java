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

package c8y;

import org.joda.time.DateTime;
import org.svenson.AbstractDynamicProperties;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

import com.cumulocity.model.DateConverter;

public class Availability extends AbstractDynamicProperties {

    private DateTime lastMessage;
    private ConnectionState status;

    public Availability() {
    }

    public Availability(DateTime lastMessage, ConnectionState status) {
        this.lastMessage = lastMessage;
        this.status = status;
    }

    @JSONProperty(ignoreIfNull = true)
    @JSONConverter(type = DateConverter.class)
    public DateTime getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(DateTime lastMessage) {
        this.lastMessage = lastMessage;
    }

    public ConnectionState getStatus() {
        return status;
    }

    public void setStatus(ConnectionState status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((lastMessage == null) ? 0 : lastMessage.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Availability other = (Availability) obj;
        if (lastMessage == null) {
            if (other.lastMessage != null) {
                return false;
            }
        } else if (!lastMessage.equals(other.lastMessage)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        return true;
    }
}
