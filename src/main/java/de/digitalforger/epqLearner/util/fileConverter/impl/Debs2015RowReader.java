package de.digitalforger.epqLearner.util.fileConverter.impl;

import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * This implementation know how to read the DEBS grand challenge data
 * 
 * From the official meta.txt: Provided data consists of reports of taxi trips
 * including starting point, drop-off point, corresponding timestamps, and
 * information related to the payment. Data are reported at the end of the trip,
 * i.e., upon arrive in the order of the drop-off timestamps
 * 
 * @author george
 *
 */
public class Debs2015RowReader implements ISourceFileRowReader {

	private static Logger logger = Logger.getLogger(Debs2015RowReader.class.getName());
	private long lastTimeStamp = 0l;
	private boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	public void resetLastTimeStamp() {
		lastTimeStamp = 0;
	}

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		logger.warning("Method not yet implemented!! 'Debs2015RowReader.toCSVRow()'");
		return null;
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {
		GenericEvent retEvent = null;
		try {
			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			String[] data = fileRow.split(",");

			//0: medallion			an md5sum of the identifier of the taxi - vehicle bound
			//1: hack_license		an md5sum of the identifier for the taxi license
			//2: pickup_datetime	time when the passenger(s) were picked up
			//3: dropoff_datetime	time when the passenger(s) were dropped off
			//4: trip_time_in_secs	duration of the trip
			//5: trip_distance		trip distance in miles
			//6: pickup_longitude	longitude coordinate of the pickup location
			//7: pickup_latitude	latitude coordinate of the pickup location
			//8: dropoff_longitude	longitude coordinate of the drop-off location
			//9: dropoff_latitude	latitude coordinate of the drop-off location
			//10: payment_type		the payment method - credit card or cash
			//11: fare_amount		fare amount in dollars
			//12: surcharge			surcharge in dollars
			//13: mta_tax			tax in dollars
			//14: tip_amount		tip in dollars
			//15: tolls_amount		bridge and tunnel tolls in dollars
			//16: total_amount		total paid amount in dollars

			
			long currentTimeTimestamp = ft.parse(data[2]).getTime();

			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if (currentTimeTimestamp <= lastTimeStamp) {
					currentTimeTimestamp = lastTimeStamp + 1;
				}
			}

			lastTimeStamp = currentTimeTimestamp;
			
			String medallion = data[0];
			String hack_license = data[1];						
			long pickupTime = ft.parse(data[2]).getTime();
			long dropoffTime = ft.parse(data[3]).getTime();
			int tripDurationSecs = Integer.parseInt(data[4]);
			double tripDistance = Double.parseDouble(data[5]);			
			double pickupLongitude = Double.parseDouble(data[6]);
			double pickupLatitute = Double.parseDouble(data[7]);		
			double dropoffLongitude = Double.parseDouble(data[8]);
			double dropoffLatitute = Double.parseDouble(data[9]);
			String paymentType = data[10];
			double fareAmount = Double.parseDouble(data[11]);			
			double surcharge = Double.parseDouble(data[12]);
			double mtaTax = Double.parseDouble(data[13]);
			double tipAmount = Double.parseDouble(data[14]);
			double tollsAmount = Double.parseDouble(data[15]);
			double totalAmount = Double.parseDouble(data[16]);
			
			if(pickupLongitude == 0.0 || pickupLatitute == 0.0 || dropoffLongitude == 0.0 || dropoffLatitute == 0.0) {
				// an event with incorrect values
				return null;
			}
			
			Attribute attrMedallion = new Attribute("medallion", medallion);
			Attribute attrHack_license = new Attribute("hack_license", hack_license);
			Attribute attrPickupTime = new Attribute("pickupTime", pickupTime);
			Attribute attrDropoffTime = new Attribute("dropoffTime", dropoffTime);
			Attribute attrTripDurationSecs = new Attribute("tripDurationSecs", tripDurationSecs);
			Attribute attrTripDistance = new Attribute("tripDistance", tripDistance);
			Attribute attrPickupLongitude = new Attribute("pickupLongitude", pickupLongitude);
			Attribute attrPickupLatitute = new Attribute("pickupLatitute", pickupLatitute);
			Attribute attrDropoffLongitude = new Attribute("dropoffLongitude", dropoffLongitude);
			Attribute attrDropoffLatitute = new Attribute("dropoffLatitute", dropoffLatitute);
			Attribute attrPaymentType = new Attribute("paymentType", paymentType);
			Attribute attrFareAmount = new Attribute("fareAmount", fareAmount);
			Attribute attrSurcharge = new Attribute("surcharge", surcharge);
			Attribute attrMtaTax = new Attribute("mtaTax", mtaTax);
			Attribute attrTipAmount = new Attribute("tipAmount", tipAmount);
			Attribute attrTollsAmount = new Attribute("tollsAmount", tollsAmount);
			Attribute attrTotalAmount = new Attribute("totalAmount", totalAmount);

			
			
			//retEvent = new GenericEvent("TaxiEv", currentTimeTimestamp, attrMedallion, attrHack_license, attrPickupTime, attrDropoffTime, attrTripDurationSecs, attrTripDistance, attrPickupLongitude, attrPickupLatitute, attrDropoffLongitude, attrDropoffLatitute, attrPaymentType, attrFareAmount, attrSurcharge, attrMtaTax, attrTipAmount, attrTollsAmount, attrTotalAmount);
			retEvent = new GenericEvent("TaxiEv", currentTimeTimestamp, attrMedallion, attrHack_license, attrTripDurationSecs, attrTripDistance, attrPaymentType, attrFareAmount, attrTipAmount, attrTotalAmount, attrPickupLongitude, attrPickupLatitute, attrDropoffLongitude, attrDropoffLatitute);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}
}
