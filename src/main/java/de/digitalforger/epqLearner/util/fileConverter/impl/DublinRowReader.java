package de.digitalforger.epqLearner.util.fileConverter.impl;

import java.text.SimpleDateFormat;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * This implementation knows how to read the dublin data
 * 
 * @author george
 * 
 */
public class DublinRowReader implements ISourceFileRowReader {
	private long lastConvertedTimestamp = -1;
	
	public boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		StringBuilder builder = new StringBuilder();

		try {
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");

			String[] data = fileRow.toString().split(",");

			// long currentTimeTimestamp = Long.parseLong(data[0]);
			// int lineID = Integer.parseInt(data[1]);
			// int direction = Integer.parseInt(data[2]);
			// String journeyPatternID = data[3];
			// long startDate = ft.parse(data[4]).getTime();
			// int vehicleJourneyID = Integer.parseInt(data[5]);
			// String busOperator = data[6];
			// int congestion = Integer.parseInt(data[7]);
			// double longitude = Double.parseDouble(data[8]);
			// double latitude = Double.parseDouble(data[9]);
			// int delay = Integer.parseInt(data[10]);
			// int blockID = Integer.parseInt(data[11]);
			// int vehicleID = Integer.parseInt(data[12]);
			// int stopID = Integer.parseInt(data[13]);
			// int atStop = Integer.parseInt(data[14]);

			builder.append("\"DublinEvt\",");

			// this option prevents traces having the same occurrence time
			if (fixTimings) {
				// on the first call just set the value
				long currentTimeStamp = Long.parseLong(data[0]);

				if (lastConvertedTimestamp == -1) {
					lastConvertedTimestamp = currentTimeStamp;
				} else {
					if (currentTimeStamp <= lastConvertedTimestamp) {
						lastConvertedTimestamp += 1;
						data[0] = lastConvertedTimestamp + "";

						// change the "source" row so it can be written to a new
						// "source" file with the new timings
						StringBuilder newSourceRowContent = new StringBuilder(data[0] + ",");
						for (int i = 1; i < data.length; i++) {
							newSourceRowContent.append(data[i] + ",");
						}
						newSourceRowContent.deleteCharAt(newSourceRowContent.length() - 1);
						fileRow.replace(0, fileRow.length(), newSourceRowContent.toString());

					} else {
						lastConvertedTimestamp = currentTimeStamp;
					}
				}
				fileRow.append("\n");
			}

			for (int i = 0; i < data.length; i++) {
				if (i == 4) {
					builder.append("\"" + ft.parse(data[4]).getTime() + "\"");
				} else {
					builder.append("\"" + data[i] + "\"");
				}

				if (i != data.length - 1) {
					builder.append(",");
				} else {
					builder.append("\n");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {
		GenericEvent retEvent = null;
		try {
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");

			String[] data = fileRow.split(",");

			long currentTimeTimestamp = Long.parseLong(data[0]);
			String lineID = data[1];
			int direction = Integer.parseInt(data[2]);
			String journeyPatternID = data[3];
			long startDate = ft.parse(data[4]).getTime();
			String vehicleJourneyID = data[5];
			String busOperator = data[6];
			int congestion = Integer.parseInt(data[7]);
			double longitude = Double.parseDouble(data[8]);
			double latitude = Double.parseDouble(data[9]);
			int delay = Integer.parseInt(data[10]);
			String blockID = data[11];
			String vehicleID = data[12];
			if (data[13].equals("null")) {
				data[13] = "-1";
			}
			String stopID = data[13];
			String atStop = data[14];

			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if(lastConvertedTimestamp == -1) {
					lastConvertedTimestamp = currentTimeTimestamp-1;
				}
				if (currentTimeTimestamp <= lastConvertedTimestamp) {
					currentTimeTimestamp = lastConvertedTimestamp + 1;
				}
			}

			lastConvertedTimestamp = currentTimeTimestamp;
			
			Attribute attrLineID = new Attribute("lineID", lineID);
			Attribute attrDirection = new Attribute("direction", direction);
			Attribute attrJourneyPatternID = new Attribute("journeyPatternID", journeyPatternID);
			Attribute attrStartDate = new Attribute("startDate", startDate);
			Attribute attrVehicleJourneyID = new Attribute("vehicleJourneyID", vehicleJourneyID);
			Attribute attrBusOperator = new Attribute("busOperator", busOperator);
			Attribute attrCongestion = new Attribute("congestion", congestion);
			Attribute attrLongitude = new Attribute("longitude", longitude);
			Attribute attrLatitude = new Attribute("latitude", latitude);
			Attribute attrDelay = new Attribute("delay", delay);
			Attribute attrBlockID = new Attribute("blockID", blockID);
			Attribute attrVehicleID = new Attribute("vehicleID", vehicleID);
			Attribute attrStopID = new Attribute("stopID", stopID);
			Attribute attrAtStop = new Attribute("atStop", atStop);

			retEvent = new GenericEvent("BusEv", currentTimeTimestamp, attrLineID, attrDelay, attrVehicleID, attrStopID, attrBusOperator, attrJourneyPatternID, attrAtStop);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}
}
