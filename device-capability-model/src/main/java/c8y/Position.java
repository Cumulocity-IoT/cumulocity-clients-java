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

import java.math.BigDecimal;

import org.svenson.AbstractDynamicProperties;

public class Position extends AbstractDynamicProperties {
	private static final long serialVersionUID = -8365376637780307348L;
	
	private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal alt;
    private long accuracy;

	public BigDecimal getLat() {
		return lat;
	}

	public void setLat(BigDecimal latitude) {
		this.lat = latitude;
	}

	public BigDecimal getLng() {
		return lng;
	}

	public void setLng(BigDecimal longitude) {
		this.lng = longitude;
	}

	public BigDecimal getAlt() {
		return alt;
	}

	public void setAlt(BigDecimal altitude) {
		this.alt = altitude;
	}
	
	public long getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(long accuracy) {
        this.accuracy = accuracy;
    }

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Position))
			return false;

		Position rhs = (Position) obj;
		boolean result = (lat == null) ? (rhs.lat == null) : lat.equals(rhs.lat);
		result = result && ((lng == null) ? (rhs.lng == null) : lng.equals(rhs.lng));
		result = result && ((alt == null) ? (rhs.alt == null) : alt.equals(rhs.alt));
		return result;
	}
	
    @Override
    public int hashCode() {
        int result = lat == null ? 0 : lat.hashCode();
        result = 31 * result + (lng == null ? 0 : lng.hashCode());
        result = 31 * result + (alt == null ? 0 : alt.hashCode());
        return result;
    }
}
