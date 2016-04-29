package de.digitalforger.epqLearner.util.fileConverter.impl;

import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * 
 * @author george
 *
 */
public class StockTradesRowReader implements ISourceFileRowReader {

	private static Logger logger = Logger.getLogger(StockTradesRowReader.class.getName());
	private long lastTimeStamp = 0l;
	public boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		logger.warning("Method not yet implemented!! 'Debs2014RowReader.toCSVRow()'");
		return null;
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {
		GenericEvent retEvent = null;
		try {

			String[] data = fileRow.split(",");

			if (data[0].startsWith("<ticker>")) {
				// we are in the title line, we can not create an event from
				// that
				return null;
			}
			// 0 <ticker>
			// 1 <per>
			// 2 <date>
			// 3 <open>
			// 4 <high>
			// 5 <low>
			// 6 <close>
			// 7 <vol>
			// 2010 11 01 1105
			SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddkkmm");
			long currentTimeTimestamp = ft.parse(data[2]).getTime();

			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if (currentTimeTimestamp <= lastTimeStamp) {
					currentTimeTimestamp = lastTimeStamp + 1;
				}
			}

			lastTimeStamp = currentTimeTimestamp;

			String ticker = data[0];
			String per = data[1];
			Double open = Double.parseDouble(data[3]);
			Double high = Double.parseDouble(data[4]);
			Double low = Double.parseDouble(data[5]);
			Double close = Double.parseDouble(data[6]);
			Long vol = Long.parseLong(data[7]);

			Attribute attrTicker = new Attribute("ticker", ticker);
			Attribute attrPer = new Attribute("per", per);
			Attribute attrOpen = new Attribute("open", open);
			Attribute attrHigh = new Attribute("high", high);
			Attribute attrLow = new Attribute("low", low);
			Attribute attrClose = new Attribute("close", close);
			Attribute attrVol = new Attribute("vol", vol);

			retEvent = new GenericEvent("StockEv", currentTimeTimestamp, attrTicker, attrPer, attrOpen, attrHigh, attrLow, attrClose, attrVol);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}

	public static void main(String[] args) {
		try {
			SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddkkmm");

			System.out.println(ft.parse("201011010942").getTime());
			System.out.println(ft.parse("201011010943").getTime());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
