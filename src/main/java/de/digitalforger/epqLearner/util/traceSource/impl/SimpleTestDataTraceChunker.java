package de.digitalforger.epqLearner.util.traceSource.impl;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.traceSource.ITraceChunker;

/**
 * Implementation of a trace chunker for simple test data
 * 
 * @author george
 *
 */
public class SimpleTestDataTraceChunker implements ITraceChunker {
	public static String TOKEN_FOR_SITUATION_OF_INTEREST = "situationofinterest";
	
	@Override
	public boolean nextEventTriggersNewTrace(GenericEvent curEvent) {
		return curEvent.getTypeName().toLowerCase().equals(TOKEN_FOR_SITUATION_OF_INTEREST);
	}
}
