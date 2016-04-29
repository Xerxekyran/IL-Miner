package de.digitalforger.epqLearner.util.traceSource;

import java.util.LinkedList;

import de.digitalforger.epqLearner.event.GenericEvent;

public class HistoricalTrace {
	LinkedList<GenericEvent> events = new LinkedList<GenericEvent>();
	boolean positiveTrace = false;

	/**
	 * 
	 * @param events
	 * @param positiveTrace
	 */
	public HistoricalTrace(LinkedList<GenericEvent> events, boolean positiveTrace) {
		super();
		this.events = events;
		this.positiveTrace = positiveTrace;
	}

	/**
	 * @return the events
	 */
	public LinkedList<GenericEvent> getEvents() {
		return events;
	}

	/**
	 * @param events
	 *            the events to set
	 */
	public void setEvents(LinkedList<GenericEvent> events) {
		this.events = events;
	}

	/**
	 * @return the positiveTrace
	 */
	public boolean isPositiveTrace() {
		return positiveTrace;
	}

	/**
	 * @param positiveTrace
	 *            the positiveTrace to set
	 */
	public void setPositiveTrace(boolean positiveTrace) {
		this.positiveTrace = positiveTrace;
	}

	@Override
	public String toString() {
		String ret = "TestTrace (";

		for (GenericEvent e : getEvents()) {
			ret += e.toString();
		}

		return ret + ")";
	}
}
