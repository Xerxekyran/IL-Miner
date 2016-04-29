package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.matchings.ActiveMatching;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * This learner takes the order that was learned in form of event instances and
 * matches them to the actual event instances in the trace. This way we have
 * (sets) of sequences that represent actual event instances in the traces. The
 * constraints to be learned can be more precise on these instances than on
 * event types only.
 * 
 * @author george
 * 
 */
public class MatchOrderInstancesToTraceEventsLearner implements ILearner {

	private static Logger logger = Logger.getLogger(MatchOrderInstancesToTraceEventsLearner.class.getName());

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the MatchOrderInstancesToTraceEventsLearner");

		// for each available order
		for (Pattern pattern : ctx.getPatternMatchingLanguage().getPatterns()) {

			MatchedEventInstanceOrder matchedEventOrder = matchSingleOrder(pattern.getEventOrder(), ctx.getPositiveTraces());
			ctx.getMatchedEventOrders().put(pattern, matchedEventOrder);
		}
	}

	/**
	 * 
	 * @param ctx
	 * @param p
	 * @param t
	 * @return
	 */
	public MatchedOrderForOneTrace performOneStepForOneTraceAndPattern(Pattern p, HistoricalTrace t) {
		return matchSingleOrderForOneTrace_combineAllPossibleMatches(p.getEventOrder(), t);
	}

	/**
	 * 
	 * @param order
	 * @param positiveTraces
	 * @return
	 */
	private MatchedEventInstanceOrder matchSingleOrder(EventInstanceOrder order, LinkedList<HistoricalTrace> positiveTraces) {
		MatchedEventInstanceOrder matchingEventsForAllOrderEntries = new MatchedEventInstanceOrder();

		if (order == null || order.size() < 1) {
			logger.log(Level.WARNING, "Tried to match an order without elements: " + order);
			return matchingEventsForAllOrderEntries;
		}

		// for each test trace
		for (HistoricalTrace t : positiveTraces) {
			// matchingEventsForAllOrderEntries.addNewMatchedOrder(t,
			// matchSingleOrderForOneTrace(order, t));
			matchingEventsForAllOrderEntries.addNewMatchedOrder(t, matchSingleOrderForOneTrace_combineAllPossibleMatches(order, t));
		}

		return matchingEventsForAllOrderEntries;
	}

	/**
	 * 
	 * @param order
	 * @param t
	 * @return
	 */
	private MatchedOrderForOneTrace matchSingleOrderForOneTrace_combineAllPossibleMatches(EventInstanceOrder order, HistoricalTrace t) {
		// every element in the order can possibly be matched against multiple
		// events in the trace
		MatchedOrderForOneTrace ret = new MatchedOrderForOneTrace();

		if (order == null || order.size() < 1) {
			logger.log(Level.WARNING, "Tried to match an order without elements: " + order);
			return ret;
		}

		HashMap<Integer, ActiveMatching> activeMatchingsAtOrderPosition = new HashMap<Integer, ActiveMatching>();
		activeMatchingsAtOrderPosition.put(0, new ActiveMatching(order));

		for (GenericEvent e : t.getEvents()) {

			for (ActiveMatching am : activeMatchingsAtOrderPosition.values()) {
				ActiveMatching possibleNewActiveMatching = am.nextEvent(e);

				if (possibleNewActiveMatching != null) {
					// we got a new one to add

					// first look if we already have a matching for this
					// position in the order
					ActiveMatching activeMatchingAtOrderPosition = activeMatchingsAtOrderPosition.get(possibleNewActiveMatching.getActiveOrderElementIndex());
					if (activeMatchingAtOrderPosition == null) {
						// no its a new matching for this position, we put it in
						// the map
						activeMatchingsAtOrderPosition.put(possibleNewActiveMatching.getActiveOrderElementIndex(), possibleNewActiveMatching);
					} else {
						// yes there is already an active matching, we can merge
						// their intermediate results
						if (!activeMatchingAtOrderPosition.mergeWithActiveMatchingAtSameOrderPosition(possibleNewActiveMatching)) {
							logger.warning("Couldnt merge active matchings at the same position, whats wronge here?");
						}
					}
				}
			}
		}

		for (ActiveMatching am : activeMatchingsAtOrderPosition.values()) {
			if (am.isMatchingWholeOrder()) {
				ret.mergeWithFinishedActiveMatching(am);
			}
		}
		if(ret.getMatchedElements().size() == 0) {
			logger.warning("Found no full match [order: "+ order +" IN TRACE with : "+ t.getEvents().size() +" events]");
		}

		return ret;
	}
}
