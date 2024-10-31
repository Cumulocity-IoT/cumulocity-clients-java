package com.cumulocity.rest.representation.tenant;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.Size;

public class OptionRepresentation extends AbstractExtensibleRepresentation {

	@Size(max = 256)
	private String category;

	@Size(max = 256)
	private String key;

	private String value;

	public static OptionRepresentation asOptionRepresentation(String category, String key, String value) {
		final OptionRepresentation optionRepresentation = new OptionRepresentation();
		optionRepresentation.setKey(key);
		optionRepresentation.setValue(value);
		optionRepresentation.setCategory(category);
		return optionRepresentation;
	}

	@JSONProperty(ignoreIfNull = true)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@JSONProperty(ignoreIfNull = true)
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@JSONProperty(ignoreIfNull = true)
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@JSONProperty(ignore = true)
	public boolean isTrue() {
		return "true".equalsIgnoreCase(getValue());
	}

	@JSONProperty(ignore = true)
	public boolean isFalse() {
		return !isTrue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OptionRepresentation other = (OptionRepresentation) obj;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
