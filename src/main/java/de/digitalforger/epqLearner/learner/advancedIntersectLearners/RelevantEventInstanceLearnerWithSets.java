package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.event.EValueType;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

enum SubsetGenerationMode {
	AllSubsets, PartialMatch, PartialMatchReduced, PartialMatchPartiallyReduced, AttributeOverlapJoin
}

/**
 * @author george
 * 
 */
public class RelevantEventInstanceLearnerWithSets implements ILearner {
	private static Logger logger = Logger.getLogger(RelevantEventInstanceLearnerWithSets.class.getName());
	private SubsetGenerationMode subsetGenerationMode = SubsetGenerationMode.AttributeOverlapJoin;
	private boolean keepAllCreatedInstances = false;

	@Override
	public void performOneStep(EpqlContext ctx) {

		logger.log(Level.FINE, "performing one step of the RelevantEventInstanceLearnerWithSets");
		
		keepAllCreatedInstances = ctx.isWITH_KEEP_ALL_CREATED_EVENT_INSTANCES();
		
		TreeSet<EventInstance> relevantEventInstancesByAttributes = getRelevantEventsByAttributes(ctx.getPositiveTraces(), ctx);

		ctx.getRelevantEventInstances().addAll(relevantEventInstancesByAttributes);

		StringBuilder builder = new StringBuilder();
		Iterator<EventInstance> iterator = relevantEventInstancesByAttributes.iterator();

		while (iterator.hasNext()) {
			EventInstance next = iterator.next();
			builder.append(next + " ");
		}

		logger.log(Level.FINE, "found relevant event instances: " + builder.toString());

	}

	/**
	 * 
	 * @param positiveTraces
	 * @return
	 */
	private TreeSet<EventInstance> getRelevantEventsByAttributes(LinkedList<HistoricalTrace> positiveTraces, EpqlContext ctx) {

		TreeSet<EventInstance> relevantEventInstances = new TreeSet<EventInstance>();

		LinkedList<LinkedList<EventInstance>> tracesAsEventInstances = RelevantEventInstanceLearnerWithSets.convertToEventInstances(positiveTraces, ctx);

		// take the elements in the first trace as given
		for (EventInstance instance : tracesAsEventInstances.get(0)) {
			relevantEventInstances.add(instance);
		}

		if (this.subsetGenerationMode.equals(SubsetGenerationMode.AttributeOverlapJoin)) {
			attributeOverlapJoin(relevantEventInstances, tracesAsEventInstances);
		} else {
			// now check all other traces and only remain the instances, that
			// occur in all traces
			for (int i = 1; i < tracesAsEventInstances.size(); i++) {
				LinkedList<EventInstance> currentTrace = tracesAsEventInstances.get(i);

				TreeSet<EventInstance> currentRelevantEventInstances = new TreeSet<EventInstance>();

				for (EventInstance instance : currentTrace) {
					currentRelevantEventInstances.add(instance);
				}
				retainAll(relevantEventInstances, currentRelevantEventInstances, tracesAsEventInstances, i);
			}
		}

		return relevantEventInstances;

	}

	/**
	 * converts traces of GenericEvent into traces of EventInstances
	 * 
	 * @param positiveTraces
	 * @param ctx
	 * @return
	 */
	public static LinkedList<LinkedList<EventInstance>> convertToEventInstances(LinkedList<HistoricalTrace> positiveTraces, EpqlContext ctx) {
		LinkedList<LinkedList<EventInstance>> tracesAsEventInstances = new LinkedList<LinkedList<EventInstance>>();

		for (HistoricalTrace t : positiveTraces) {
			LinkedList<EventInstance> currentTraceAsEventInstances = new LinkedList<EventInstance>();

			for (GenericEvent e : t.getEvents()) {
				HashMap<String, Value> nominalAttributes = new HashMap<String, Value>();

				// use the event type as a nominal attribute as well
				nominalAttributes.put(EventInstance.PREFIX_FOR_EVENTTYPE_NAME + "EventTypeName", new Value(e.getTypeName()));

				Iterator<String> iterator = e.getAttributes().keySet().iterator();
				while (iterator.hasNext()) {
					String attrName = iterator.next();
					Value attrValue = e.getAttributeValue(attrName);
					if (!ctx.isONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES() || attrValue.getType().equals(EValueType.STRING)) {
						nominalAttributes.put(attrName, attrValue);
					}
				}

				currentTraceAsEventInstances.add(new EventInstance(nominalAttributes, e.getTypeName()));
			}

			tracesAsEventInstances.add(currentTraceAsEventInstances);
		}

		return tracesAsEventInstances;
	}

	/**
	 * 
	 * @param occurredRelevantEventInstances
	 * @param eventInstancesFromCurrentTrace
	 */
	private void retainAll(TreeSet<EventInstance> occurredRelevantEventInstances, TreeSet<EventInstance> eventInstancesFromCurrentTrace,
			final LinkedList<LinkedList<EventInstance>> allTracesAsEventInstances, int currentTraceIndex) {

		boolean restart = false;

		ArrayList<EventInstance> relInstances = new ArrayList<EventInstance>();

		Iterator<EventInstance> iterator = occurredRelevantEventInstances.iterator();
		while (iterator.hasNext()) {
			EventInstance next = iterator.next();
			relInstances.add(next);

			if (this.subsetGenerationMode.equals(SubsetGenerationMode.AllSubsets)) {
				LinkedList<EventInstance> childs = createAllChilds(next);
				relInstances.addAll(childs);
			}
		}

		// for all elements check if they occur in the other set
		do {
			LinkedList<EventInstance> toDelete = new LinkedList<EventInstance>();
			restart = false;

			for (EventInstance thisRelevantInstance : relInstances) {
				Iterator<EventInstance> currentIterator = eventInstancesFromCurrentTrace.iterator();
				boolean found = false;
				while (currentIterator.hasNext()) {
					EventInstance otherRelevantInstance = currentIterator.next();

					if (thisRelevantInstance.attributesWithValuesAreContainedIn(otherRelevantInstance)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// if they are not totally equal, we have to look deeper
					// and maybe only remove some nominal attributes. this may
					// create more relevant instance types
					if (this.subsetGenerationMode.equals(SubsetGenerationMode.PartialMatchPartiallyReduced)
							&& partialMatchPartiallyReduced(thisRelevantInstance, relInstances, eventInstancesFromCurrentTrace)) {
						restart = true;
					} else if (this.subsetGenerationMode.equals(SubsetGenerationMode.PartialMatchReduced)
							&& partialMatchReduced(thisRelevantInstance, relInstances, eventInstancesFromCurrentTrace)) {
						restart = true;
					} else if (this.subsetGenerationMode.equals(SubsetGenerationMode.PartialMatch) && partialMatch(thisRelevantInstance, relInstances)) {
						restart = true;
					}

					toDelete.add(thisRelevantInstance);
				}
				if (restart)
					break;
			}
			relInstances.removeAll(toDelete);
		} while (restart);

		// remove equal elements
		for (int i = 0; i < relInstances.size(); i++) {
			for (int k = i + 1; k < relInstances.size(); k++) {

				if (relInstances.get(i).equals(relInstances.get(k))) {
					relInstances.remove(i);
					i--;
					break;
				}
			}
		}

		// write back to the treeSet
		occurredRelevantEventInstances.clear();
		for (EventInstance addlElement : relInstances) {
			occurredRelevantEventInstances.add(addlElement);
		}
	}

	/**
	 * 
	 * @param next
	 * @return
	 */
	private LinkedList<EventInstance> createAllChilds(EventInstance parent) {
		TreeSet<EventInstance> allChilds = new TreeSet<EventInstance>();

		LinkedList<EventInstance> newParents = createChildsWithOneLessAttribute(parent);
		TreeSet<EventInstance> childsInThisRound = new TreeSet<EventInstance>();
		allChilds.addAll(newParents);

		boolean foundNewChilds = false;
		do {
			foundNewChilds = false;
			childsInThisRound = new TreeSet<EventInstance>();

			for (EventInstance currentParent : newParents) {
				LinkedList<EventInstance> currentChilds = createChildsWithOneLessAttribute(currentParent);
				if (currentChilds != null && currentChilds.size() > 0) {
					allChilds.addAll(currentChilds);
					childsInThisRound.addAll(currentChilds);
					foundNewChilds = true;
				}
			}
			newParents = new LinkedList<EventInstance>(childsInThisRound);
		} while (foundNewChilds);

		LinkedList<EventInstance> ret = new LinkedList<EventInstance>();
		Iterator<EventInstance> iterator = allChilds.iterator();
		while (iterator.hasNext()) {
			ret.add(iterator.next());
		}
		return ret;
	}

	/**
	 * 
	 * @param relevantEventInstances
	 * @param tracesAsEventInstances
	 */
	private void attributeOverlapJoin(TreeSet<EventInstance> relevantEventInstances, LinkedList<LinkedList<EventInstance>> tracesAsEventInstances) {
		ArrayList<EventInstance> relInstances = new ArrayList<EventInstance>();

		// Iterator<EventInstance> iterator = relevantEventInstances.iterator();
		// while (iterator.hasNext()) {
		// EventInstance next = iterator.next();
		// relInstances.add(next);
		// }

		for (int i = 1; i < tracesAsEventInstances.size(); i++) {
			LinkedList<EventInstance> trace = tracesAsEventInstances.get(i);
			for (EventInstance evInstFromPositiveTrace : trace) {
				for (EventInstance thisRelevantInstance : relevantEventInstances) {
					EventInstance newEvInstance = join(thisRelevantInstance, evInstFromPositiveTrace);
					// a join can also be null (no child possible, because no
					// overlap can be found)
					if (newEvInstance != null) {

						// only add new instances
						boolean alreadyExists = false;
						for (EventInstance relevantInstance : relInstances) {
							if (newEvInstance.equals(relevantInstance)) {
								alreadyExists = true;
								break;
							}
						}

						if (!alreadyExists) {
							relInstances.add(newEvInstance);
						}
					}
				}
			}
			
			relevantEventInstances.clear();
			relevantEventInstances.addAll(relInstances);
			
			if(!keepAllCreatedInstances) {
				relInstances.clear();	
			}			
		}

//		// remove equal elements
//		for (int i = 0; i < relInstances.size(); i++) {
//			for (int k = i + 1; k < relInstances.size(); k++) {
//
//				if (relInstances.get(i).equals(relInstances.get(k))) {
//					relInstances.remove(i);
//					i--;
//					break;
//				}
//			}
//		}
//
//		// write back to the treeSet
//		relevantEventInstances.clear();
//		for (EventInstance addlElement : relInstances) {
//			relevantEventInstances.add(addlElement);
//		}
	}

	/**
	 * creates a child element, that only contains attributes, that appear on
	 * both instances with the same value
	 * 
	 * @param thisRelevantInstance
	 * @param evInstanceFromCurrentTrace
	 * @return
	 */
	private EventInstance join(EventInstance thisRelevantInstance, EventInstance evInstanceFromCurrentTrace) {
		if (!thisRelevantInstance.getEventTypeName().equals(evInstanceFromCurrentTrace.getEventTypeName())) {
			return null;
		}

		EventInstance ret = new EventInstance(thisRelevantInstance.getEventTypeName());

		HashMap<String, Value> nominalAttributesThis = thisRelevantInstance.getRelevantEventProperties();
		HashMap<String, Value> nominalAttributesThat = evInstanceFromCurrentTrace.getRelevantEventProperties();
		HashMap<String, Value> newNominalAttributes = ret.getRelevantEventProperties();

		for (String attrName : nominalAttributesThis.keySet()) {
			Value v1 = nominalAttributesThis.get(attrName);
			Value v2 = nominalAttributesThat.get(attrName);

			if (v1.equals(v2)) {
				newNominalAttributes.put(attrName, new Value(v1));
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param thisRelevantInstance
	 * @param relInstances
	 * @param eventInstancesFromCurrentTrace
	 * @return
	 */
	private boolean partialMatchPartiallyReduced(EventInstance thisRelevantInstance, ArrayList<EventInstance> relInstances,
			TreeSet<EventInstance> eventInstancesFromCurrentTrace) {
		HashMap<String, Value> nominalAttributes = thisRelevantInstance.getRelevantEventProperties();
		if (nominalAttributes.size() <= 1) {
			return false;
		}

		LinkedList<EventInstance> candidates = createChildsWithOneLessAttribute(thisRelevantInstance);

		// if we can not add anything, return false
		if (candidates.size() == 0) {
			return false;
		}

		// only keep candidates that are in all traces
		boolean foundSomething = false;
		for (EventInstance reducedInstance : candidates) {
			Iterator<EventInstance> currentIterator = eventInstancesFromCurrentTrace.iterator();
			while (currentIterator.hasNext()) {
				EventInstance otherRelevantInstance = currentIterator.next();
				if (reducedInstance.attributesWithValuesAreContainedIn(otherRelevantInstance)) {
					relInstances.add(reducedInstance);
					foundSomething = true;
				}
			}
		}

		// if no candidate was a match, add all candidates to the list, so new
		// "childs" can get spawned out of them
		if (!foundSomething) {
			relInstances.addAll(candidates);
		}
		return true;
	}

	/**
	 * 
	 * @param thisRelevantInstance
	 * @param relInstances
	 * @param eventInstancesFromCurrentTrace
	 * @return
	 */
	private boolean partialMatchReduced(EventInstance thisRelevantInstance, ArrayList<EventInstance> relInstances, TreeSet<EventInstance> eventInstancesFromCurrentTrace) {

		HashMap<String, Value> nominalAttributes = thisRelevantInstance.getRelevantEventProperties();
		if (nominalAttributes.size() <= 1) {
			return false;
		}

		LinkedList<EventInstance> candidates = createChildsWithOneLessAttribute(thisRelevantInstance);

		for (EventInstance reducedInstance : candidates) {
			Iterator<EventInstance> currentIterator = eventInstancesFromCurrentTrace.iterator();
			while (currentIterator.hasNext()) {
				EventInstance otherRelevantInstance = currentIterator.next();
				if (reducedInstance.attributesWithValuesAreContainedIn(otherRelevantInstance)) {
					relInstances.add(reducedInstance);
					return true;
				}
			}
		}

		// if all tests are not already returning true, we add all candidates so
		// they can be reduced with the next step
		relInstances.addAll(candidates);
		return true;
	}

	/**
	 * 
	 * @return
	 */
	private LinkedList<EventInstance> createChildsWithOneLessAttribute(EventInstance eventInstance) {
		LinkedList<EventInstance> ret = new LinkedList<EventInstance>();

		HashMap<String, Value> nominalAttributes = eventInstance.getRelevantEventProperties();
		if (nominalAttributes.size() <= 1) {
			return ret;
		}

		// so we know that the "thisRelevantInstance" is not contained in all
		// traces, lets add permutations of it with 1 less attribute in it
		for (String attrNameToRemove : nominalAttributes.keySet()) {

			// do not remove the attribute that represents the event type
			if (attrNameToRemove.startsWith(EventInstance.PREFIX_FOR_EVENTTYPE_NAME)) {
				continue;
			}

			HashMap<String, Value> newAttributes = new HashMap<String, Value>();
			for (String attrName : nominalAttributes.keySet()) {
				if (!attrName.equals(attrNameToRemove)) {
					newAttributes.put(attrName, nominalAttributes.get(attrName));
				}
			}
			ret.add(new EventInstance(newAttributes, eventInstance.getEventTypeName()));
		}

		return ret;
	}

	/**
	 * 
	 * @param thisRelevantInstance
	 * @param relInstances
	 * @return
	 */
	private boolean partialMatch(EventInstance thisRelevantInstance, ArrayList<EventInstance> relInstances) {

		HashMap<String, Value> nominalAttributes = thisRelevantInstance.getRelevantEventProperties();
		if (nominalAttributes.size() <= 1) {
			return false;
		}

		// so we know that the "thisRelevantInstance" is not contained in all
		// traces, lets add permutations of it with 1 less attribute in it
		LinkedList<EventInstance> childsWithOneLessAttribute = createChildsWithOneLessAttribute(thisRelevantInstance);
		relInstances.addAll(childsWithOneLessAttribute);

		return true;
	}
}
