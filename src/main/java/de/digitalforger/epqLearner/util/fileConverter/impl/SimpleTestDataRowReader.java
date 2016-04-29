package de.digitalforger.epqLearner.util.fileConverter.impl;

import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

public class SimpleTestDataRowReader implements ISourceFileRowReader {
	private static Logger logger = Logger.getLogger(SimpleTestDataRowReader.class.getName());

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		new UnsupportedOperationException("Not yet implemented");
		return null;
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {
		GenericEvent retEvent = null;
		try {

			String[] data = fileRow.split(",");

			long timestamp = Long.parseLong(data[0]);
			String eventTypeName = data[1];

			LinkedList<Attribute> attributes = new LinkedList<Attribute>();

			for (int i = 2; i < data.length; i++) {
				String[] attrData = data[i].split("=");
				String strValue = attrData[1];
				Value val = null;
				try {
					val = new Value(Long.parseLong(strValue));
				} catch (NumberFormatException ex) {
					// this is no long, try to create a double instead
					try {
						val = new Value(Double.parseDouble(strValue));
					} catch (NumberFormatException ex2) {
						// not a double either
						// we create a string value instead
						val = new Value(strValue);
					}
				}

				attributes.add(new Attribute(attrData[0], val));
			}

			Attribute[] attrs = attributes.toArray(new Attribute[attributes.size()]);
			retEvent = new GenericEvent(eventTypeName, timestamp, attrs);
			System.out.println(retEvent);

		} catch (Exception e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}

		return retEvent;
	}

}
