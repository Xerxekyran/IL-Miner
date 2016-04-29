package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.LinkedList;

/**
 * 
 * @author george
 *
 */
public class EventMappingsForTwoElementConstraint {
	private LinkedList<EventsForChainConstraint> mappings = new LinkedList<EventsForChainConstraint>();

	/**
	 * 
	 * @param mappings
	 * @param chainConstraint
	 */
	public EventMappingsForTwoElementConstraint(ChainConstraint chainConstraint) {
		this.chainConstraint = chainConstraint;
	}

	private ChainConstraint chainConstraint = null;

	public LinkedList<EventsForChainConstraint> getMappings() {
		return mappings;
	}

	public ChainConstraint getChainConstraint() {
		return chainConstraint;
	}

	public void addEventMapping(EventsForChainConstraint eventsForChainConstraint) {
		mappings.add(eventsForChainConstraint);
	}

}
