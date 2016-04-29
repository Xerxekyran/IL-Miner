package de.digitalforger.epqLearner.util.traceSource;

import de.digitalforger.epqLearner.event.GenericEvent;

public interface ITraceChunker {

	public boolean nextEventTriggersNewTrace(GenericEvent curEvent);	
}
