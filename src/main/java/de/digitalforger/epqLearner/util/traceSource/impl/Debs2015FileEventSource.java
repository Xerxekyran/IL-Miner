package de.digitalforger.epqLearner.util.traceSource.impl;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.impl.Debs2015RowReader;
import de.digitalforger.epqLearner.util.traceSource.FileEventSource;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * 
 * @author george
 *
 */
public class Debs2015FileEventSource extends FileEventSource {
	private static Logger logger = Logger.getLogger(Debs2015FileEventSource.class.getName());

	/**
	 * 
	 * @param pathToFileSource
	 * @param foundMatches
	 * @param deltaT
	 * @throws FileNotFoundException
	 */
	public Debs2015FileEventSource(String pathToFileSource, LinkedList<Long[]> foundMatches, long deltaT) throws FileNotFoundException {
		super(pathToFileSource, new Debs2015RowReader(), new TraceChunkerFromMatches(foundMatches), deltaT);		
		logger.info("Initializing Debs2015FileEventSource done");		
	}
	
	@Override
	protected void continueFileReading() {
		String row = "";

		try {
			while (null != (row = fileReader.readLine())) {
				// read the file and convert it into an event object
				GenericEvent curEvent = sourceFileRowReader.fileRowToGenericEvent(row);
				if(curEvent == null) {
					continue;
				}
				historyOfReadEvents.add(curEvent);

				// checks the size of the history queue and adjust if needed
				checkHistoryOfEventsQueue();

				// ask the trace chunker if this new event leads to a new trace
				if (traceChunker.nextEventTriggersNewTrace(curEvent)) {

					Long[] lastSuccessfullMatch = ((TraceChunkerFromMatches) traceChunker).getLastSuccessfullMatch();
					LinkedList<GenericEvent> traceEvents = new LinkedList<GenericEvent>(getHistoryOfEventsAccordingToMatch(lastSuccessfullMatch, historyOfReadEvents));
					HistoricalTrace newTrace = new HistoricalTrace(traceEvents, true);

					// we have a new trace, add it to the list of available
					// traces and stop finding new traces for now
					this.availableTraces.add(newTrace);
					break;
				}
			}
		} catch (Exception e) {
			logger.severe("Error reading data file: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * +
	 * @param lastSuccessfullMatch
	 * @param historyOfReadEvents
	 * @return
	 */
	private Collection<GenericEvent> getHistoryOfEventsAccordingToMatch(Long[] lastSuccessfullMatch, LinkedList<GenericEvent> historyOfReadEvents) {
		LinkedList<GenericEvent> ret = new LinkedList<GenericEvent>();
		for(GenericEvent historyEv : historyOfReadEvents) {
			if(historyEv.getTimestamp() >= lastSuccessfullMatch[0]) {
				ret.add(historyEv);
			}
		}
		
		return ret;
	}
}
