package de.digitalforger.epqLearner.util.traceSource.impl;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.impl.DublinRowReader;
import de.digitalforger.epqLearner.util.traceSource.FileEventSource;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * 
 * @author george
 *
 */
public class DublinFileEventSource extends FileEventSource {
	private static Logger logger = Logger.getLogger(DublinFileEventSource.class.getName());
	private List<String> filesToRead;
	private int indexOfReadFiles = 0;

	/**
	 * 
	 * @param pathToFileSource
	 * @param foundMatches
	 * @param deltaT
	 * @throws FileNotFoundException
	 */
	public DublinFileEventSource(List<String> filesToRead, LinkedList<Long[]> foundMatches, long deltaT) throws FileNotFoundException {
		super(filesToRead.get(0), new DublinRowReader(), new TraceChunkerFromMatches(foundMatches), deltaT);
		this.filesToRead = filesToRead;
		continueFileReading();
	}

	@Override
	protected void continueFileReading() {
		// this check is needed, as the superclass calls this method too early
		if(filesToRead == null) {
			return;			
		}
		
		String row = "";
		boolean foundCE = false;
		try {
			while (null != (row = fileReader.readLine())) {
				// read the file and convert it into an event object
				GenericEvent curEvent = sourceFileRowReader.fileRowToGenericEvent(row);
				historyOfReadEvents.add(curEvent);

				// checks the size of the history queue and adjust if needed
				checkHistoryOfEventsQueue();

				// ask the trace chunker if this new event leads to a new trace
				foundCE = traceChunker.nextEventTriggersNewTrace(curEvent);
				if (foundCE) {

					Long[] lastSuccessfullMatch = ((TraceChunkerFromMatches) traceChunker).getLastSuccessfullMatch();
					LinkedList<GenericEvent> traceEvents = new LinkedList<GenericEvent>(getHistoryOfEventsAccordingToMatch(lastSuccessfullMatch, historyOfReadEvents));
					HistoricalTrace newTrace = new HistoricalTrace(traceEvents, true);

					// we have a new trace, add it to the list of available
					// traces and stop finding new traces for now
					this.availableTraces.add(newTrace);
					break;
				}
			}
			
			if(row == null && !foundCE && indexOfReadFiles < filesToRead.size()-1) {
				indexOfReadFiles++;
				openSource(filesToRead.get(indexOfReadFiles));
				continueFileReading();
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
