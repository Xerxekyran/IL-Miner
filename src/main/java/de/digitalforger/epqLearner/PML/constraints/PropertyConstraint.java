package de.digitalforger.epqLearner.PML.constraints;

import java.io.Serializable;

import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.Value;

/**
 * A constraint for a single attribute of an event against a constant value
 * 
 * @author george
 * 
 */
public class PropertyConstraint implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6161821966501304132L;

	private OrderElement belongsToOrderElement;
	private String attributeName = "";
	private Value constantConstraintValue;
	private EConstraintOperator constraintOperator;

	/**
	 * 
	 * @param belongsToOrderElement
	 * @param attributeName
	 * @param constantConstraintValue
	 * @param constraintOperator
	 */
	public PropertyConstraint(OrderElement belongsToOrderElement, String attributeName, Value constantConstraintValue, EConstraintOperator constraintOperator) {
		super();
		this.belongsToOrderElement = belongsToOrderElement;
		this.attributeName = attributeName;
		this.constantConstraintValue = constantConstraintValue;
		this.constraintOperator = constraintOperator;
	}

	/**
	 * @return the belongsToOrderElement
	 */
	public OrderElement getBelongsToOrderElement() {
		return belongsToOrderElement;
	}

	/**
	 * @param belongsToOrderElement
	 *            the belongsToOrderElement to set
	 */
	public void setBelongsToOrderElement(OrderElement belongsToOrderElement) {
		this.belongsToOrderElement = belongsToOrderElement;
	}

	/**
	 * @return the attributeName
	 */
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * @param attributeName
	 *            the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * @return the constantConstraintValue
	 */
	public Value getConstantConstraintValue() {
		return constantConstraintValue;
	}

	/**
	 * @param constantConstraintValue
	 *            the constantConstraintValue to set
	 */
	public void setConstantConstraintValue(Value constantConstraintValue) {
		this.constantConstraintValue = constantConstraintValue;
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
		return "PropConstr[" + this.getBelongsToOrderElement().getEventInstance().getEventTypeName() + "_" + this.getBelongsToOrderElement().getOrderElementIndex() + "."
				+ this.getAttributeName() + " " + this.getConstraintOperator() + " " + this.getConstantConstraintValue() + "]";
	}

	/**
	 * 
	 * @param next
	 * @return
	 */
	public boolean equalsOtherPropertyConstraint(PropertyConstraint other) {
		boolean ret = false;

		// are we talking about the same order element?
		if (other.getBelongsToOrderElement().getOrderElementIndex().equals(getBelongsToOrderElement().getOrderElementIndex())
				&& other.getBelongsToOrderElement().getEventInstance() == getBelongsToOrderElement().getEventInstance()) {
			// check if the constraint itself is the same
			if (getAttributeName().equals(other.getAttributeName()) && getConstraintOperator().equals(other.getConstraintOperator())
					&& getConstantConstraintValue().compareTo(other.getConstantConstraintValue()) == 0) {
				ret = true;
			}
		}

		return ret;
	}
}
