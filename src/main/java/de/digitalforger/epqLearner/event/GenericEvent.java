package de.digitalforger.epqLearner.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic event. The type is only given by a string. It contains a list of
 * attributes and a timestamp
 * 
 * @author george
 * 
 */
public class GenericEvent implements Serializable, IEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8695360142459151423L;

	private String typeName;
	private long timestamp;
	private Map<String, Attribute> attributes;

	/**
	 * 
	 * @param typeName
	 * @param timestamp
	 * @param attributes
	 */
	public GenericEvent(String typeName, long timestamp, Attribute... attributes) {
		this.typeName = typeName;
		this.attributes = new HashMap<String, Attribute>();

		if (attributes != null) {
			for (Attribute attr : attributes) {
				this.attributes.put(attr.getName(), attr);
			}
		}

		this.timestamp = timestamp;
	}

	/**
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @param typeName
	 *            the typeName to set
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp
	 *            the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes
	 *            the attributes to set
	 */
	public void setAttributes(Map<String, Attribute> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Return the value of an attribute with the given name
	 * 
	 * @param attrName
	 *            the name of an attribute of this event
	 * @return the value of the attribute with the given name
	 */
	public Value getAttributeValue(String attrName) {
		return this.attributes.get(attrName).getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GenericEvent [typeName=" + typeName + ", timestamp=" + timestamp + ", attributes=" + attributes + "]";
	}
}
