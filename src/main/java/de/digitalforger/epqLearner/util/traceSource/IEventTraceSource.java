package de.digitalforger.epqLearner.util.traceSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

/**
 * 
 * @author george
 *
 */
public interface IEventTraceSource extends Serializable {

	public void openSource(String pathToFileSource) throws FileNotFoundException;
	public void closeSource() throws IOException;

	/**
	 * returns a boolean stating if any trace is left in the source
	 * 
	 * @return
	 */
	public boolean hasNextTrace();

	/**
	 * return the next event in the current trace. null is returned if the
	 * current trace is empty, a nect call of this method will return the first
	 * event of the next trace if available
	 * 
	 * @return
	 */
	public HistoricalTrace nextTrace();

}
