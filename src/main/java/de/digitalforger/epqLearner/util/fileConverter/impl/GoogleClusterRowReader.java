package de.digitalforger.epqLearner.util.fileConverter.impl;

import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.fileConverter.ISourceFileRowReader;

/**
 * 
 * @author george
 *
 */
public class GoogleClusterRowReader implements ISourceFileRowReader {
	private long lastConvertedTimestamp = -1;
	public boolean DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS = true;

	@Override
	public String toCSVRow(StringBuilder fileRow, boolean fixTimings) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GenericEvent fileRowToGenericEvent(String fileRow) {
		GenericEvent retEvent = null;
		try {
			/**
			 * 1. timestamp			 
			 * 2. missing info
			 * 3. job ID
			 * 4. task index - within the job
			 * 5. machine ID
			 * 6. event type
			 * 7. user name
			 * 8. scheduling class
			 * 9. priority
			 * 10. resource request for CPU cores
			 * 11. resource request for RAM
			 * 12. resource request for local disk space
			 * 13. different-machine constraint
			 * 
			 * 0        1 2         3 4         5 6                                            7 8 9       10      11        12
			 * 600026913,,515042969,1,372630443,1,/fk1fVcVxZ6iM6gHZzqbIyq56m5zrmHfpdcZ/zzkq4c=,2,0,0.01562,0.01553,0.0002155,0
			 */
			String[] data = fileRow.split(",");

			long currentTimeTimestamp = Long.parseLong(data[0]);
			String missingInfo = data[1];
			String jobID = data[2];
			Integer taskIndex = Integer.parseInt(data[3]);
			String machineID = data[4];
			String eventType = data[5];
			String username = data[6];
			String shedulingClass = data[7];
			String priority = data[8];
			Double resourceRequestCPU = Double.parseDouble(data[9]);
			Double resourceRequestRAM = Double.parseDouble(data[10]);
			Double resourceRequestHDD = Double.parseDouble(data[11]);
			String differentMachineConstraint = data[12];

			
			if (DO_NOT_ALLOW_FOR_SAME_TIME_STAMPS) {
				if (lastConvertedTimestamp == -1) {
					lastConvertedTimestamp = currentTimeTimestamp - 1;
				}
				if (currentTimeTimestamp <= lastConvertedTimestamp) {
					currentTimeTimestamp = lastConvertedTimestamp + 1;
				}
			}

			lastConvertedTimestamp = currentTimeTimestamp;

			Attribute attrMissingInfo = new Attribute("missingInfo", missingInfo);		
			Attribute attrJobID = new Attribute("jobID", jobID);			
			Attribute attrTaskIndex = new Attribute("taskIndex", taskIndex);
			Attribute attrMachineID = new Attribute("machineID", machineID);
			Attribute attrEventType = new Attribute("eventType", eventType);
			Attribute attrUsername = new Attribute("username", username);
			Attribute attrShedulingClass = new Attribute("shedulingClass", shedulingClass);
			Attribute attrPriority = new Attribute("priority", priority);
			Attribute attrResourceRequestCPU = new Attribute("resourceRequestCPU", resourceRequestCPU);
			Attribute attrResourceRequestRAM = new Attribute("resourceRequestRAM", resourceRequestRAM);
			Attribute attrResourceRequestHDD = new Attribute("resourceRequestHDD", resourceRequestHDD);
			Attribute attrDifferentMachineConstraint = new Attribute("differentMachineConstraint", differentMachineConstraint);

//			retEvent = new GenericEvent("ClusterTaskEv", currentTimeTimestamp, attrMissingInfo, attrJobID, attrTaskIndex, attrMachineID,
//					attrEventType, attrUsername, attrShedulingClass, attrPriority, attrResourceRequestCPU, attrResourceRequestRAM,
//					attrResourceRequestHDD, attrDifferentMachineConstraint);

			
			retEvent = new GenericEvent("ClusterTaskEv", currentTimeTimestamp, attrJobID, attrTaskIndex, attrMachineID,
					attrEventType, attrUsername, attrShedulingClass, attrPriority);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return retEvent;
	}

}
