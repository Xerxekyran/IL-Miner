package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.LinkedList;

import de.digitalforger.epqLearner.event.GenericEvent;

/**
 * 
 * @author george
 *
 */
public class EventMappingsForOneElementConstraint {
	private LinkedList<GenericEvent> mappings = new LinkedList<GenericEvent>();
	private ConstantConstraint constantConstraint = null;

	/**
	 * 
	 * @param mappings
	 * @param chainConstraint
	 */
	public EventMappingsForOneElementConstraint(ConstantConstraint constantConstraint) {
		this.constantConstraint = constantConstraint;
	}

	public LinkedList<GenericEvent> getMappings() {
		return mappings;
	}

	public ConstantConstraint getConstantConstraint() {
		return constantConstraint;
	}

	public void addEventMapping(GenericEvent eventForConstraint) {
		mappings.add(eventForConstraint);
	}

}
