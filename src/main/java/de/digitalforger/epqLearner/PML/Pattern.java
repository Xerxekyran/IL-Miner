package de.digitalforger.epqLearner.PML;

import java.io.Serializable;
import java.util.LinkedList;

import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;
import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.event.Value;

/**
 * 
 * @author george
 *
 */
public class Pattern implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1042572280122314420L;

	private String name = "";

	private EventInstanceOrder eventOrder = null;
	private long timeWindow = Long.MIN_VALUE;
	private LinkedList<PropertyConstraint> propertyConstraints = new LinkedList<PropertyConstraint>();
	private LinkedList<RelationConstraint> relationConstraints = new LinkedList<RelationConstraint>();

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("Pattern(");
		
		ret.append("Name:"+ name + ", ");		
		ret.append("Order:["+ eventOrder +"], ");
		ret.append("Property Constraints ["+ propertyConstraints +"], ");
		ret.append("Relation Constraints ["+ relationConstraints +"]");
		
		ret.append(")");
		return ret.toString();
	}
	
	/**
	 * 
	 * @param name
	 */
	public Pattern(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return
	 */
	public Pattern copyButRemainOrderElementReferences() {
		Pattern ret = new Pattern(getName());

		ret.setTimeWindow(getTimeWindow());

		// here we take the same list and keep everything the same (a copy
		// should rely on the same order)
		ret.setEventOrder(getEventOrder());

		// the constraints should be new ones but rely on the same order element
		// as before
		LinkedList<PropertyConstraint> copiedPropConstraints = ret.getPropertyConstraints();
		for (PropertyConstraint prop : propertyConstraints) {
			copiedPropConstraints.add(new PropertyConstraint(prop.getBelongsToOrderElement(), prop.getAttributeName(), new Value(prop.getConstantConstraintValue()),
					EConstraintOperator.valueOf(prop.getConstraintOperator().toString())));
		}

		LinkedList<RelationConstraint> copiedRelationConstraints = ret.getRelationConstraints();
		for (RelationConstraint rel : relationConstraints) {
			copiedRelationConstraints.add(new RelationConstraint(rel.getFromOrderElement(), rel.getToOrderElement(), rel.getFromOrderElementAttributeName(), rel
					.getToOrderElementAttributeName(), EConstraintOperator.valueOf(rel.getConstraintOperator().toString())));
		}

		return ret;
	}

	/**
	 * @return the relationConstraints
	 */
	public LinkedList<RelationConstraint> getRelationConstraints() {
		return relationConstraints;
	}

	/**
	 * @param relationConstraints
	 *            the relationConstraints to set
	 */
	public void setRelationConstraints(LinkedList<RelationConstraint> relationConstraints) {
		this.relationConstraints = relationConstraints;
	}

	/**
	 * @return the propertyConstraints
	 */
	public LinkedList<PropertyConstraint> getPropertyConstraints() {
		return propertyConstraints;
	}

	/**
	 * @param propertyConstraints
	 *            the propertyConstraints to set
	 */
	public void setPropertyConstraints(LinkedList<PropertyConstraint> propertyConstraints) {
		this.propertyConstraints = propertyConstraints;
	}

	/**
	 * @return the eventOrder
	 */
	public EventInstanceOrder getEventOrder() {
		return eventOrder;
	}

	/**
	 * @param eventOrder
	 *            the eventOrder to set
	 */
	public void setEventOrder(EventInstanceOrder eventOrder) {
		this.eventOrder = eventOrder;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public long getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(long timeWindow) {
		this.timeWindow = timeWindow;
	}
}
