package com.cumulocity.model.idtype;

import com.cumulocity.model.ID;
import com.google.common.base.Function;
import org.svenson.JSON;
import org.svenson.JSONable;

import jakarta.annotation.Nullable;

public class GId extends ID implements JSONable {

    public static GId asGId(Object id) {
        if (id instanceof GId) {
            return (GId) id;
        } else if (id instanceof ID) {
            return new GId(((ID) id).getValue());
        } else {
            return asGId(String.valueOf(id));
        }
    }

    public static GId asGId(String id) {
        return id == null ? null : new GId(id);
    }

    public static GId asGId(Long id) {
        return id == null ? null : new GId(id.toString());
    }

    public GId() {
        super();
    }

    public GId(String id) {
        super(id);
    }

    public GId(String type, String value, String name) {
        super(type, value, name);
    }

    public static Function<GId, String> asString() {
        return new Function<GId, String>() {
            @Override
            public String apply(GId input) {
                return asString(input);
            }
        };
    }

    /**
     * GId type returns just its value when converting to JSON (see
     * https://startups.jira.com/wiki/display/MTM/Identification+model)
     */
    @Override
    public String toJSON() {
        return JSON.defaultJSON().quote(super.getValue());
    }

    public Long getLong() {
        try {
            if (getValue() == null) {
                return null;
            }
            return Long.parseLong(getValue());
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> Function<T, GId> asGId() {
        return new Function<T, GId>() {
            @Nullable
            @Override
            public GId apply(@Nullable T input) {
                return GId.asGId(input);
            }
        };
    }
}
