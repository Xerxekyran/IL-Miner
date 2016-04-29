package de.digitalforger.epqLearner.util.fileConverter;

import de.digitalforger.epqLearner.event.GenericEvent;

public interface ISourceFileRowReader {
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings);
	public GenericEvent fileRowToGenericEvent(String fileRow);
}
