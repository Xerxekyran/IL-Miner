package de.digitalforger.epqLearner.PML.constraints.order;

import java.util.LinkedList;

import de.digitalforger.epqLearner.event.GenericEvent;

/**
 * A MatchedElement stores an OrderElement and all events where the
 * EventInstance of the OrderElement can be applied to.
 * 
 * @author george
 * 
 */
public class MatchedElement {
	private LinkedList<GenericEvent> matchedEvents = new LinkedList<GenericEvent>();
	private OrderElement orderElement = null;

	/**
	 * @return the matchedEvents
	 */
	public LinkedList<GenericEvent> getMatchedEvents() {		
		return matchedEvents;
	}

	/**
	 * @param matchedEvents
	 *            the matchedEvents to set
	 */
	public void setMatchedEvents(LinkedList<GenericEvent> matchedEvents) {
		this.matchedEvents = matchedEvents;
	}

	/**
	 * @return the orderElement
	 */
	public OrderElement getOrderElement() {
		return orderElement;
	}

	/**
	 * @param orderElement
	 *            the orderElement to set
	 */
	public void setOrderElement(OrderElement orderElement) {
		this.orderElement = orderElement;
	}

	/**
	 * 
	 * @return
	 */
	public MatchedElement cloneWithNewList() {
		MatchedElement ret = new MatchedElement();
		ret.getMatchedEvents().addAll(getMatchedEvents());
		ret.orderElement = getOrderElement();
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("ME{" + getOrderElement() + "}[");

		for (GenericEvent e : matchedEvents) {
			ret.append(e + ",");
		}

		ret.append("]");
		return ret.toString();
	}
}
