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
package com.cumulocity.sdk.client.event;

import static com.cumulocity.model.util.DateTimeUtils.nowLocal;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.joda.time.DateTime;
import org.junit.Test;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.model.idtype.GId;

public class EventFilterTest {

    @Test
    public void shouldHoldTypeAndSource() throws Exception {
        EventFilter filter = new EventFilter().byType("type").bySource(new GId("1"));
        assertThat(filter.getType(), is("type"));
        assertThat(filter.getSource(), is("1"));
    }
    
    @Test
    public void shouldHoldFragmentTypeAndDate() throws Exception {
        DateTime fromDate = nowLocal();
        DateTime toDate = nowLocal();
        EventFilter filter = new EventFilter().byFragmentType(Object.class).byDate(fromDate, toDate);
        assertThat(filter.getFragmentType(), is(Object.class));
        assertThat(filter.getFromDate(), is(DateTimeConverter.date2String(fromDate)));
        assertThat(filter.getToDate(), is(DateTimeConverter.date2String(toDate)));
    }
}
