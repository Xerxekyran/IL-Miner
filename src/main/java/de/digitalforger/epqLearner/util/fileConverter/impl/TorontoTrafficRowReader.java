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
public class TorontoTrafficRowReader implements ISourceFileRowReader {

	private static Logger logger = Logger.getLogger(TorontoTrafficRowReader.class.getName());
	private long lastTimeStamp = 0l;
	private static boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		logger.warning("Method not yet implemented!! 'TorontoTrafficRowReader.toCSVRow()'");
		return null;
	}

	public void resetLastTimeStamp() {
		lastTimeStamp = 0;
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {

		GenericEvent retEvent = null;
		try {
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			String[] data = fileRow.split(", ");

			/*
			 * 1st Column: Date/ time of the feed (Time Stamp)
			 * 
			 * 2nd Column: Unique continuous ID for each detector base on the
			 * road direction (easy for subscription)
			 * 
			 * 3rd Column: Detector Name in the system
			 * 
			 * 4th Column: Detector physical address
			 * 
			 * 5th Column: Detector's Latitude
			 * 
			 * 6th Column: Detector's Longitude
			 * 
			 * 7th Column: Integer representing the lane at which vehicles are
			 * being detected (Lane Index)
			 * 
			 * 8th Column: % of time loop is occupied per interval
			 * 
			 * 9th Column: Average speed during an interval (km/h)
			 * 
			 * 10th Column: vehicles per interval in (100 vehicle/hour)
			 * 
			 * 11th Column: VdsDevice's unique ID
			 * 
			 * 12th Colunn: Region Name where the detector is located
			 */
			long currentTimeTimestamp = ft.parse(data[0]).getTime();

			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if (currentTimeTimestamp <= lastTimeStamp) {
					currentTimeTimestamp = lastTimeStamp + 1;
				}
			}

			lastTimeStamp = currentTimeTimestamp;

			int detectorID = Integer.parseInt(data[1]);
			String detectorName = data[2];
			String detectorPhysicalAddress = data[3];
			double latitude = Double.parseDouble(data[4]);
			double longitude = Double.parseDouble(data[5]);
			int laneIndex = Integer.parseInt(data[6]);
			String percentTimeLoopPerInterval = data[7];
			int kmh = Integer.parseInt(data[8]);
			int vehiclesPerInterval = Integer.parseInt(data[9]);
			String vehicleDeviceID = data[10];
			String regionName = data[11];

			Attribute attrDetectorID = new Attribute("detectorID", detectorID);
			Attribute attrDetectorName = new Attribute("detectorName", detectorName);
			Attribute attrDetectorPhysicalAddress = new Attribute("detectorPhysicalAddress", detectorPhysicalAddress);
			Attribute attrLatitude = new Attribute("latitude", latitude);
			Attribute attrLongitude = new Attribute("longitude", longitude);
			Attribute attrLaneIndex = new Attribute("laneIndex", laneIndex);
			Attribute attrPercentTimeLoopPerInterval = new Attribute("percentTimeLoopPerInterval", percentTimeLoopPerInterval);
			Attribute attrKmh = new Attribute("kmh", kmh);
			Attribute attrVehiclesPerInterval = new Attribute("vehiclesPerInterval", vehiclesPerInterval);
			Attribute attrVehicleDeviceID = new Attribute("vehicleDeviceID", vehicleDeviceID);
			Attribute attrRegionName = new Attribute("regionName", regionName);

			retEvent = new GenericEvent("TrafficEv", currentTimeTimestamp, attrDetectorID, attrDetectorName, attrLatitude, attrLongitude, attrLaneIndex, attrKmh,
					attrVehicleDeviceID, attrRegionName);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}

}
