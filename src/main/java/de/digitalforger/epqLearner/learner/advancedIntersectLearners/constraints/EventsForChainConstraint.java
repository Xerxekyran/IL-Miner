package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import de.digitalforger.epqLearner.event.GenericEvent;

public class EventsForChainConstraint {
	public GenericEvent fromEvent = null;
	public GenericEvent toEvent = null;

	/**
	 * 
	 * @param fromEvent
	 * @param toEvent
	 */
	public EventsForChainConstraint(GenericEvent fromEvent, GenericEvent toEvent) {
		this.fromEvent = fromEvent;
		this.toEvent = toEvent;		
	}
}
