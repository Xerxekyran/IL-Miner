package de.digitalforger.epqLearner.learner.intersectWithNegativeTraces;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * This intersect learner also uses negative traces to exclude certain event
 * types
 * 
 * @author george
 * 
 */
public class RelevantEventTypeLearner implements ILearner {

	private static Logger logger = Logger.getLogger(RelevantEventTypeLearner.class.getName());

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the ReleventEventTypeLearner");

		LinkedList<String> positivelyOccurredEventTypes;
		LinkedList<String> negativelyOccurredEventTypes;

		LinkedList<HistoricalTrace> positiveTraces = new LinkedList<HistoricalTrace>();
		LinkedList<HistoricalTrace> negativeTraces = new LinkedList<HistoricalTrace>();
		for (HistoricalTrace t : ctx.getPositiveTraces()) {
			if (t.isPositiveTrace()) {
				positiveTraces.add(t);
			} else {
				negativeTraces.add(t);
			}
		}
		positivelyOccurredEventTypes = getRelevantEventTypes(positiveTraces);
		negativelyOccurredEventTypes = getRelevantEventTypes(negativeTraces);

		positivelyOccurredEventTypes.removeAll(negativelyOccurredEventTypes);

		// LinkedList<EventType> ctxRelevantEventTypes =
		// ctx.getPatternMatchingLanguage().getPatterns().get(0).getRelevantEventTypes();

		// for (String evType : positivelyOccurredEventTypes) {
		// ctxRelevantEventTypes.add(new EventType(evType));
		// }

		logger.log(Level.FINE, "Relevant Event Types: " + positivelyOccurredEventTypes);
	}

	/**
	 * 
	 * @param traces
	 * @return
	 */
	private LinkedList<String> getRelevantEventTypes(LinkedList<HistoricalTrace> traces) {
		LinkedList<String> ret = new LinkedList<String>();

		if (traces.size() < 1) {
			return ret;
		}

		try {

			TreeSet<String> relevantEvTypeNames = new TreeSet<String>();

			// add the first trace without question
			for (GenericEvent e : traces.get(0).getEvents()) {
				relevantEvTypeNames.add(e.getTypeName());
			}

			for (HistoricalTrace t : traces) {
				// extract each relevant ev type
				retainAllEventTypeNames(relevantEvTypeNames, t.getEvents());
			}

			Iterator<String> iterator = relevantEvTypeNames.iterator();
			while (iterator.hasNext()) {
				ret.add(iterator.next());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * 
	 * @param evTypeNames
	 * @param newEvents
	 */
	private void retainAllEventTypeNames(TreeSet<String> evTypeNames, LinkedList<GenericEvent> newEvents) {
		LinkedList<String> newEvTypeNames = new LinkedList<String>();
		for (GenericEvent ge : newEvents) {
			newEvTypeNames.add(ge.getTypeName());
		}

		evTypeNames.retainAll(newEvTypeNames);
	}
}
