package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

public class EventInstanceOccurenceCountLearner implements ILearner {
	private static Logger logger = Logger.getLogger(EventInstanceOccurenceCountLearner.class.getName());

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the EventInstanceOccurenceCountLearner");

		LinkedList<EventInstance> relevantEventInstances = ctx.getRelevantEventInstances();

		HashMap<EventInstance, Integer> occurrenceCount = new HashMap<EventInstance, Integer>();

		StringBuilder occurrenceString = new StringBuilder();
		for (EventInstance e : relevantEventInstances) {
			occurrenceCount.put(e, countOccurrence(e, ctx.getPositiveTraces()));

			occurrenceString.append(e + "{" + occurrenceCount.get(e) + "} ");
		}

		// set the learned count to the context object
		ctx.setEventInstanceOccurrenceCount(occurrenceCount);

		logger.log(Level.FINE, "Event Type Occurrence Count: " + occurrenceString.toString());
	}

	/**
	 * 
	 * @param e
	 * @param positiveTraces
	 * @return
	 */
	private int countOccurrence(EventInstance e, LinkedList<HistoricalTrace> positiveTraces) {
		int minCounter = Integer.MAX_VALUE;

		for (HistoricalTrace trace : positiveTraces) {
			int currentCount = 0;
			for (GenericEvent ev : trace.getEvents()) {
				if (e.attributesWithValuesAreContainedIn(ev)) {
					currentCount++;
				}
			}
			if (currentCount < minCounter) {
				minCounter = currentCount;
			}
		}

		return minCounter;
	}
}
