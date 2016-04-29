package de.digitalforger.epqLearner.util.fileConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import de.digitalforger.epqLearner.util.fileConverter.impl.DublinRowReader;

/**
 * 
 * @author george
 * 
 */
public class ConvertToCSV {

	private ISourceFileRowReader rowConverter = null;

	/**
	 * 
	 * @param rowConverter
	 */
	public ConvertToCSV(ISourceFileRowReader rowConverter) {
		this.rowConverter = rowConverter;
	}



	/**
	 * 
	 * @param sourceFile
	 * @param targetCSVFile
	 * @throws IOException
	 * 
	 */
	public void convertToCSVFile(String sourceFileName, String targetCSVFile, boolean rewriteTimings) {

		BufferedReader sourceFileReader = null;
		BufferedWriter targetFileWriter = null;
		BufferedWriter changedSourcetFileWriter = null;

		String changedSourceFileName = getChangedSourceFileName(sourceFileName);

		boolean isFirstRow = true;
		long cntRows = 0;

		System.out.println("Start reading from [" + sourceFileName + "] and writing to [" + targetCSVFile + "]... ");

		try {
			sourceFileReader = new BufferedReader(new FileReader(new File(sourceFileName)));
			targetFileWriter = new BufferedWriter(new FileWriter(new File(targetCSVFile)));

			if (rewriteTimings) {

				changedSourcetFileWriter = new BufferedWriter(new FileWriter(new File(changedSourceFileName)));
			}

			String row = "";

			while (null != (row = sourceFileReader.readLine())) {
				// skip the header file row, we do not use this one here
				if (isFirstRow) {
					isFirstRow = false;
					continue;
				}
				
				StringBuilder fileRow = new StringBuilder(row);
				String csvRow = rowConverter.toCSVRow(fileRow, rewriteTimings);
				cntRows++;
				targetFileWriter.append(csvRow);

				if (rewriteTimings) {
					changedSourcetFileWriter.append(fileRow);
				}
			}

			targetFileWriter.flush();
			targetFileWriter.close();

			if (rewriteTimings) {
				changedSourcetFileWriter.flush();
				changedSourcetFileWriter.close();
				System.out.println("Finished changed source file with " + cntRows + " rows [" + changedSourceFileName + "].");
			}

			System.out.println("Finished CSV file with " + cntRows + " rows.");

		} catch (Exception ex) {
			System.out.println("Error reading data file: " + ex);
			ex.printStackTrace();
		} finally {
			try {
				sourceFileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				targetFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param fileName
	 * @return
	 */
	private String getChangedSourceFileName(String fileName) {
		int lastDot = fileName.lastIndexOf(".");

		return fileName.substring(0, lastDot) + "_changed" + fileName.substring(lastDot);
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ConvertToCSV convertToCSV = new ConvertToCSV(new DublinRowReader());
		convertToCSV.convertToCSVFile("data/dublin/training.csv", "data/dublin/epmDublinTraining.csv", true);
	}
}
