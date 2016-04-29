package de.digitalforger.epqLearner.util.traceSource.impl;

import java.util.LinkedList;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.traceSource.ITraceChunker;

public class TraceChunkerFromMatches implements ITraceChunker {

	private LinkedList<Long[]> matches = null;
	private Long[] lastSuccessfullMatch = null;

	/**
	 * 
	 * @param foundMatches
	 */
	public TraceChunkerFromMatches(LinkedList<Long[]> foundMatches) {
		this.matches = foundMatches;
	}

	/**
	 * @return the lastSuccessfullMatch
	 */
	public Long[] getLastSuccessfullMatch() {
		return lastSuccessfullMatch;
	}

	@Override
	public boolean nextEventTriggersNewTrace(GenericEvent curEvent) {
		boolean ret = false;
		LinkedList<Long[]> toDelete = new LinkedList<Long[]>();
		for (Long[] curMatch : matches) {
			// check if the current event lies behind the match end data
			if (curEvent.getTimestamp() >= curMatch[1]) {
				ret = true;
				toDelete.add(curMatch);
				lastSuccessfullMatch = curMatch;
			}

			// if the match begin is in the future we do not have to check them
			// further
			if (curEvent.getTimestamp() < curMatch[0]) {
				break;
			}
		}

		matches.removeAll(toDelete);

		return ret;
	}

}
