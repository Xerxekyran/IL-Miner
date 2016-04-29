package de.digitalforger.epqLearner.util.traceSource.impl;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.util.fileConverter.impl.SimpleTestDataRowReader;
import de.digitalforger.epqLearner.util.traceSource.FileEventSource;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

public class SimpleTestDataFileSource extends FileEventSource {
	private static Logger logger = Logger.getLogger(SimpleTestDataFileSource.class.getName());

	/**
	 * 
	 * @param pathToFileSource
	 * @param deltaT
	 * @throws FileNotFoundException
	 */
	public SimpleTestDataFileSource(String pathToFileSource, long deltaT) throws FileNotFoundException {
		super(pathToFileSource, new SimpleTestDataRowReader(), new SimpleTestDataTraceChunker(), deltaT);
	}

	@Override
	protected void checkHistoryOfEventsQueue() {
		// we dont want to have the "situation of interest" events in the list
		if (historyOfReadEvents.getLast().getTypeName().toLowerCase().equals(SimpleTestDataTraceChunker.TOKEN_FOR_SITUATION_OF_INTEREST)) {
			historyOfReadEvents.removeLast();
		}
		super.checkHistoryOfEventsQueue();
	}

	
	
	
	/**
	 * Testing the implementation 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SimpleTestDataFileSource simpleTestDataFileSource = new SimpleTestDataFileSource("data/simpleTestData/SimpleExample1.txt", 5);

			while (simpleTestDataFileSource.hasNextTrace()) {
				HistoricalTrace nextTrace = simpleTestDataFileSource.nextTrace();

				System.out.println(nextTrace);
			}
		} catch (FileNotFoundException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}
	}
}
