package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.MinMaxValue;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

public class TimeWindowLearner implements ILearner {

	private static Logger logger = Logger.getLogger(TimeWindowLearner.class.getName());

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the TimeWindowLearner");

		// check each pattern individual
		for (Pattern p : ctx.getPatternMatchingLanguage().getPatterns()) {

			Long timeWindow = 0L;
			MatchedEventInstanceOrder matchedEventInstanceOrderForTrace = ctx.getMatchedEventOrders().get(p);
			
			// look in each trace and find max and min time of the used events
			for (HistoricalTrace t : ctx.getPositiveTraces()) {
				
				MatchedOrderForOneTrace matchedOrderForOneTrace = matchedEventInstanceOrderForTrace.getOrderForTraces().get(t);

				MinMaxValue currentMinMax = readMinMaxValuesForTrace(matchedOrderForOneTrace, p);
				if ((currentMinMax.maxVal - currentMinMax.minVal) > timeWindow) {
					timeWindow = currentMinMax.maxVal - currentMinMax.minVal;
				}				
			}

			// write the difference between min and max of matched events as the
			// time window to the pattern
			if(p.getTimeWindow() <= 0L) {
				p.setTimeWindow(timeWindow);	
			}			

			logger.log(Level.FINE, "pattern gets timewindow value: " + timeWindow);
		}
	}

	/**
	 * 
	 * @param minMax
	 * @param matchedOrderForOneTrace
	 * @param p
	 * @param p
	 */
	private MinMaxValue readMinMaxValuesForTrace(MatchedOrderForOneTrace matchedOrderForOneTrace, Pattern p) {
		MinMaxValue ret = new MinMaxValue();
		if(matchedOrderForOneTrace == null || matchedOrderForOneTrace.getMatchedElements() == null || matchedOrderForOneTrace.getMatchedElements().isEmpty()) {
			logger.warning("Got no elements to work with");
			return ret;
		}
		
		LinkedList<GenericEvent> firstEvents = matchedOrderForOneTrace.getMatchedElements().getFirst().getMatchedEvents();
		LinkedList<GenericEvent> lastEvents = matchedOrderForOneTrace.getMatchedElements().getLast().getMatchedEvents();

		// the "first" events define the maximum timestamp value
		OrderElement firstOE = p.getEventOrder().get(0);
		OrderElement lastOE = p.getEventOrder().get(p.getEventOrder().size() - 1);
		for (GenericEvent e : firstEvents) {

			// does this event fulfill all constraints?
			if (eventCanBeUsed(e, p, firstOE)) {
				if (e.getTimestamp() < ret.minVal) {
					ret.minVal = e.getTimestamp();
				}
			}
		}

		// the "last" events define the maximum timestamp value
		for (GenericEvent e : lastEvents) {
			// does this event fulfill all constraints?
			if (eventCanBeUsed(e, p, lastOE)) {
				if (e.getTimestamp() > ret.maxVal) {
					ret.maxVal = e.getTimestamp();
				}
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param e
	 * @param p
	 * @return
	 */
	private boolean eventCanBeUsed(GenericEvent e, Pattern p, OrderElement forOrderElement) {
		boolean ret = true;

		// check property constraints
		for (PropertyConstraint pc : p.getPropertyConstraints()) {
			// the correct order element for the check?
			if (pc.getBelongsToOrderElement() == forOrderElement) {
				int compareTo = e.getAttributes().get(pc.getAttributeName()).getValue().compareTo(pc.getConstantConstraintValue());
				switch (pc.getConstraintOperator()) {
				case Equal:
					if (compareTo != 0) {
						return false;
					}
					break;
				case GreaterThan:
					if (compareTo <= 0) {
						return false;
					}
					break;
				case GreaterThanEqual:
					if (compareTo < 0) {
						return false;
					}
					break;
				case LessThan:
					if (compareTo > 0) {
						return false;
					}
					break;
				case LessThanEqual:
					if (compareTo >= 0) {
						return false;
					}
					break;
				case Unequal:
					if (compareTo == 0) {
						return false;
					}
					break;
				default:
					break;
				}
			}
		}

		return ret;
	}

}
