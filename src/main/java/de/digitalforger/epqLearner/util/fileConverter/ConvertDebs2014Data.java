package de.digitalforger.epqLearner.util.fileConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.impl.Debs2014RowReader;

public class ConvertDebs2014Data {

	/**
	 * Splits the given data file into two different files, in order to have a
	 * training data set and an evaluation data set
	 * 
	 * @param wholeTestDataPath
	 * @param trainDataPath
	 * @param evalDataPath
	 * @throws FileNotFoundException
	 */
	public void splitTestData(String wholeTestDataPath, String trainDataPath, String evalDataPath) throws Exception {

	}

	/**
	 * 
	 * @param propertyValue
	 * @param sourceFile
	 * @param targetFile
	 */
	public void generateEvalAndTrainDataWithFilter(int propertyValue, String sourceFile, String outEvalFile, String outTrainFile) {
		String tmpFileName = "tmpFilteredFile.csv";

		try {
			System.out.println("************************");
			System.out.println("generateEvalAndTrainDataWithFilter(" + propertyValue + ", " + sourceFile + ", " + outEvalFile + ", " + outTrainFile + ")");
			System.out.println("Split a dataset into two seperated files");
			System.out.println("************************");

			System.out.println("Reading each row and filter them on property value: " + propertyValue);
			File tmpFile = new File(tmpFileName);

			// first filter the file
			BufferedReader sourceReader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "utf-8"));

			String row = "";
			int linesInFile = 0;
			while ((row = sourceReader.readLine()) != null) {
				String[] data = row.split(",");
				int property = Integer.parseInt(data[3]);
				if (property == propertyValue) {
					writer.append(row);
					writer.newLine();
					linesInFile++;
					if(linesInFile % 100000 == 0) {
						System.out.println("Next 100k lines written");
						writer.flush();
					}
					
				}
			}
			writer.close();
			sourceReader.close();
			System.out.println(linesInFile + " rows found.");

			System.out.println("Now splitting the data into two seperate files");

			// new reader
			sourceReader = new BufferedReader(new InputStreamReader(new FileInputStream(tmpFile)));

			// split the source into even peaces
			for (int i = 0; i < 2; i++) {

				if (i == 0) {
					System.out.println("Writing to file: " + outTrainFile);
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outTrainFile), "utf-8"));
				} else {
					System.out.println("Writing to file: " + outEvalFile);
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outEvalFile), "utf-8"));
				}

				int halfLinesInFile = linesInFile / 2;
				if (linesInFile % 2 != 0) {
					halfLinesInFile++;
				}

				for (long j = 0; j < halfLinesInFile; j++) {
					String readLine = sourceReader.readLine();
					if (readLine == null) {
						break;
					}

					writer.append(readLine);
					writer.newLine();
				}
				writer.close();
			}
			sourceReader.close();

			System.out.println("Reading complete.");

			boolean delete = tmpFile.delete();
			System.out.println("Cleaning up tmpFile: " + delete);
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ConvertDebs2014Data fData = new ConvertDebs2014Data();

		fData.generateEvalAndTrainDataWithFilter(0, "data/debs2014/sorted.csv", "data/debs2014/eval.csv", "data/debs2014/train.csv");

	}
}
