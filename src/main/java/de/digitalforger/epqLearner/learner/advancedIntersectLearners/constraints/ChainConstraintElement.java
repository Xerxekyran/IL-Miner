package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.LinkedList;

import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;

/**
 * One element in an constraint that stores the event it is based upon
 * 
 * @author george
 * 
 */
public class ChainConstraintElement {
	private OrderElement belongsToElement = null;
	private LinkedList<GenericEvent> basedUponEvents = new LinkedList<GenericEvent>();
	private String attributeName = "";

	/**
	 * 
	 * @param belongsTo
	 * @param attrName
	 */
	public ChainConstraintElement(OrderElement belongsTo, String attrName) {
		this.belongsToElement = belongsTo;
		this.attributeName = attrName;
	}

	/**
	 * 
	 * @param belongsTo
	 * @param attrName
	 * @param basedUponEvent
	 */
	public ChainConstraintElement(OrderElement belongsTo, String attrName, GenericEvent basedUponEvent) {
		this.belongsToElement = belongsTo;
		this.attributeName = attrName;
		this.basedUponEvents.add(basedUponEvent);
	}

	/**
	 * 
	 * @param belongsTo
	 * @param attrName
	 * @param basedUponEvents
	 */
	public ChainConstraintElement(OrderElement belongsTo, String attrName, LinkedList<GenericEvent> basedUponEvents) {
		this.belongsToElement = belongsTo;
		this.attributeName = attrName;
		this.basedUponEvents = basedUponEvents;
	}

	/**
	 * 
	 * @param currentElement
	 * @return
	 */
	public boolean equalsIgnoringEvents(ChainConstraintElement other) {
		boolean ret = false;

		// check for the same order element
		if (this.belongsToElement == other.getBelongsToElement() && this.attributeName.equals(other.getAttributeName())) {
			ret = true;
		}

		return ret;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean equalsWithEventsCanBeFoundIn(ChainConstraintElement other) {
		boolean ret = false;

		if (equalsIgnoringEvents(other)) {
			// now check if the events do match also
			if (other.getBasedUponEvents().containsAll(getBasedUponEvents())) {
				ret = true;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param ev
	 *            the event to be added to the basedUponList
	 * @param removeDuplicates
	 *            true if no duplicate events should be added to this
	 *            chainElement. if false is given, the event is simply added
	 *            without any further check
	 */
	public void addBasedUponEvents(GenericEvent ev, boolean removeDuplicates) {
		if (!removeDuplicates) {
			basedUponEvents.add(ev);
		} else {
			// check if we already have this event in the list before adding
			// here
			boolean exists = false;
			for (GenericEvent thisEvent : getBasedUponEvents()) {
				if (thisEvent == ev) {
					exists = true;
				}
			}
			if (!exists) {
				basedUponEvents.add(ev);
			}

		}

	}

	/**
	 * @return the basedUponEvents
	 */
	public LinkedList<GenericEvent> getBasedUponEvents() {
		return basedUponEvents;
	}

	/**
	 * @param basedUponEvents
	 *            the basedUponEvents to set
	 */
	public void setBasedUponEvents(LinkedList<GenericEvent> basedUponEvents) {
		this.basedUponEvents = basedUponEvents;
	}

	/**
	 * @return the belongsToElement
	 */
	public OrderElement getBelongsToElement() {
		return belongsToElement;
	}

	/**
	 * @param belongsToElement
	 *            the belongsToElement to set
	 */
	public void setBelongsToElement(OrderElement belongsToElement) {
		this.belongsToElement = belongsToElement;
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

	@Override
	public String toString() {
		return "ChainElement [" + belongsToElement.getEventInstance().getEventTypeName() + "_" + belongsToElement.getOrderElementIndex() + "." + attributeName + "]";
	}
}
