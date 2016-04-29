package de.digitalforger.epqLearner.test.executeTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.learner.EpqlLearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;
import de.digitalforger.epqLearner.util.traceSource.impl.SimpleTestDataFileSource;

public class Test_MiniExample {
	
	private static String LOG_FILE_NAME = "logging.properties";
	
	
	private void runTest() {
		LinkedList<HistoricalTrace> negativeTraces = new LinkedList<HistoricalTrace>();
		LinkedList<HistoricalTrace> positiveTraces = new LinkedList<HistoricalTrace>();
		
		try {
			SimpleTestDataFileSource simpleTestDataFileSource = new SimpleTestDataFileSource("data/simpleTestData/SimpleExample1.txt", 6);			
			while(simpleTestDataFileSource.hasNextTrace()) {
				positiveTraces.add(simpleTestDataFileSource.nextTrace());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
		
		EpqlLearner learner = new EpqlLearner(positiveTraces, negativeTraces);
		learner.executeEverything();
	
	}	
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		LogManager manager = LogManager.getLogManager();
		try {
			manager.readConfiguration(new FileInputStream(new File(LOG_FILE_NAME)));
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		ConsoleHandler consoleHandler = new ConsoleHandler();
		logger.addHandler(consoleHandler);

		Test_MiniExample miniExample = new Test_MiniExample();
		miniExample.runTest();
	}


}
