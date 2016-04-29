package de.digitalforger.epqLearner.PML.constraints;

import java.io.Serializable;

import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;

/**
 * Represents a constraints of the form: A.x < B.y
 * 
 * @author george
 * 
 */
public class RelationConstraint implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3381013794817313367L;

	private OrderElement fromOrderElement;
	private OrderElement toOrderElement;

	private String fromOrderElementAttributeName = "";
	private String toOrderElementAttributeName = "";

	private EConstraintOperator constraintOperator;

	/**
	 * 
	 * @param fromOrderElement
	 * @param toOrderElement
	 * @param fromOrderElementAttributeName
	 * @param toOrderElementAttributeName
	 * @param constraintOperator
	 */
	public RelationConstraint(OrderElement fromOrderElement, OrderElement toOrderElement, String fromOrderElementAttributeName, String toOrderElementAttributeName,
			EConstraintOperator constraintOperator) {
		super();
		this.fromOrderElement = fromOrderElement;
		this.toOrderElement = toOrderElement;
		this.fromOrderElementAttributeName = fromOrderElementAttributeName;
		this.toOrderElementAttributeName = toOrderElementAttributeName;
		this.constraintOperator = constraintOperator;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean describeTheSameConstraint(RelationConstraint other) {
		boolean ret = true;

		if (other.getToOrderElement() != getToOrderElement() || other.getFromOrderElement() != getFromOrderElement()
				|| !other.getFromOrderElementAttributeName().equals(getFromOrderElementAttributeName())
				|| !other.getToOrderElementAttributeName().equals(getToOrderElementAttributeName()) || !other.getConstraintOperator().equals(getConstraintOperator())) {
			ret = false;
		}

		return ret;
	}

	/**
	 * @return the fromOrderElement
	 */
	public OrderElement getFromOrderElement() {
		return fromOrderElement;
	}

	/**
	 * @param fromOrderElement
	 *            the fromOrderElement to set
	 */
	public void setFromOrderElement(OrderElement fromOrderElement) {
		this.fromOrderElement = fromOrderElement;
	}

	/**
	 * @return the toOrderElement
	 */
	public OrderElement getToOrderElement() {
		return toOrderElement;
	}

	/**
	 * @param toOrderElement
	 *            the toOrderElement to set
	 */
	public void setToOrderElement(OrderElement toOrderElement) {
		this.toOrderElement = toOrderElement;
	}

	/**
	 * @return the fromOrderElementAttributeName
	 */
	public String getFromOrderElementAttributeName() {
		return fromOrderElementAttributeName;
	}

	/**
	 * @param fromOrderElementAttributeName
	 *            the fromOrderElementAttributeName to set
	 */
	public void setFromOrderElementAttributeName(String fromOrderElementAttributeName) {
		this.fromOrderElementAttributeName = fromOrderElementAttributeName;
	}

	/**
	 * @return the toOrderElementAttributeName
	 */
	public String getToOrderElementAttributeName() {
		return toOrderElementAttributeName;
	}

	/**
	 * @param toOrderElementAttributeName
	 *            the toOrderElementAttributeName to set
	 */
	public void setToOrderElementAttributeName(String toOrderElementAttributeName) {
		this.toOrderElementAttributeName = toOrderElementAttributeName;
	}

	/**
	 * @return the constraintOperator
	 */
	public EConstraintOperator getConstraintOperator() {
		return constraintOperator;
	}

	/**
	 * @param constraintOperator
	 *            the constraintOperator to set
	 */
	public void setConstraintOperator(EConstraintOperator constraintOperator) {
		this.constraintOperator = constraintOperator;
	}

	@Override
	public String toString() {
		return fromOrderElement.toString() + "." + fromOrderElementAttributeName + " " + getConstraintOperator().toString() + " " + toOrderElement.toString() + "."
				+ toOrderElementAttributeName;
	}

	public boolean equalsOtherPropertyConstraint(RelationConstraint other) {
		boolean ret = false;
		// are we talking about the same order elements?
		if (other.getFromOrderElement().getOrderElementIndex().equals(getFromOrderElement().getOrderElementIndex())
				&& other.getFromOrderElement().getEventInstance() == getFromOrderElement().getEventInstance()
				&& other.getToOrderElement().getOrderElementIndex().equals(getToOrderElement().getOrderElementIndex())
				&& other.getToOrderElement().getEventInstance() == getToOrderElement().getEventInstance()) {

			// check if the constraint itself is the same
			if (getFromOrderElementAttributeName().equals(other.getFromOrderElementAttributeName()) && getConstraintOperator().equals(other.getConstraintOperator())
					&& getToOrderElementAttributeName().equals(other.getToOrderElementAttributeName())) {
				ret = true;
			}
		}

		return ret;
	}
}
