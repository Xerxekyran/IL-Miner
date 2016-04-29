package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.LinkedList;

import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;

/**
 * 
 * @author george
 *
 */
public class ConstantConstraint extends AbstractConstraint {
	private OrderElement belongsToOrderElement;
	private LinkedList<GenericEvent> basedUponEvents = new LinkedList<GenericEvent>();
	private String attributeName = "";
	private Value constantConstraintValue;

	/**
	 * 
	 * @param belongsToOrderElement
	 * @param attributeName
	 * @param constantConstraintValue
	 * @param startingEvent
	 * @param operator
	 */
	public ConstantConstraint(OrderElement belongsToOrderElement, String attributeName, Value constantConstraintValue, GenericEvent startingEvent,
			EConstraintOperator operator) {
		this.belongsToOrderElement = belongsToOrderElement;
		this.attributeName = attributeName;
		this.constantConstraintValue = constantConstraintValue;
		this.operator = operator;
		addBasedUponEvent(startingEvent);
	}

	/**
	 * 
	 * @param belongsToOrderElement
	 * @param basedUponEvents
	 * @param attributeName
	 * @param constantConstraintValue
	 */
	public ConstantConstraint(OrderElement belongsToOrderElement, String attributeName, Value constantConstraintValue, LinkedList<GenericEvent> basedUponEvents,
			EConstraintOperator operator) {
		super();
		this.belongsToOrderElement = belongsToOrderElement;
		this.attributeName = attributeName;
		this.constantConstraintValue = constantConstraintValue;
		this.operator = operator;
		this.basedUponEvents = basedUponEvents;
	}

	public EConstraintOperator getOperator() {
		return operator;
	}

	public void setOperator(EConstraintOperator operator) {
		this.operator = operator;
	}

	/**
	 * 
	 * @param ev
	 */
	public void addBasedUponEvent(GenericEvent ev) {
		basedUponEvents.add(ev);
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
	 * @return the basedUponEvents
	 */
	public LinkedList<GenericEvent> getBasedUponEvents() {
		return basedUponEvents;
	}

	/**
	 * 
	 */
	public ConstantConstraint cloneWithNewBasedUponEventList() {
		LinkedList<GenericEvent> basedUponList = new LinkedList<GenericEvent>();
		basedUponList.addAll(basedUponEvents);

		ConstantConstraint clone = new ConstantConstraint(belongsToOrderElement, attributeName, constantConstraintValue, basedUponList, operator);

		return clone;
	}

	@Override
	public String toString() {
		String operatorString = null;
		switch (getOperator()) {
		case Equal:
			operatorString = "=";
			break;
		case GreaterThan:
			operatorString = ">";
			break;
		case GreaterThanEqual:
			operatorString = ">=";
			break;
		case LessThan:
			operatorString = "<";
			break;
		case LessThanEqual:
			operatorString = "<=";
			break;
		case Unequal:
			operatorString = "!=";
			break;
		}

		return "ConstantConstraint: {" + belongsToOrderElement.getEventInstance().getEventTypeName() + "_" + belongsToOrderElement.getOrderElementIndex() + ", "
				+ attributeName + " " + operatorString + " " + constantConstraintValue + " based upon " + basedUponEvents.size() + " events}";
	}

}
