package de.digitalforger.epqLearner.ModelToText;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.ModelToText.Esper.EsperFileGenerator;
import de.digitalforger.epqLearner.ModelToText.SES.SESFileGenerator;

/**
 * 
 * @author george
 *
 */
public class FileGeneratorFactory {

	private static Logger logger = Logger.getLogger(FileGeneratorFactory.class.getName());

	private static FileGeneratorFactory instance = null;

	/**
	 * private to enable singleton pattern
	 */
	private FileGeneratorFactory() {

	}

	/**
	 * 
	 * @return
	 */
	public static FileGeneratorFactory getInstance() {
		if (instance == null) {
			instance = new FileGeneratorFactory();
		}

		return instance;
	}

	/**
	 * 
	 * @param format
	 * @return
	 * @throws Exception
	 */
	public IFileGenerator getFileGenerator(EOutputFormats format) {
		IFileGenerator generator = null;

		switch (format) {
		case SES:
			generator = new SESFileGenerator();
			break;
		case Esper:
			generator = new EsperFileGenerator();
			break;
		default:
			logger.log(Level.SEVERE, "This output format is not supported: " + format.toString());
		}

		return generator;

	}

}
