package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

/**
 * 
 * @author george
 *
 */
public class RelationshipConstraintPartner {
	String fromAttributeName = "";
	String toAttributeName = "";

	String fromEventType = "";
	String toEventType = "";

	/**
	 * @param fromAttributeName
	 * @param toAttributeName
	 * @param fromEventType
	 * @param toEventType
	 */
	public RelationshipConstraintPartner(String fromAttributeName, String toAttributeName, String fromEventType, String toEventType) {
		super();
		this.fromAttributeName = fromAttributeName;
		this.toAttributeName = toAttributeName;
		this.fromEventType = fromEventType;
		this.toEventType = toEventType;
	}

	/**
	 * @return the fromAttributeName
	 */
	public String getFromAttributeName() {
		return fromAttributeName;
	}

	/**
	 * @param fromAttributeName
	 *            the fromAttributeName to set
	 */
	public void setFromAttributeName(String fromAttributeName) {
		this.fromAttributeName = fromAttributeName;
	}

	/**
	 * @return the toAttributeName
	 */
	public String getToAttributeName() {
		return toAttributeName;
	}

	/**
	 * @param toAttributeName
	 *            the toAttributeName to set
	 */
	public void setToAttributeName(String toAttributeName) {
		this.toAttributeName = toAttributeName;
	}

	/**
	 * @return the fromEventType
	 */
	public String getFromEventType() {
		return fromEventType;
	}

	/**
	 * @param fromEventType
	 *            the fromEventType to set
	 */
	public void setFromEventType(String fromEventType) {
		this.fromEventType = fromEventType;
	}

	/**
	 * @return the toEventType
	 */
	public String getToEventType() {
		return toEventType;
	}

	/**
	 * @param toEventType
	 *            the toEventType to set
	 */
	public void setToEventType(String toEventType) {
		this.toEventType = toEventType;
	}

}
