/**
 * 
 */
package de.digitalforger.epqLearner.ModelToText;

import de.digitalforger.epqLearner.PML.Pattern;

/**
 * Interface for all TextGenerators
 * 
 * @author george
 *
 */
public interface IFileGenerator {
	public void writePatternToFile(String fileName, Pattern pattern);
	public String getPatternAsString(Pattern pattern);
	public String getFileEnding();
}
