package com.cumulocity.model;

import com.cumulocity.model.util.ExtensibilityConverter;
import com.google.common.base.Function;
import org.svenson.AbstractDynamicProperties;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Map.Entry;

import static com.cumulocity.model.util.Classes.isSimpleProperty;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents the common elements of any modeled entity. Can serialize into and
 * be deserialized from JSON.
 */
public abstract class Document<T extends ID> extends JSONBase {



    public static DynamicPropertiesFilter acceptAll = new DynamicPropertiesFilter() {

        @Override
        public boolean apply(String name) {
            return true;
        }
    };

    public static <E extends AbstractDynamicProperties> E copyDynamicProperties(AbstractDynamicProperties source, E target, DynamicPropertiesFilter filter) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null!");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null!");
        }
        for (String name : source.propertyNames()) {
            if(filter.apply(name)) {
                target.setProperty(name, source.getProperty(name));
            }
        }
        return target;
    }

    public static <E extends AbstractDynamicProperties> E copyDynamicProperties(AbstractDynamicProperties source, E target) {
        return copyDynamicProperties(source, target, acceptAll);
    }

    public static <E extends AbstractDynamicProperties> E deepCopyDynamicProperties(AbstractDynamicProperties source, E target) {
        checkArgument(source != null, "Source cannot be null!");
        checkArgument(target != null, "Target cannot be null!");
        for (String name : source.propertyNames()) {
            final Object sourceValue = source.getProperty(name);
            if (sourceValue == null || isSimpleProperty(sourceValue.getClass())) {
                target.setProperty(name, sourceValue);
            } else {
                target.setProperty(name, getJSONParser().parse(getJSONGenerator().forValue(sourceValue)));
            }
        }
        return target;
    }

	private T id;

    @Deprecated
    private String internalId;

	/**
	 * The document revision in couchDB. revision will no longer be a
	 * strong-typed property - it is not applicable in the Agent Perspective
	 * (See note on
	 * https://startups.jira.com/wiki/display/MTM/Extensibility+model)
	 */
	@Deprecated
	private String revision;

    protected Document() {
    }

    protected Document(T id) {
        this(id, (String) null);
    }

    protected Document(T id, Map<String, Object> fragments) {
        this(id, null, null, fragments);
    }

    protected Document(T id, String internalId) {
        this(id, internalId, null);
    }

    protected Document(T id, String internalId, String revision) {
        this(id, internalId, revision, null);
    }

    protected Document(T id, String internalId, String revision, Map<String, Object> fragments) {
        this.id = id;
        this.internalId = internalId;
        this.revision = revision;
        add(fragments);
    }

    @JSONProperty(value = "id", ignoreIfNull = true)
	@JSONConverter(type = IDTypeConverter.class)
	public T getId() {
		return this.id;
	}

	public void setId(T id) {
		this.id = id;
	}

    //Not use in current repository implementation, to remove
    @Deprecated
	@JSONProperty(value = "_id", ignoreIfNull = true)
	public String getInternalId() {
		return internalId;
	}

    //Not use in current repository implementation, to remove
    @Deprecated
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

    //Not use in current repository implementation, to remove
	@Deprecated
	@JSONProperty(value = "_rev", ignoreIfNull = true)
	public String getRevision() {
		return this.revision;
	}

    //Not use in current repository implementation, to remove
	@Deprecated
	public void setRevision(String revision) {
		this.revision = revision;
	}

	/**
	 * Sets a property referring to the given object. The name of the property
	 * will be the fully qualified class name with dots replaced by underscores.<br>
	 * For example, if the object is of type:<br>
	 * <code>
	 * com.cumulocity.model.Coordinate
	 * </code>
	 * <br>then the property name will be:<br>
	 * <code>
	 * "com_cumulocity_model_Coordinate"
	 * </code>
	 *
	 * @param object
	 */
	@JSONProperty(ignore = true)
	public void set(Object object) {
		set(object, object.getClass());
	}

	/**
	 * Sets a property referring to the given object, using an arbitrary
	 * property name.
	 *
	 * @param object
	 * @param propertyName
	 */
	@JSONProperty(ignore = true)
	public void set(Object object, String propertyName) {
		setProperty(propertyName, object);
	}

	/**
	 * Sets a property referring to the given object. The name of the property
	 * will be the fully qualified class name of the given class, with dots
	 * replaced by underscores.<br>
	 * This can be useful if you want to name the property after the base class
	 * rather than the actual class of object.<br>
	 * For example, if clazz is of type:<br>
	 * <code>
	 * com.cumulocity.model.Coordinate
	 * </code>
	 * <br>then the property name will be:<br>
	 * <code>
	 * "com_cumulocity_model_Coordinate"
	 * </code>
	 *
	 * @param object
	 * @param clazz
	 */
	@JSONProperty(ignore = true)
	public <C> void set(Object object, Class<C> clazz) {
		setProperty(ExtensibilityConverter.classToStringRepresentation(clazz),
				object);
	}

    @JSONProperty(ignore = true)
    public void add(Map<String, Object> fragments) {
        if (fragments == null) {
            return;
        }
        for (Entry<String, Object> fragment : fragments.entrySet()) {
            setProperty(fragment.getKey(), fragment.getValue());
        }
    }

	/**
	 * Returns the object whose parameter name is given by clazz, or null if no
	 * such property exists, or property cannot be cased to clazz.
	 *
	 * @see #set(Object)
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public <C> C get(Class<C> clazz) {
        Object property = getProperty(ExtensibilityConverter
                .classToStringRepresentation(clazz));
        return clazz.isInstance(property) ? clazz.cast(property) : null;
    }

	public <C> C get(final String propertyName, final Class<C> asClass) {
		final Object o = get(propertyName);
		if (o == null) {
			return null;
		}
		return readObject(o, asClass);
	}

	protected <C> C readObject(Object source, Class<C> asClass) {
		return fromJSON(getJSONGenerator().forValue(source), asClass);
	}

	/**
	 * Remove the fragment whose name is given by clazz if exists
	 *
	 * @param clazz
	 */
	public void remove(Class<?> clazz) {
		removeProperty(ExtensibilityConverter.classToStringRepresentation(clazz));
	}

	/**
	 * Returns the object associated with the given property name, or null if no
	 * such property exists.
	 *
	 * @param name
	 * @return
	 */
	public Object get(String name) {
		return getProperty(name);
	}

    /**
     * Returns the object associated with the given property name, or null if no
     * such property exists.
     * Such an accessor is required when bean naming conventions are used to
     * discover available properties.
     *
     * Same as a call to {@link #get(String name)}
     *
     * @param name
     * @return
     */
    public Object getFragment(String name) {
        return get(name);
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;

        Document document = (Document) o;

        if (getId() != null ? !getId().equals(document.getId()) : document.getId() != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return toJSON();
    }


    public static <T extends ID> Function<? super Document<T>,T> asId(){
    	return new Function<Document<T>, T>() {
			@Nullable
			@Override
			public T apply(@Nullable Document<T> input) {
				return input.getId();
			}
		};
	}

}
