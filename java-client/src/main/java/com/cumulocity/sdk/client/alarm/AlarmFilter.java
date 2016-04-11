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

package com.cumulocity.sdk.client.alarm;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.model.event.CumulocitySeverities;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Filter;
import com.cumulocity.sdk.client.ParamSource;
import org.joda.time.DateTime;

/**
 * A filter to be used in alarm queries.
 * The setter (by*) methods return the filter itself to provide chaining:
 * {@code AlarmFilter filter = new AlarmFilter().byStatus(st).bySource(src);}
 */
public class AlarmFilter extends Filter {

    @ParamSource
    private String status;

    @ParamSource
    private String source;

    @ParamSource
    private String dateFrom;

    @ParamSource
    private String dateTo;
    
    @ParamSource
    private String severity;
    
    @ParamSource
    private String resolved;
    
    @ParamSource
    private String type;
    
    /**
     * Specifies the {@code source} query parameter
     *
     * @param source the managed object that generated the alarm(s)
     * @return the alarm filter with {@code source} set
     */
    public AlarmFilter bySource(GId source) {
        this.source = source.getValue();
        return this;
    }
    
    /**
     * Specifies the {@code source} query parameter
     *
     * @param source the managed object that generated the alarm(s)
     * @return the alarm filter with {@code source} set
     */
    @Deprecated
    public AlarmFilter bySource(ManagedObjectRepresentation source) {
        this.source = source.getId().getValue();
        return this;
    }

    /**
     * Specifies the {@code status} query parameter
     *
     * @param status status of the alarm(s)
     * @return the alarm filter with {@code status} set
     */
    public AlarmFilter byStatus(CumulocityAlarmStatuses... statuses) {
    	if(statuses == null){
    		this.status = null;
    		return this;
    	}
    	StringBuilder tmp = new StringBuilder();
		for (int index = 0; index <= statuses.length - 1; index++) {
			tmp.append(statuses[index].toString());
			if(index < statuses.length -1){
				tmp.append(",");	
			}
		}
		this.status = tmp.toString();
        return this;

    }

    /**
     * @return the {@code status} parameter of the query
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Specifies the {@code status} query parameter
     *
     * @param status status of the alarm(s)
     * @return the alarm filter with {@code status} set
     */
    public AlarmFilter bySeverity(CumulocitySeverities severity) {
        this.severity = severity.toString();
        return this;
    }

    /**
     * @return the {@code status} parameter of the query
     */
    public String getSeverity() {
        return severity;
    }

    
    /**
     * @return the {@code source} parameter of the query
     */
    public String getSource() {
        return source;
    }


    /**
     * Specifies the {@code fromDate} and {@code toDate} query parameters 
     * for query in a time range.
     *
     * @param fromDate the start date time of the range
     * @param toDate the end date time of the range
     * @return the alarm filter with {@code fromDate} and {@code toDate} set.
     */
    public AlarmFilter byDate(DateTime fromDate, DateTime toDate) {
        this.dateFrom = DateTimeConverter.date2String(fromDate);
        this.dateTo = DateTimeConverter.date2String(toDate);
        return this;
    }

    /**
     * Specifies the {@code fromDate} query parameter 
     * for querying all alarms from the specified date time.
     *
     * @param fromDate the date time from which all alarms to be returned.
     * @return the alarm filter with {@code fromDate} set
     */
    public AlarmFilter byFromDate(DateTime fromDate) {
        this.dateFrom = DateTimeConverter.date2String(fromDate);
        return this;
    }
    
    public String getFromDate() {
        return dateFrom;
    }

    public String getToDate() {
        return dateTo;
    }

    public String getResolved() {
        return resolved;
    }

    public AlarmFilter byResolved(Boolean resolved) {
        this.resolved = resolved.toString();
        return this;
    }

	public String getType() {
		return type;
	}

	public AlarmFilter byType(String type) {
		this.type = type;
		return this;
	}
    
    
}
