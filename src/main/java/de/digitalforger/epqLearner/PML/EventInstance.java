package de.digitalforger.epqLearner.PML;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;

/**
 * An Event instance is a property based description of an event.
 * 
 * @author george
 * 
 */
public class EventInstance implements Comparable<EventInstance>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3218038227706640132L;

	public static String PREFIX_FOR_EVENTTYPE_NAME = "##";
	
	private String eventTypeName = "";
	private HashMap<String, Value> relevantEventProperties;
	private Integer occurrenceCount = -1;

	/**
	 * 
	 */
	public EventInstance(String eventTypeName) {
		this.relevantEventProperties = new HashMap<String, Value>();
		this.eventTypeName = eventTypeName;
	}

	/**
	 * 
	 * @param relevantEventProperties
	 */
	public EventInstance(HashMap<String, Value> relevantEventProperties, String eventTypeName) {
		this.relevantEventProperties = relevantEventProperties;
		this.eventTypeName = eventTypeName;
	}

	/**
	 * @return the relevantNominalEventProperties
	 */
	public HashMap<String, Value> getRelevantEventProperties() {
		return relevantEventProperties;
	}

	/**
	 * @param relevantEventPropertiesWithValue
	 *            the relevantEventPropertiesWithValue to set
	 */
	public void setRelevantEventPropertiesWithValue(HashMap<String, Value> relevantEventPropertiesWithValue) {
		this.relevantEventProperties = relevantEventPropertiesWithValue;
	}

	/**
	 * @return the occurrenceCount
	 */
	public Integer getOccurrenceCount() {
		return occurrenceCount;
	}

	/**
	 * @param occurrenceCount
	 *            the occurrenceCount to set
	 */
	public void setOccurrenceCount(Integer occurrenceCount) {
		this.occurrenceCount = occurrenceCount;
	}

	/**
	 * @return the eventTypeName
	 */
	public String getEventTypeName() {
		return eventTypeName;
	}

	/**
	 * @param eventTypeName
	 *            the eventTypeName to set
	 */
	public void setEventTypeName(String eventTypeName) {
		this.eventTypeName = eventTypeName;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(getEventTypeName() + " [ ");

		for (String attrName : this.getRelevantEventProperties().keySet()) {
			ret.append(attrName + "=" + this.getRelevantEventProperties().get(attrName).toString() + " ");
		}
		ret.append("]");
		return ret.toString();
	}

	@Override
	public int compareTo(EventInstance o) {
		if (equals(o)) {
			return 0;
		} else {
			int attrSize = this.getRelevantEventProperties().size() - o.getRelevantEventProperties().size();
			if (attrSize == 0) {
				return -1;
			} else {
				return attrSize;
			}
		}
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean attributesWithValuesAreContainedIn(EventInstance otherRelevantEventInstance) {
		boolean ret = true;

		if (otherRelevantEventInstance == null) {
			return false;
		}

		HashMap<String, Value> otherNominalAttributes = otherRelevantEventInstance.getRelevantEventProperties();

		for (String attrName : this.getRelevantEventProperties().keySet()) {
			Value otherAttrValue = otherNominalAttributes.get(attrName);

			// if this attr is not present in the other list, its not the same
			if (otherAttrValue == null) {
				return false;
			}

			// now compare the two values of the attribute
			if (!otherAttrValue.equals(this.getRelevantEventProperties().get(attrName))) {
				return false;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean attributesWithValuesAreContainedIn(GenericEvent event) {
		boolean ret = true;

		if (event == null) {
			return false;
		}

		Map<String, Attribute> attributes = event.getAttributes();

		for (String attrName : this.getRelevantEventProperties().keySet()) {

			if (attrName.startsWith(PREFIX_FOR_EVENTTYPE_NAME)) {
				if (!event.getTypeName().equals(this.getEventTypeName())) {
					return false;
				} else {
					continue;
				}
			}

			Attribute attribute = attributes.get(attrName);

			if (attribute == null) {
				return false;
			}

			Value otherAttrValue = attribute.getValue();

			// if this attr is not present in the other list, its not the same
			if (otherAttrValue == null) {
				return false;
			}

			// now compare the two values of the attribute
			if (!otherAttrValue.equals(this.getRelevantEventProperties().get(attrName))) {
				return false;
			}
		}

		return ret;
	}

	@Override
	public boolean equals(Object other) {

		boolean ret = true;

		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		EventInstance otherRelevantEventInstance = ((EventInstance) other);

		HashMap<String, Value> otherNominalAttributes = otherRelevantEventInstance.getRelevantEventProperties();

		for (String attrName : getRelevantEventProperties().keySet()) {
			Value otherAttrValue = otherNominalAttributes.get(attrName);

			// if this attr is not present in the other list, its not the same
			if (otherAttrValue == null) {
				return false;
			}

			// now compare the two values of the attribute
			if (!otherAttrValue.equals(getRelevantEventProperties().get(attrName))) {
				return false;
			}
		}

		// now check if this object has all attributes like the other one
		for (String attrName : otherNominalAttributes.keySet()) {
			Value attrValue = this.getRelevantEventProperties().get(attrName);

			if (attrValue == null) {
				return false;
			}

			if (!attrValue.equals(otherNominalAttributes.get(attrName))) {
				return false;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param event
	 * @return
	 */
	public boolean attributesAreEqual(GenericEvent event) {
		boolean ret = true;

		if (event == null) {
			return false;
		}

		Map<String, Attribute> attributes = event.getAttributes();

		for (String attrName : this.getRelevantEventProperties().keySet()) {

			if (attrName.startsWith(PREFIX_FOR_EVENTTYPE_NAME)) {
				if (!event.getTypeName().equals(this.getEventTypeName())) {
					return false;
				} else {
					continue;
				}
			}

			Attribute attribute = attributes.get(attrName);

			if (attribute == null) {
				return false;
			}

			Value otherAttrValue = attribute.getValue();

			// if this attr is not present in the other list, its not the same
			if (otherAttrValue == null) {
				return false;
			}

			// now compare the two values of the attribute
			if (!otherAttrValue.equals(this.getRelevantEventProperties().get(attrName))) {
				return false;
			}
		}

		// now check if this object has all attributes like the other one
		for (String attrName : attributes.keySet()) {			
			
			Value attrValue = this.getRelevantEventProperties().get(attrName);

			if (attrValue == null) {
				return false;
			}

			if (!attrValue.equals(attributes.get(attrName))) {
				return false;
			}
		}

		return ret;
	}
}
