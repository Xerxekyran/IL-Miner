package de.digitalforger.epqLearner.util.fileConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

public class ConvertFileToTraces {
	private static Logger logger = Logger.getLogger(ConvertFileToTraces.class.getName());

	/**
	 * 
	 * @param dataFileWithoutLabels
	 * @param sourceRowReader
	 * @param positiveTraces
	 * @param negativeTraces
	 * @param matchTimings
	 * @param maximumPositiveTraces
	 */
	public void readTraces(String dataFileWithoutLabels, ISourceFileRowReader sourceRowReader, LinkedList<HistoricalTrace> positiveTraces,
			LinkedList<HistoricalTrace> negativeTraces, LinkedList<Long[]> matchTimings, Integer maximumPositiveTraces) {
		logger.info("Parsing traces from file (" + dataFileWithoutLabels + ")");

		HashMap<Long[], HistoricalTrace> activeTestTraces = new HashMap<Long[], HistoricalTrace>();
		int lastAddedIndex = -1;
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(new File(dataFileWithoutLabels)));
			String row = "";

			while (null != (row = fileReader.readLine())) {

				GenericEvent curEvent = sourceRowReader.fileRowToGenericEvent(row);

				// here we get the list of timing window that match to our
				// current timestamp in the data
				LinkedList<HistoricalTrace> finishedTraces = new LinkedList<HistoricalTrace>();
				lastAddedIndex = checkActiveTimings(activeTestTraces, curEvent.getTimestamp(), matchTimings, lastAddedIndex, finishedTraces);
				if (finishedTraces.size() > 0) {
					positiveTraces.addAll(finishedTraces);
				}
				if (positiveTraces.size() >= maximumPositiveTraces) {
					break;
				}

				for (Long[] activeTiming : activeTestTraces.keySet()) {
					// add the event to it
					activeTestTraces.get(activeTiming).getEvents().add(curEvent);
				}
			}

			fileReader.close();

		} catch (Exception e) {
			System.out.println("Error reading data file: " + e);
			e.printStackTrace();
		}

		logger.log(Level.FINE, "Deleting entries with no events");
		LinkedList<HistoricalTrace> toDelete = new LinkedList<HistoricalTrace>();
		for (HistoricalTrace t : positiveTraces) {
			if (t.getEvents().size() == 0) {
				toDelete.add(t);
			}
		}
		positiveTraces.removeAll(toDelete);

		logger.info("Parsing done. Found " + positiveTraces.size() + " positive traces.");
	}

	/**
	 * 
	 * @param activeTestTraces
	 * @param currentTimeTimestamp
	 * @param matchTimings
	 */
	private int checkActiveTimings(HashMap<Long[], HistoricalTrace> activeTestTraces, long currentTimeTimestamp, LinkedList<Long[]> matchTimings, int lastAddedIndex,
			LinkedList<HistoricalTrace> finishedTraces) {
		// step 1
		// first check if there is an active timing that can be removed
		LinkedList<Long[]> toDelete = new LinkedList<Long[]>();
		for (Long[] activeTiming : activeTestTraces.keySet()) {
			if (currentTimeTimestamp > activeTiming[1]) {
				finishedTraces.add(activeTestTraces.get(activeTiming));
				toDelete.add(activeTiming);
			}
		}

		for (Long[] delKey : toDelete) {
			activeTestTraces.remove(delKey);
		}

		// step 2
		// add new active timings if their time has come
		// for (Long[] possibleNewTiming : matchTimings) {
		int retVal = lastAddedIndex;
		for (int i = lastAddedIndex + 1; i < matchTimings.size(); i++) {
			Long[] possibleNewTiming = matchTimings.get(i);

			if (possibleNewTiming[0] > currentTimeTimestamp) {
				// we can stop here, because the beginning of the current timing
				// is in the future
				break;
			}

			if (currentTimeTimestamp >= possibleNewTiming[0] && currentTimeTimestamp <= possibleNewTiming[1]) {
				// check if we do not already have this one
				if (activeTestTraces.get(possibleNewTiming) == null) {
					activeTestTraces.put(possibleNewTiming, new HistoricalTrace(new LinkedList<GenericEvent>(), true));
				}
			}
			retVal = i;
		}

		return retVal;
	}
}
