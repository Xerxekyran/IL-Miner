package de.digitalforger.epqLearner.PML.constraints.order;

import java.io.Serializable;

import de.digitalforger.epqLearner.PML.EventInstance;

public class OrderElement implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7885199199307913726L;
	
	private EventInstance eventInstance = null;
	private Long orderElementIndex = null;

	/**
	 * 
	 * @param eventInstance
	 * @param orderID
	 */
	public OrderElement(EventInstance eventInstance, Long orderElementIndex) {
		setEventInstance(eventInstance);
		setOrderElementIndex(orderElementIndex);
	}

	/**
	 * @return the eventInstance
	 */
	public EventInstance getEventInstance() {
		return eventInstance;
	}

	/**
	 * @param eventInstance
	 *            the eventInstance to set
	 */
	public void setEventInstance(EventInstance eventInstance) {
		this.eventInstance = eventInstance;
	}

	/**
	 * @return the orderElementIndex
	 */
	public Long getOrderElementIndex() {
		return orderElementIndex;
	}

	/**
	 * @param orderElementIndex
	 *            the orderElementIndex to set
	 */
	public void setOrderElementIndex(Long orderElementIndex) {
		this.orderElementIndex = orderElementIndex;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(orderElementIndex + ": " + eventInstance.getEventTypeName());
		ret.append(eventInstance.getRelevantEventProperties().toString());
		return ret.toString();
	}
}
