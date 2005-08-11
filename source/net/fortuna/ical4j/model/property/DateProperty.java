/*
 * $Id$
 *
 * Created on 9/07/2005
 *
 * Copyright (c) 2005, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.ical4j.model.property;

import java.util.TimeZone;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.util.TimeZoneUtils;

/**
 * Base class for properties with a DATE or DATE-TIME value. Note that some
 * sub-classes may only allow either a DATE or a DATE-TIME value, for which
 * additional rules/validation should be specified.
 * @author Ben Fortuna
 */
public abstract class DateProperty extends Property {
    
    private Date date;

    /**
     * @param aName
     * @param aList
     */
    public DateProperty(final String name, final ParameterList parameters) {
        super(name, parameters);
    }

    /**
     * @param aName
     */
    public DateProperty(final String name) {
        super(name);
    }

    /**
     * @return Returns the date.
     */
    public final Date getDate() {
        return date;
    }

    /**
     * @param date The date to set.
     */
    public final void setDate(final Date date) {
        this.date = date;
    }

    /**
     * Updates the VTIMEZONE associated with the property's value. The underlying
     * TZID parameter will also be updated by this method.
     * Note that this method is only applicable where the current date is an
     * instance of <code>DateTime</code>. For all other cases an
     * <code>UnsupportedOperationException</code> will be thrown.
     * @param vTimeZone
     */
    public final void setVTimeZone(final VTimeZone vTimeZone) {
        if (getDate() != null && !(getDate() instanceof DateTime)) {
            throw new UnsupportedOperationException("VTimeZone is not applicable to current value");
        }
        if (TimeZoneUtils.isUtc(vTimeZone.getTimeZone())) {
            getParameters().remove(getParameters().getParameter(Parameter.TZID));
        }
        else {
            getParameters().add(vTimeZone.getTzIdParam());
            if (getDate() != null) {
                ((DateTime) getDate()).setTimeZone(vTimeZone.getTimeZone());
            }
        }
    }
    
    /**
     * Resets the VTIMEZONE associated with the property. If utc is true,
     * any TZID parameters are removed and the Java timezone is updated
     * to UTC time. If utc is false, TZID parameters are removed and the
     * Java timezone is set to the default timezone (i.e. represents a
     * "floating" local time)
     * @param utc
     */
    public final void setUtc(final boolean utc) {
        if (utc) {
            setVTimeZone(VTimeZone.getVTimeZone(TimeZone.getTimeZone("UTC").getID()));
        }
        else {
            setVTimeZone(VTimeZone.getDefault());
        }
    }
    
    /**
     * Indicates whether the current date value is specified in UTC time.
     * @return
     */
    public final boolean isUtc() {
        if (getDate() instanceof DateTime) {
            return ((DateTime) getDate()).isUtc();
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see net.fortuna.ical4j.model.Property#validate()
     */
    public void validate() throws ValidationException {
        super.validate();
        Value value = (Value) getParameters().getParameter(Parameter.VALUE);
        if (value != null && !Value.DATE.equals(value) && !Value.DATE_TIME.equals(value)) {
            throw new ValidationException("Invalid VALUE parameter [" + value.getValue() + "]");
        }
        if ((Value.DATE.equals(value) && getDate() instanceof DateTime)
                || (Value.DATE_TIME.equals(value) && !(getDate() instanceof DateTime))) {
            throw new ValidationException("VALUE parameter [" + value.getValue() + "] is invalid for date instance");
        }
    }
}
