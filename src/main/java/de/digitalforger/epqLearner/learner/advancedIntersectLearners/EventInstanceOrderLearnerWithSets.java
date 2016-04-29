package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPattern;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.Sequence;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;
import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.parallelSequenceMining.ParallelSequenceMining;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.sequence.BrutForceSequenceLearner;
import de.digitalforger.epqLearner.util.ObjectCloner;
import de.digitalforger.epqLearner.util.SetHelper;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * This learner uses one of the sequential pattern mining algorithms from the
 * SPMF open source data mining package. A description can be found at: <a href=
 * "http://www.philippe-fournier-viger.com/spmf/index.php?link=documentation.php#examplePrefixSpan"
 * >SPMF-PrefixSPan Documentation</a>
 * 
 * @author george
 * 
 */
public class EventInstanceOrderLearnerWithSets implements ILearner {
	private static Logger logger = Logger.getLogger(EventInstanceOrderLearnerWithSets.class.getName());

	// shall there be more debug outputs?
	private static boolean DEBUG_OUTPUTS_FOR_THIS_FILE = true;
	private static boolean USE_PARALLEL_SEQUENCE_MINING = false;
	private boolean mergeAllEventInstances = false;
	private boolean useAllSubsetOrders = true;

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.INFO, "performing one step of the EventInstanceOrderLearnerWithSets");

		this.mergeAllEventInstances = ctx.isWITH_MERGE_ITEMSETS_IN_FOUND_ORDERS();
		this.useAllSubsetOrders = ctx.isUSE_POWERSET_OF_FOUND_ORDERS();

		try {

			LinkedList<Pattern> availablePatterns = ctx.getPatternMatchingLanguage().getPatterns();

			// the first (and currently only) pattern holds the information we
			// need
			LinkedList<EventInstance> relevantEventInstances = ctx.getRelevantEventInstances();

			HashMap<Integer, EventInstance> idToEventInstanceMapping;
			LinkedList<EventInstanceOrder> bestPatterns = null;

			// special case if we only have one relevant event instance, we can
			// simply use the longest occurrence of it as the order
			if (relevantEventInstances.size() == 1) {
				logger.log(Level.INFO, "Only one relevant event instance -> We simply use its occurrence count");
				bestPatterns = new LinkedList<EventInstanceOrder>();
				EventInstanceOrder eventInstanceOrder = new EventInstanceOrder();
				bestPatterns.add(eventInstanceOrder);

				for (Long index = 0l; index < ctx.getEventInstanceOccurrenceCount().get(relevantEventInstances.getFirst()); index++) {
					eventInstanceOrder.add(new OrderElement(relevantEventInstances.getFirst(), index));
				}
			} else {

				logger.info("Building sequence database...");
				ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase sequenceDbForBIDE = new ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase();
				idToEventInstanceMapping = convertTracesToSequences(sequenceDbForBIDE, ctx.getPositiveTraces(), relevantEventInstances,
						ctx.isONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES());

				if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
					logger.info(sequenceDbForBIDE.getPrint());
				}

				LinkedList<Integer> prunedElements = new LinkedList<Integer>();
				if (ctx.isWITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET()) {
					prunedElements = pruneSequenceDB(sequenceDbForBIDE);
				}

				// another reduction, this time some information gets lost
				// (hopefully
				// the one thats not really information)
				if (ctx.isWITH_LOW_INFORMATION_VALUE_REDUCTION()) {
					logger.log(Level.INFO, "Reducing candidates by measuring its information (entropy)");
					pruneSequenceDBWithLowInformationValue(sequenceDbForBIDE);
				}

				// optimization for the relevant instances: delete instances
				// that can be build from other instances by combining them this
				// way the order learner has less instances to work with and we
				// can rebuild the "bigger" instance as a combination of the
				// "little" instances
				if (ctx.isWITH_SUPERSET_REDUCTION_OPTIMIZATION()) {
					logger.log(Level.INFO, "Reducing candidates by removing candidates that can be build from others for each itemset");
					pruneSequenceDBWithSupersetReduction(sequenceDbForBIDE, idToEventInstanceMapping);
				}

				if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
					logger.info(sequenceDbForBIDE.getPrint());
				}

				switch (ctx.getSequentialMiningAlgorithm()) {
				case BrutForce:
					// execute the algorithm
					logger.log(Level.INFO, "Executing BrutForce algorithm");
					BrutForceSequenceLearner brutForceLearner = new BrutForceSequenceLearner();
					SequentialPatterns patternsBrutForce = brutForceLearner.runAlgorithm(sequenceDbForBIDE);
					logger.log(Level.INFO, "Algorithm returned patterns");

					// some statistic outputs for debugging
					// algo.printStatistics(sequenceDbForBIDE.size());
					if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
						logger.info(patternsBrutForce.getFrequentPatternsAsString(sequenceDbForBIDE.size(), true));
					}
					logger.log(Level.INFO, "Choosing best Pattern from result of the algorithm");
					bestPatterns = chooseBestPatterns(patternsBrutForce, idToEventInstanceMapping, prunedElements, ctx.isONLY_TAKE_LONGEST_FOUND_SEQUENCES());

					for (EventInstanceOrder order : bestPatterns) {
						logger.info("Found order: " + order);
					}

					break;
				case BidePlus:
					/*
					 * Test of BIDE+ algorithm
					 */
					SequentialPatterns patternsBidePlus = null;

					// parallel version of the sequence mining?
					if (USE_PARALLEL_SEQUENCE_MINING) {
						logger.log(Level.INFO, "Using parallel version of the BidePlus algorithm");
						ParallelSequenceMining parallelSequenceMining = new ParallelSequenceMining();
						patternsBidePlus = parallelSequenceMining.doParallelExecution(sequenceDbForBIDE);
					} else {
						// execute the algorithm
						logger.log(Level.INFO, "Executing BidePlus algorithm");
						AlgoBIDEPlus algo = new AlgoBIDEPlus();
						patternsBidePlus = algo.runAlgorithm(sequenceDbForBIDE, null, sequenceDbForBIDE.size());
					}
					logger.log(Level.INFO, "Algorithm returned patterns");

					// some statistic outputs for debugging
					// algo.printStatistics(sequenceDbForBIDE.size());
					if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
						logger.info(patternsBidePlus.getFrequentPatternsAsString(sequenceDbForBIDE.size(), true));
					}
					logger.log(Level.INFO, "Choosing best Pattern from result of the algorithm");
					bestPatterns = chooseBestPatterns(patternsBidePlus, idToEventInstanceMapping, prunedElements, ctx.isONLY_TAKE_LONGEST_FOUND_SEQUENCES());

					for (EventInstanceOrder order : bestPatterns) {
						logger.info("Found order: " + order);
					}

					break;
				default:
					logger.warning("Unknown sequential pattern mining algorithm");
					break;
				}
			}

			// if the algorithm actually got some results
			if (bestPatterns != null) {
				// save this best pattern to the context

				// if we have more than one order, create new patterns for each
				for (int i = 0; i < bestPatterns.size(); i++) {

					// check if we have enough patterns to store the orders
					while (bestPatterns.size() > availablePatterns.size()) {
						// add a copy of the first one
						Pattern deepCopy = (Pattern) ObjectCloner.deepCopy(availablePatterns.get(0));
						availablePatterns.add(deepCopy);
					}

					// set the order to the pattern
					availablePatterns.get(i).setEventOrder(bestPatterns.get(i));
				}
			}
		} catch (Exception ex) {
			logger.severe(ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param relevantEventInstances
	 * @param sequenceDbForBIDE
	 */
	private void pruneSequenceDBWithLowInformationValue(ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase sequenceDbForBIDE) {
		HashMap<Integer, Integer> countIDs = new HashMap<Integer, Integer>();
		Integer numItemSets = 0;

		// count all occurrences of each itemset and the num of all
		// possible itemsets
		for (Sequence s : sequenceDbForBIDE.getSequences()) {
			for (List<Integer> itemSets : s.getItemsets()) {
				numItemSets += itemSets.size();
				for (Integer item : itemSets) {

					Integer countVal = countIDs.get(item);
					if (countVal == null) {
						countVal = 0;
					}

					countIDs.put(item, countVal + 1);
				}
			}
		}

		double wholeInformation = 0;
		HashMap<Integer, Double> informationContentForID = new HashMap<Integer, Double>();
		for (Integer id : countIDs.keySet()) {

			double probabilityForID = countIDs.get(id).doubleValue() / numItemSets.doubleValue();
			double informationContent = Math.log(1 / probabilityForID);

			if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
				// System.out.println(id + ": " + probabilityForID + "\t" +
				// informationContent);
				logger.info(id + ": " + probabilityForID + "\t" + informationContent);
			}

			wholeInformation += probabilityForID * Math.log(1 / probabilityForID);

			informationContentForID.put(id, informationContent);
		}

		if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
			// System.out.println("Sum: " + wholeInformation);
			logger.info("Sum: " + wholeInformation);
		}

		// now identify the candidates that have a lower information value then
		// the wholeInformation (something like an average value)
		LinkedList<Integer> candidatesToRemoveFromSequenceDB = new LinkedList<Integer>();
		for (Integer id : informationContentForID.keySet()) {
			if (informationContentForID.get(id) < wholeInformation) {
				// candidate for removing this
				candidatesToRemoveFromSequenceDB.add(id);
			}
		}

		// now delete the candidates from all itemsets they are contained in
		if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
			// System.out.println("Removing the following candidates: " +
			// candidatesToRemoveFromSequenceDB);
			logger.info("Removing the following candidates: " + candidatesToRemoveFromSequenceDB);
		}

		for (Sequence s : sequenceDbForBIDE.getSequences()) {
			for (int i = 0; i < s.getItemsets().size(); i++) {

				// actually remove the candidates with low information value
				List<Integer> itemSet = s.getItemsets().get(i);
				itemSet.removeAll(candidatesToRemoveFromSequenceDB);

				// do not keep empty itemsets
				if (itemSet.size() < 1) {
					s.getItemsets().remove(i);
					i--;
				}
			}
		}
	}

	/**
	 * This method returns ids from the sequence id that are contained in all
	 * sequences (these ids seem not to contain information that are good to
	 * seperate items and we can simply add them back after the sequences are
	 * found)
	 * 
	 * @param sequenceDbForBIDE
	 * @return
	 */
	private LinkedList<Integer> pruneSequenceDB(ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase sequenceDbForBIDE) {
		LinkedList<Integer> ret = new LinkedList<Integer>();

		boolean abort = false;
		for (Integer currentID : sequenceDbForBIDE.getSequenceIDs()) {
			for (Sequence sequence : sequenceDbForBIDE.getSequences()) {
				for (List<Integer> itemset : sequence.getItemsets()) {
					if (!itemset.contains(currentID)) {
						abort = true;
						break;
					}
				}
				if (abort) {
					break;
				}
			}

			// if we did not abort until here, this id is contained in all
			// itemsets of all sequences -> remove it
			if (!abort) {
				ret.add(currentID);
			}
			abort = false;
		}

		// something to delete?
		if (!ret.isEmpty()) {
			for (Sequence sequence : sequenceDbForBIDE.getSequences()) {
				for (List<Integer> itemset : sequence.getItemsets()) {
					itemset.removeAll(ret);
				}
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param sequenceDbForBIDE
	 * @param positiveTraces
	 * @param relevantEventInstances
	 * @return
	 */
	private HashMap<Integer, EventInstance> convertTracesToSequences(ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase sequenceDbForBIDE,
			LinkedList<HistoricalTrace> positiveTraces, LinkedList<EventInstance> relevantEventInstances, boolean useOnlyStringAttributes) {
		int sequenceID = 0;
		ArrayList<Integer> traceAsIntegers = new ArrayList<Integer>();

		// init the mapping from event instances to numbers
		HashMap<EventInstance, Integer> mapEventInstanceToAnID = new HashMap<EventInstance, Integer>();
		HashMap<Integer, EventInstance> mapIDToEventInstance = new HashMap<Integer, EventInstance>();

		int idCounter = 1;
		for (EventInstance evInst : relevantEventInstances) {
			mapIDToEventInstance.put(idCounter, evInst);
			mapEventInstanceToAnID.put(evInst, idCounter++);
		}

		for (HistoricalTrace t : positiveTraces) {

			// we work on EventInstances, so convert each GenericEvent in the
			// trace accordingly
			// LinkedList<EventInstance> traceAsEventInstances =
			// convertGenericEventsToEventInstances(t.getEvents(),
			// existingMappingInstances, useOnlyStringAttributes);

			ca.pfv.spmf.input.sequence_database_list_integers.Sequence s = new ca.pfv.spmf.input.sequence_database_list_integers.Sequence(sequenceID++);

			// for (EventInstance e : traceAsEventInstances) {
			for (GenericEvent e : t.getEvents()) {
				// get the eventInstance, that "handles" this kind of event
				LinkedList<EventInstance> eventInstances = findEventInstancesFromPatternForTraceEventInstance(e, relevantEventInstances);

				if (eventInstances == null || eventInstances.size() < 1) {
					logger.warning("HM how could that happen? rethink approach here: EventInstanceOrderLearner::convertTracesToSequences()");
				}

				for (int i = 0; i < eventInstances.size(); i++) {
					traceAsIntegers.add(mapEventInstanceToAnID.get(eventInstances.get(i)));
				}

				// add the traces to the sequence db
				s.addItemset(traceAsIntegers);
				traceAsIntegers = new ArrayList<Integer>();
			}

			sequenceDbForBIDE.addSequence(s);

			// addSequence(traceAsIntegers.toArray(integers), sequenceID++);
			// traceAsIntegers = new ArrayList<Integer>();
		}

		if (DEBUG_OUTPUTS_FOR_THIS_FILE) {
			logger.info("*** EXPLANATION ***");
			for (Integer id : mapIDToEventInstance.keySet()) {
				logger.info(id + " -> " + mapIDToEventInstance.get(id));
			}
			logger.info("*** EXPLANATION ***\n");
		}

		return mapIDToEventInstance;
	}

	/**
	 * 
	 * @param patterns
	 * @param idToEventInstanceMapping
	 * @return
	 */
	private LinkedList<EventInstanceOrder> chooseBestPatterns(SequentialPatterns patterns, HashMap<Integer, EventInstance> idToEventInstanceMapping,
			LinkedList<Integer> prunedElements, boolean onlyTakeLongestSequences) {

		LinkedList<EventInstanceOrder> ret = new LinkedList<EventInstanceOrder>();

		List<SequentialPattern> consideredPatterns = null;

		if (onlyTakeLongestSequences) {
			consideredPatterns = patterns.getLevel(patterns.getLevelCount() - 1);
		} else {
			consideredPatterns = new LinkedList<SequentialPattern>();

			for (int curLevel = 0; curLevel < patterns.levels.size(); curLevel++) {
				List<SequentialPattern> patternsInLevel = patterns.getLevel(curLevel);
				consideredPatterns.addAll(patternsInLevel);
			}
		}

		if (consideredPatterns == null) {
			logger.log(Level.WARNING, "Could not get a best pattern because no pattern is present at all.");
			return ret;
		}

		// closed sequential mining does not output all kinds of sequences,
		// but we can recreate them
		if (useAllSubsetOrders) {
			createAllSubsetOrders(consideredPatterns);
		}

		for (int i = 0; i < consideredPatterns.size(); i++) {

			LinkedList<EventInstanceOrder> bestPattern = convertSequentialPatternToListOfEventInstances(consideredPatterns.get(i), idToEventInstanceMapping,
					prunedElements);
			ret.addAll(bestPattern);
		}

		// this is needed because we may have created the same order from
		// powersets of different initial orders
		removeDuplicateOrders(ret);

		
		logger.info("Found "+ ret.size() + " different orders.");
		return ret;
	}

	/**
	 * 
	 * @param ret
	 */
	private void removeDuplicateOrders(LinkedList<EventInstanceOrder> ret) {
		for (int j = 0; j < ret.size() - 1; j++) {
			for (int i = j + 1; i < ret.size(); i++) {
				if (ret.get(j).hasEqualOrder(ret.get(i))) {
					ret.remove(i);
				}
			}
		}
	}

	/**
	 * 
	 * @param sequentialPattern
	 * @param idToEventInstanceMapping
	 * @param prunedElements
	 * @return
	 */
	private EventInstanceOrder convertSequentialPatternToOneListOfEventInstances(SequentialPattern sequentialPattern,
			HashMap<Integer, EventInstance> idToEventInstanceMapping, LinkedList<Integer> prunedElements) {
		EventInstanceOrder ret = new EventInstanceOrder();

		Iterator<Itemset> iterator = sequentialPattern.getItemsets().iterator();
		Long orderCounter = 0L;
		while (iterator.hasNext()) {
			Itemset itemset = iterator.next();
			List<Integer> items = itemset.getItems();

			// add the pruned items back here, so we dont actually loose the
			// information in them, although they are contained in all items
			items.addAll(prunedElements);

			LinkedList<EventInstance> instancesInItemset = new LinkedList<EventInstance>();
			for (Integer id : items) {
				instancesInItemset.add(idToEventInstanceMapping.get(id));
			}

			// try to remove items that are subsets of other items in one
			// itemset
			removeSubsetEventInstances(instancesInItemset, items);

			// there is only one event istance per itemset when merging is
			// allowed
			ret.add(new OrderElement(mergeEventInstances(instancesInItemset), orderCounter++));
		}

		return ret;
	}

	/**
	 * This method may return multiple orders from one sequence, as it is
	 * possible to derive multiple types of order from a set of instances for
	 * one itemset
	 * 
	 * @param sequentialPattern
	 * @param idToEventInstanceMapping
	 * @param prunedElements
	 * @return
	 */
	private LinkedList<EventInstanceOrder> convertSequentialPatternToListOfEventInstances(SequentialPattern sequentialPattern,
			HashMap<Integer, EventInstance> idToEventInstanceMapping, LinkedList<Integer> prunedElements) {
		LinkedList<EventInstanceOrder> ret = new LinkedList<EventInstanceOrder>();

		if (mergeAllEventInstances) {
			// merge all itemsets to one
			EventInstanceOrder oneOrder = convertSequentialPatternToOneListOfEventInstances(sequentialPattern, idToEventInstanceMapping, prunedElements);
			ret.add(oneOrder);
		} else {
			// use each item in an itemset and create all possible combinations

			Iterator<Itemset> iterator = sequentialPattern.getItemsets().iterator();
			Long orderCounter = 0L;
			while (iterator.hasNext()) {
				Itemset itemset = iterator.next();
				List<Integer> items = itemset.getItems();

				// add the pruned items back here, so we dont actually loose the
				// information in them, although they are contained in all items
				items.addAll(prunedElements);

				LinkedList<EventInstance> instancesInItemset = new LinkedList<EventInstance>();
				for (Integer id : items) {
					instancesInItemset.add(idToEventInstanceMapping.get(id));
				}

				// first element?
				if (orderCounter == 0) {
					// we create a new order for each event instance
					for (EventInstance currentInstance : instancesInItemset) {
						EventInstanceOrder newOrder = new EventInstanceOrder();
						newOrder.add(new OrderElement(currentInstance, orderCounter));
						ret.add(newOrder);
					}
				} else {
					// every order element after the first one adds itself to
					// all existing orders
					// with mutliple elements new orders get created

					LinkedList<EventInstanceOrder> retList = new LinkedList<EventInstanceOrder>();

					for (EventInstance currentInstance : instancesInItemset) {
						LinkedList<EventInstanceOrder> listOfCurrentOrders = new LinkedList<EventInstanceOrder>();
						for (EventInstanceOrder o : ret) {
							EventInstanceOrder newO = new EventInstanceOrder();
							for (int i = 0; i < o.size(); i++) {
								newO.add(o.get(i));
							}

							listOfCurrentOrders.add(newO);
						}

						retList.addAll(listOfCurrentOrders);

						for (EventInstanceOrder order : listOfCurrentOrders) {
							order.add(new OrderElement(currentInstance, orderCounter));
						}

					}
					ret = retList;

				}

				orderCounter++;
			}

		}

		return ret;
	}

	/**
	 * Merge the nominal attribute values of the instances
	 * 
	 * @param instancesInItemset
	 * @param evType
	 * @return
	 */
	private EventInstance mergeEventInstances(LinkedList<EventInstance> instancesInItemset) {
		EventInstance ret = new EventInstance(instancesInItemset.getFirst().getEventTypeName());

		for (EventInstance inst : instancesInItemset) {
			ret.getRelevantEventProperties().putAll(inst.getRelevantEventProperties());
		}

		return ret;
	}

	/**
	 * 
	 * @param instancesInItemset
	 * @param items
	 */
	private void removeSubsetEventInstances(LinkedList<EventInstance> instancesInItemset, List<Integer> items) {
		for (int i = 0; i < instancesInItemset.size(); i++) {
			for (int k = i + 1; k < instancesInItemset.size(); k++) {
				if (instancesInItemset.get(i).attributesWithValuesAreContainedIn(instancesInItemset.get(k))) {
					instancesInItemset.remove(i);
					items.remove(i);
					i--;
					break;
				} else if (instancesInItemset.get(k).attributesWithValuesAreContainedIn(instancesInItemset.get(i))) {
					instancesInItemset.remove(k);
					items.remove(k);
					k--;
				}
			}
		}

	}

	/**
	 * 
	 * @param e
	 *            an event in the trace
	 * @param relevantEventInstances
	 *            a list of relevant event instances
	 * @return a list of event instances that can be applied to the given event
	 */
	private LinkedList<EventInstance> findEventInstancesFromPatternForTraceEventInstance(GenericEvent e, LinkedList<EventInstance> relevantEventInstances) {
		LinkedList<EventInstance> ret = new LinkedList<EventInstance>();

		for (EventInstance evInst : relevantEventInstances) {
			if (e.getTypeName().equals(evInst.getEventTypeName()) && evInst.attributesWithValuesAreContainedIn(e)) {
				ret.add(evInst);
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param eventInstancesInItemSet
	 * @return true if something was changed in the list, false otherwise
	 */
	private boolean removeElementsThatCanBeBuildFromOthers(LinkedList<EventInstance> eventInstancesInItemSet) {
		boolean somethingChanged = false;

		// try to find a combination of several other event instances that
		// together build the current one (without adding new attributes /
		// values)
		LinkedList<EventInstance> toDelete = new LinkedList<EventInstance>();

		for (int i = 0; i < eventInstancesInItemSet.size(); i++) {
			EventInstance buildUpInstance = new EventInstance("");
			EventInstance currentInstanceToCheck = eventInstancesInItemSet.get(i);

			for (int k = 0; k < eventInstancesInItemSet.size(); k++) {
				// if we found an instance thats build of others, we can delete
				// the current one
				if (currentInstanceToCheck.equals(buildUpInstance)) {
					toDelete.add(currentInstanceToCheck);
					break;
				}

				if (k == i)
					continue;

				EventInstance possibleInstanceToAdd = eventInstancesInItemSet.get(k);

				// if the one to add is contained in the current, we can use it
				// to build the current
				if (possibleInstanceToAdd.attributesWithValuesAreContainedIn(currentInstanceToCheck)) {
					buildUpInstance.getRelevantEventProperties().putAll(possibleInstanceToAdd.getRelevantEventProperties());
				}
			}

		}

		if (toDelete.size() > 0) {
			eventInstancesInItemSet.removeAll(toDelete);
			somethingChanged = true;
		}

		return somethingChanged;
	}

	/**
	 * 
	 * @param sequenceDbForBIDE
	 * @param idToEventInstanceMapping
	 */
	private void pruneSequenceDBWithSupersetReduction(SequenceDatabase sequenceDbForBIDE, HashMap<Integer, EventInstance> idToEventInstanceMapping) {

		HashMap<EventInstance, Integer> reverseMapping = new HashMap<EventInstance, Integer>();

		// for each sequence
		for (Sequence s : sequenceDbForBIDE.getSequences()) {

			// for each itemset of that sequence
			for (List<Integer> itemset : s.getItemsets()) {

				// step 1 create event instances for each id in each itemset
				LinkedList<EventInstance> eventInstancesInItemSet = new LinkedList<>();

				for (Integer id : itemset) {
					EventInstance eventInstance = idToEventInstanceMapping.get(id);
					reverseMapping.put(eventInstance, id);

					if (eventInstance != null) {
						eventInstancesInItemSet.add(eventInstance);
					}
				}

				// step 2: remove event instances that can be build from others
				// in that itemset
				if (removeElementsThatCanBeBuildFromOthers(eventInstancesInItemSet)) {
					// step 3: apply the pruning to the sequence db
					itemset.clear();
					for (EventInstance inst : eventInstancesInItemSet) {
						itemset.add(reverseMapping.get(inst));
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param consideredPatterns
	 */
	private void createAllSubsetOrders(List<SequentialPattern> consideredPatterns) {
		Set<SequentialPattern> newConsideredPatterns = new HashSet<SequentialPattern>();

		for (SequentialPattern p : consideredPatterns) {
			List<List<Itemset>> powerSet = SetHelper.powerSet(p.getItemsets());			

			for (List<Itemset> newCombination : powerSet) {
				if (newCombination == null || newCombination.isEmpty()) {
					continue;
				}
				SequentialPattern cloneSequence = new SequentialPattern(new ArrayList<Itemset>(newCombination) , p.getSequenceIDs());
				// cloneSequence.getItemsets().addAll(newCombination);

				// dont add things we already have
				boolean alreadyExists = false;
				for (SequentialPattern existingSeq : newConsideredPatterns) {
					boolean difference = false;
					if (existingSeq.size() == cloneSequence.size()) {
						for (int i = 0; i < existingSeq.size(); i++) {
							if (!existingSeq.get(i).getItems().equals(cloneSequence.get(i).getItems())) {
								difference = true;
								break;
							}
						}

						if (!difference) {
							alreadyExists = true;
							break;
						}
					}
				}

				if (!alreadyExists) {
					newConsideredPatterns.add(cloneSequence);
				}

			}
		}

		consideredPatterns.clear();
		consideredPatterns.addAll(newConsideredPatterns);
	}
}
