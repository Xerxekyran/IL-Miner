package de.digitalforger.epqLearner.util.fileConverter.impl;

import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * This implementation know how to read the DEBS grand challenge data of 2014
 * 
 * The Grand Challenge 2014 data set is based on simulations driven by
 * real-world energy consumption profiles originating from smart plugs deployed
 * in households.
 * 
 * @author george
 *
 */
public class Debs2014RowReader implements ISourceFileRowReader {

	private static Logger logger = Logger.getLogger(Debs2014RowReader.class.getName());
	private long lastTimeStamp = 0l;
	public boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	public void resetLastTimeStamp() {
		lastTimeStamp = 0;
	}

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

			// 0 id
			// 1 timestamp
			// 2 value /kWh
			// 3 property (0 = work in kWh 1 = loadin Watts)
			// 4 plug_id
			// 5 household_id
			// 6 house_id

			long currentTimeTimestamp = Long.parseLong(data[1]);

			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if (currentTimeTimestamp <= lastTimeStamp) {
					currentTimeTimestamp = lastTimeStamp + 1;
				}
			}

			lastTimeStamp = currentTimeTimestamp;

			String id = data[0];
			double value = Double.parseDouble(data[2]);
			int property = Integer.parseInt(data[3]);
			String plugID = data[4];
			String householdID = data[5];
			String houseID = data[6];

			Attribute attrID = new Attribute("id", id);
			Attribute attrValue = new Attribute("value", value);
			Attribute attrProperty = new Attribute("property", property);
			Attribute attrPlugID = new Attribute("plugID", plugID);
			Attribute attrHouseholdID = new Attribute("householdID", householdID);
			Attribute attrHouseID = new Attribute("houseID", houseID);

			retEvent = new GenericEvent("TaxiEv", currentTimeTimestamp, attrID, attrValue, attrProperty, attrPlugID, attrHouseholdID, attrHouseID);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}
}
