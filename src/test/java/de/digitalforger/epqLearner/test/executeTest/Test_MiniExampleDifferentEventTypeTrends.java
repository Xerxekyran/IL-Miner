package de.digitalforger.epqLearner.test.executeTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;
import de.digitalforger.epqLearner.learner.EpqlLearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;
import de.digitalforger.epqLearner.util.traceSource.impl.SimpleTestDataFileSource;

public class Test_MiniExampleDifferentEventTypeTrends {
	
	private static String LOG_FILE_NAME = "logging.properties";
	
	
	private void runTest() {
		LinkedList<HistoricalTrace> negativeTraces = new LinkedList<HistoricalTrace>();
		LinkedList<HistoricalTrace> positiveTraces = new LinkedList<HistoricalTrace>();
		
		
		try {
			SimpleTestDataFileSource simpleTestDataFileSource = new SimpleTestDataFileSource("data/simpleTestData/SimpleExampleDifferentEventTypeTrends.txt", 2);			
			while(simpleTestDataFileSource.hasNextTrace()) {
				positiveTraces.add(simpleTestDataFileSource.nextTrace());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
//		LinkedList<GenericEvent> events = new LinkedList<GenericEvent>();
//		HashMap<String, Attribute> attributes;
//		GenericEvent genericEvent;
//
//		// **********
//		// TRACE 1
//		genericEvent = new GenericEvent("A", 0);
//		attributes = new HashMap<String, Attribute>();		
//		attributes.put("value", new Attribute("value", new Value(10)));
//		genericEvent.setAttributes(attributes);
//		events.add(genericEvent);				
//		
//		genericEvent = new GenericEvent("B", 5);
//		attributes = new HashMap<String, Attribute>();		
//		attributes.put("temp", new Attribute("temp", new Value(12)));
//		genericEvent.setAttributes(attributes);
//		events.add(genericEvent);				
//		
//		HistoricalTrace t = new HistoricalTrace(events, true);
//		positiveTraces.add(t);
//		
//		// **********
//		// TRACE 2
//		events = new LinkedList<GenericEvent>();
//				
//		genericEvent = new GenericEvent("A", 0);
//		attributes = new HashMap<String, Attribute>();		
//		attributes.put("value", new Attribute("value", new Value(10)));
//		genericEvent.setAttributes(attributes);
//		events.add(genericEvent);				
//		
//		genericEvent = new GenericEvent("B", 5);
//		attributes = new HashMap<String, Attribute>();		
//		attributes.put("temp", new Attribute("temp", new Value(12)));
//		genericEvent.setAttributes(attributes);
//		events.add(genericEvent);				
//		
//		t = new HistoricalTrace(events, true);
//		positiveTraces.add(t);
		
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

		Test_MiniExampleDifferentEventTypeTrends miniExample = new Test_MiniExampleDifferentEventTypeTrends();
		miniExample.runTest();
	}


}
