package com.cumulocity.rest.representation.event;

import com.cumulocity.rest.representation.SourceableConverter;
import com.cumulocity.rest.representation.SourceableRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.model.IDTypeConverter;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

import java.util.Date;

import static com.cumulocity.model.util.DateTimeUtils.newLocal;

/**
 * A Java Representation for the Media Type {@link EventMediaType#EVENT}.
 *
 */

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EventRepresentation extends AbstractExtensibleRepresentation implements SourceableRepresentation {

    @EqualsAndHashCode.Include
    private GId id;

    @EqualsAndHashCode.Include
    private String type;

    @EqualsAndHashCode.Include
    private DateTime time;

    @EqualsAndHashCode.Include
    private DateTime creationTime;

    @EqualsAndHashCode.Include
    private DateTime lastUpdated;

    @EqualsAndHashCode.Include
    private String text;

    @EqualsAndHashCode.Include
    private ManagedObjectRepresentation managedObject;

    @EqualsAndHashCode.Include
    private ExternalIDRepresentation externalSource;

    public EventRepresentation() {
    }

    @JSONConverter(type = IDTypeConverter.class)
    @JSONProperty(ignoreIfNull = true)
    public GId getId() {
        return id;
    }

    public void setId(GId id) {
        this.id = id;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JSONProperty(value = "deprecated_Time", ignore = true)
    @Deprecated
    public Date getTime() {
        return time == null ? null : time.toDate();
    }

    @Deprecated
    public void setTime(Date time) {
        this.time = time == null ? null : newLocal(time);
    }

    @JSONProperty(value = "time", ignoreIfNull = true)
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getDateTime() {
        return time;
    }

    public void setDateTime(DateTime time) {
        this.time = time;
    }

    @JSONProperty(value = "text", ignoreIfNull = true)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JSONProperty(value = "source", ignoreIfNull = true)
    @JSONConverter(type = SourceableConverter.class)
    public ManagedObjectRepresentation getSource() {
        return managedObject;
    }

    public void setSource(ManagedObjectRepresentation managedObject) {
        this.managedObject = managedObject;
    }

    @JSONProperty(value = "externalSource", ignoreIfNull = true)
    public ExternalIDRepresentation getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(ExternalIDRepresentation externalSource) {
        this.externalSource = externalSource;
    }

    @JSONProperty(value = "deprecated_CreationTime", ignore = true)
    @Deprecated
    public Date getCreationTime() {
        return creationTime == null ? null : creationTime.toDate();
    }

    @Deprecated
    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime == null ? null : newLocal(creationTime);
    }

    @JSONProperty(value = "creationTime", ignoreIfNull = true)
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getCreationDateTime() {
        return creationTime;
    }

    public void setCreationDateTime(DateTime creationTime) {
        this.creationTime = creationTime;
    }

    @JSONProperty(value = "lastUpdated", ignoreIfNull = true)
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getLastUpdatedDateTime() {
        return lastUpdated;
    }

    public void setLastUpdatedDateTime(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @EqualsAndHashCode.Include
    GId getSourceId() {
        return managedObject == null ? null : managedObject.getId();
    }
}
