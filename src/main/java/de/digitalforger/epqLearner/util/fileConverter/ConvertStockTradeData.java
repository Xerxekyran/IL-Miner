package de.digitalforger.epqLearner.util.fileConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.impl.StockTradesRowReader;

/**
 * 
 * @author george
 *
 */
public class ConvertStockTradeData {
	/**
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 */
	public void sortByDate(String inputFileName, String outputFileName) {
		StockTradesRowReader rowReader = new StockTradesRowReader();
		rowReader.DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = false;

		String row = "";

		PriorityQueue<GenericEvent> sortedList = new PriorityQueue<GenericEvent>(new Comparator<GenericEvent>() {
			@Override
			public int compare(GenericEvent o1, GenericEvent o2) {
				Long l1 = o1.getTimestamp();
				Long l2 = o2.getTimestamp();
				return l1.compareTo(l2);
			}
		});

		System.out.println("Start reading file [" + inputFileName + "]...");
		try {
			// read the input file and sort it on the fly (sorted set)
			BufferedReader reader = new BufferedReader(new FileReader(new File(inputFileName)));
			while (null != (row = reader.readLine())) {
				GenericEvent ev = rowReader.fileRowToGenericEvent(row);
				if (ev != null) {
					sortedList.add(ev);
				}
			}

			reader.close();
			System.out.println("Reading complete.");

			// write the sorted set back to a file
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFileName)));

			System.out.println("Writer file header");
			writer.write("<ticker>,<per>,<date>,<open>,<high>,<low>,<close>,<vol>");
			writer.newLine();
			SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddkkmm");

			System.out.println("Start writing events to resulting file [" + outputFileName + "]...");
			// AAME,I,201011011105,1.7,1.7,1.7,1.7,225
			GenericEvent e = sortedList.poll();

			// for (GenericEvent e : sortedSet) {
			while (e != null) {
				Map<String, Attribute> attributes = e.getAttributes();
				String ticker = attributes.get("ticker").getValue().getStringValue();
				String per = attributes.get("per").getValue().getStringValue();
				Date date = new Date(e.getTimestamp());
				Double open = attributes.get("open").getValue().getDoubleValue();
				Double high = attributes.get("high").getValue().getDoubleValue();
				Double low = attributes.get("low").getValue().getDoubleValue();
				Double close = attributes.get("close").getValue().getDoubleValue();
				Long vol = attributes.get("vol").getValue().getLongValue();

				writer.append(ticker);
				writer.append(',');
				writer.append(per);
				writer.append(',');
				writer.append(ft.format(date));
				writer.append(',');
				writer.append(open + ",");
				writer.append(high + ",");
				writer.append(low + ",");
				writer.append(close + ",");
				writer.append(vol + "");
				writer.newLine();

				e = sortedList.poll();
			}
			System.out.println("Writing events completed.");
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ConvertStockTradeData exec = new ConvertStockTradeData();
		// exec.sortByDate("data/Intraday_NASDAQ_MS8/NASDAQ_20101101_30.txt",
		// "data/Intraday_NASDAQ_MS8_sorted/NASDAQ_20101101_30.txt");
		// exec.sortByDate("data/Intraday_NASDAQ_MS8/NASDAQ_20101102_30.txt",
		// "data/Intraday_NASDAQ_MS8_sorted/NASDAQ_20101102_30.txt");
		// exec.sortByDate("data/Intraday_NASDAQ_MS8/NASDAQ_20101103_30.txt",
		// "data/Intraday_NASDAQ_MS8_sorted/NASDAQ_20101103_30.txt");
		// exec.sortByDate("data/Intraday_NASDAQ_MS8/NASDAQ_20101104_30.txt",
		// "data/Intraday_NASDAQ_MS8_sorted/NASDAQ_20101104_30.txt");

		DecimalFormat df = new DecimalFormat("00");
		for (int index = 1; index <= 30; index++) {

			exec.sortByDate("data/Intraday_NASDAQ_MS8/NASDAQ_201011" + df.format(index).toString() + "_1.txt", "data/Intraday_NASDAQ_MS8_sorted/NASDAQ_201011"
					+ df.format(index).toString() + "_1.txt");

		}
	}
}
