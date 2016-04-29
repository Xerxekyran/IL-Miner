package de.digitalforger.epqLearner.PML.constraints.order;

import java.util.HashMap;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * A MatchedEventInstanceOrder holds for each given trace and EventInstanceOrder
 * the Events that belong to each Element in the EventInstanceOrder
 * 
 * HashMap<EventInstanceOrder, LinkedList<LinkedList<LinkedList<GenericEvent>>>>
 * 
 * @author george
 * 
 */
public class MatchedEventInstanceOrder {

	private static Logger logger = Logger.getLogger(MatchedEventInstanceOrder.class.getName());

	private HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = new HashMap<HistoricalTrace, MatchedOrderForOneTrace>();

	/**
	 * @return the orderForTraces
	 */
	public HashMap<HistoricalTrace, MatchedOrderForOneTrace> getOrderForTraces() {
		return orderForTraces;
	}

	/**
	 * 
	 * @param trace
	 * @param mo
	 */
	public void addNewMatchedOrder(HistoricalTrace trace, MatchedOrderForOneTrace mo) {
		if (orderForTraces.get(trace) != null) {
			logger.warning("Overwriting an already found MatchedOrder.");
		}
		orderForTraces.put(trace, mo);
	}
}
