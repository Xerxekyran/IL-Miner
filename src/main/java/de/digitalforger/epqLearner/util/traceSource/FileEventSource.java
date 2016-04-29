package de.digitalforger.epqLearner.util.traceSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * 
 * @author george
 *
 */
public abstract class FileEventSource implements IEventTraceSource {
	private static Logger logger = Logger.getLogger(FileEventSource.class.getName());

	protected BufferedReader fileReader = null;
	protected ISourceFileRowReader sourceFileRowReader = null;
	protected ITraceChunker traceChunker = null;
	protected LinkedList<GenericEvent> historyOfReadEvents = new LinkedList<GenericEvent>();
	protected LinkedList<HistoricalTrace> availableTraces = new LinkedList<HistoricalTrace>();
	protected long deltaT = 0L;

	/**
	 * 
	 * @param pathToFileSource
	 * @param traceChunker
	 * @throws FileNotFoundException
	 */
	public FileEventSource(String pathToFileSource, ISourceFileRowReader sourceFileRowReader, ITraceChunker traceChunker, long deltaT) throws FileNotFoundException {
		this.traceChunker = traceChunker;
		this.deltaT = deltaT;
		this.sourceFileRowReader = sourceFileRowReader;

		openSource(pathToFileSource);
		continueFileReading();
	}

	@Override
	public boolean hasNextTrace() {
		return (availableTraces.size() > 0);
	}

	@Override
	public HistoricalTrace nextTrace() {
		// read file further (until next trace is found or file source is empty)
		continueFileReading();
		return availableTraces.remove();
	}

	@Override
	public void openSource(String pathToFileSource) throws FileNotFoundException {
		if (fileReader != null) {
			try {
				closeSource();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		fileReader = new BufferedReader(new FileReader(new File(pathToFileSource)));
	}

	@Override
	public void closeSource() throws IOException {
		if (fileReader != null) {
			fileReader.close();
			fileReader = null;
		}
	}

	/**
	 * 
	 */
	protected void continueFileReading() {
		String row = "";

		try {
			while (null != (row = fileReader.readLine())) {
				// read the file and convert it into an event object
				GenericEvent curEvent = sourceFileRowReader.fileRowToGenericEvent(row);
				historyOfReadEvents.add(curEvent);

				// checks the size of the history queue and adjust if needed
				checkHistoryOfEventsQueue();

				// ask the trace chunker if this new event leads to a new trace
				if (traceChunker.nextEventTriggersNewTrace(curEvent)) {
					LinkedList<GenericEvent> traceEvents = new LinkedList<GenericEvent>(historyOfReadEvents);
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
	 * checks the size of the history queue and removes elements if needed
	 */
	protected void checkHistoryOfEventsQueue() {
		GenericEvent headOfQueue = null;
		GenericEvent lastAddedEvent = null;
		double timeWindow = 0.0;

		do {
			headOfQueue = historyOfReadEvents.getFirst();
			lastAddedEvent = historyOfReadEvents.getLast();

			// if we do not have enough events, do nothing
			if (headOfQueue == null || lastAddedEvent == null) {
				break;
			}

			timeWindow = lastAddedEvent.getTimestamp() - headOfQueue.getTimestamp();

			// remove event if the timespan is too large
			if (timeWindow > deltaT) {
				historyOfReadEvents.removeFirst();
			}
		} while (timeWindow > deltaT);

	}
}
