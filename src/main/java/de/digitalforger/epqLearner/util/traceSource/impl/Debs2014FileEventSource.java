package de.digitalforger.epqLearner.util.traceSource.impl;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.util.fileConverter.impl.Debs2014RowReader;
import de.digitalforger.epqLearner.util.traceSource.FileEventSource;

/**
 * 
 * @author george
 *
 */
public class Debs2014FileEventSource extends FileEventSource {
	private static Logger logger = Logger.getLogger(Debs2014FileEventSource.class.getName());

	/**
	 * 
	 * @param pathToFileSource
	 * @param foundMatches
	 * @param deltaT
	 * @throws FileNotFoundException
	 */
	public Debs2014FileEventSource(String pathToFileSource, LinkedList<Long[]> foundMatches, long deltaT) throws FileNotFoundException {
		super(pathToFileSource, new Debs2014RowReader(), new TraceChunkerFromMatches(foundMatches), deltaT);
		
		logger.info("Initializing Debs2014FileEventSource done");
	}
}
