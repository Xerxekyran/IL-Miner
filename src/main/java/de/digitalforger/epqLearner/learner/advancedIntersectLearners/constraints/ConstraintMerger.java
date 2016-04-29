package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedElement;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

public class ConstraintMerger {
	private static Logger logger = Logger.getLogger(ConstraintMerger.class.getName());
	private static boolean WITH_DEBUG_OUTPUTS = false;

	/**
	 * 
	 * @param mapOfEqualityConstraints
	 * @param mapOfInequalityConstraints
	 * @param chainConstraints
	 * @param patternsWithConstraints
	 * @param ctx
	 */
	public void mergeConstraints(HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfEqualityConstraints,
			HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfInequalityConstraints, LinkedList<ChainConstraint> chainConstraints,
			LinkedList<Pattern> patternsWithConstraints, EpqlContext ctx) {

		// we start with only one pattern here
		if (patternsWithConstraints == null || patternsWithConstraints.size() != 1) {
			logger.warning("Constraint merger got a false list of patterns. I need a list with one element, but got: "
					+ (patternsWithConstraints == null ? "null" : patternsWithConstraints.size()));
			return;
		}

		Pattern p = patternsWithConstraints.getFirst();

		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = ctx.getMatchedEventOrders().get(p).getOrderForTraces();

		// get the used orderelements that we are currently working with
		Collection<MatchedOrderForOneTrace> values = orderForTraces.values();
		if (values == null || values.isEmpty()) {
			logger.warning("Found no Matched Order to work with.");
			return;
		}

		MatchedOrderForOneTrace next = values.iterator().next();
		if (next == null) {
			logger.warning("Found no Matched Order to work with.");
			return;
		}

		LinkedList<OrderElement> order = new LinkedList<OrderElement>();
		for (MatchedElement me : next.getMatchedElements()) {
			order.add(me.getOrderElement());
		}

//		// if there are no constraints that need to be merged, simply take the
//		// order
		if ((chainConstraints == null || chainConstraints.size() < 1) && (mapOfInequalityConstraints == null || mapOfInequalityConstraints.size() < 1)
				&& (mapOfEqualityConstraints == null || mapOfEqualityConstraints.size() < 1)) {
			logger.info("Merging step is skipped, because there are no constraints to be merged. We simply take the learned order.");			
			addNominalConstraintFromOrderToPattern(p, order);														
			return;
		}

		// build up look up maps of constraints
		HashMap<AbstractConstraint, Long> constraintIDMapping = new HashMap<AbstractConstraint, Long>();
		HashMap<Long, AbstractConstraint> reverseConstraintIDMapping = new HashMap<Long, AbstractConstraint>();
		getConstraintToIDMapping(mapOfEqualityConstraints, mapOfInequalityConstraints, chainConstraints, constraintIDMapping, reverseConstraintIDMapping);

		HashMap<HistoricalTrace, LinkedList<HashSet<Long>>> constraintCombinationsPerTrace = new HashMap<HistoricalTrace, LinkedList<HashSet<Long>>>();

		HashMap<HistoricalTrace, LinkedList<MinMaxValue>> timeMinMaxValues = new HashMap<HistoricalTrace, LinkedList<MinMaxValue>>();

		// for every trace
		// get patterns with maximum constraints in it, each constraint at
		// least in one pattern

		// for every test trace we have a matched Order
		for (HistoricalTrace t : orderForTraces.keySet()) {

			// get the matched order for this trace
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(t);

			// get the possible constraint mappings for this trace

			LinkedList<EventMappingsForOneElementConstraint> eventMappingsForEqualityConstraints = getEventMappingsForConstantConstraints(matchedOrderForOneTrace,
					mapOfEqualityConstraints, ctx);

			LinkedList<EventMappingsForOneElementConstraint> eventMappingsForInequalityConstraints = getEventMappingsForConstantConstraints(matchedOrderForOneTrace,
					mapOfInequalityConstraints, ctx);

			LinkedList<EventMappingsForTwoElementConstraint> eventMappingsForChainConstraints = getEventMappingsForChainConstraints(matchedOrderForOneTrace,
					chainConstraints, ctx);
			
			// list that holds the timestamp of the first and last event for
			// that combination
			LinkedList<MinMaxValue> timeMinMaxValuesOfCurrentCombinations = new LinkedList<MinMaxValue>();

			// get the possible constraint combinations for this trace
			LinkedList<HashSet<Long>> constraintCombinations = mergeConstraintsForTrace(constraintIDMapping, order, eventMappingsForEqualityConstraints,
					eventMappingsForInequalityConstraints, eventMappingsForChainConstraints, timeMinMaxValuesOfCurrentCombinations);

			timeMinMaxValues.put(t, timeMinMaxValuesOfCurrentCombinations);
			constraintCombinationsPerTrace.put(t, constraintCombinations);
		}

		// find the union of all found pattern over all traces (one pattern
		// from each trace at least)
		LinkedList<Long> finalTimings = new LinkedList<Long>();
		LinkedList<HashSet<Long>> constraintCombinationsOverAllTraces = mergeConstraintCombinationsOverAllTraces(constraintCombinationsPerTrace, timeMinMaxValues,
				finalTimings);

		// remove subsets of constraint combinations as they are included in
		// others
		removeSubsetConstraintCombinations(constraintCombinationsOverAllTraces);

		if (WITH_DEBUG_OUTPUTS) {
			logger.fine("** Final result **");
			logger.fine(constraintCombinationsOverAllTraces.toString());
			
			for(HashSet<Long> set: constraintCombinationsOverAllTraces) {
				System.out.println(set);
			}
		}

		// create a pattern for each found constraint combination and write it
		// back to the list
		patternsWithConstraints.clear();
		for (int i = 0; i < constraintCombinationsOverAllTraces.size(); i++) {
			HashSet<Long> constraintCombination = constraintCombinationsOverAllTraces.get(i);
			Pattern copy = p.copyButRemainOrderElementReferences();
			ctx.getMatchedEventOrders().put(copy, ctx.getMatchedEventOrders().get(p));
			if (i < finalTimings.size()) {
				copy.setTimeWindow(finalTimings.get(i));
			}

			for (Long constraintID : constraintCombination) {
				AbstractConstraint abstractConstraint = reverseConstraintIDMapping.get(constraintID);

				if (abstractConstraint instanceof ChainConstraint) {
					ChainConstraint cc = (ChainConstraint) abstractConstraint;
					copy.getRelationConstraints().add(
							new RelationConstraint(cc.getElements().getFirst().getBelongsToElement(), cc.getElements().getLast().getBelongsToElement(), cc.getElements()
									.getFirst().getAttributeName(), cc.getElements().getLast().getAttributeName(), cc.getOperator()));
				} else if (abstractConstraint instanceof ConstantConstraint) {
					ConstantConstraint cc = (ConstantConstraint) abstractConstraint;
					copy.getPropertyConstraints().add(
							new PropertyConstraint(cc.getBelongsToOrderElement(), cc.getAttributeName(), cc.getConstantConstraintValue(), cc.getOperator()));
				}
			}

			addNominalConstraintFromOrderToPattern(copy, order);

			patternsWithConstraints.add(copy);

		}

	}

	/**
	 * removes conbinations that are included in other combinations
	 * 
	 * @param constraintCombinationsOverAllTraces
	 */
	private void removeSubsetConstraintCombinations(LinkedList<HashSet<Long>> constraintCombinationsOverAllTraces) {
		
		for(int i = 0; i < constraintCombinationsOverAllTraces.size()-1; i++) {
			HashSet<Long> set1 = constraintCombinationsOverAllTraces.get(i);
			HashSet<Long> set2 = constraintCombinationsOverAllTraces.get(i+1);
			
			// if 2 is a subset of 1
			if(set1.containsAll(set2) && set1.size() >= set2.size()) {
				constraintCombinationsOverAllTraces.remove(i+1);
				i--;
			} else if(set2.containsAll(set1) && set2.size() >= set1.size()) {
				constraintCombinationsOverAllTraces.remove(i);
				i--;
			}
		}
	}

	private void addNominalConstraintFromOrderToPattern(Pattern p, LinkedList<OrderElement> order) {
		for (OrderElement oe : order) {
			HashMap<String, Value> relevantNominalEventProperties = oe.getEventInstance().getRelevantEventProperties();

			for (String attrName : relevantNominalEventProperties.keySet()) {
				if (attrName.startsWith(EventInstance.PREFIX_FOR_EVENTTYPE_NAME)) {
					continue;
				}
				p.getPropertyConstraints().add(new PropertyConstraint(oe, attrName, relevantNominalEventProperties.get(attrName), EConstraintOperator.Equal));
			}
		}

	}

	/**
	 * 
	 * @param constraintCombinationsPerTrace
	 * @param timeMinMaxValuesOverAllTraces
	 * @return
	 */
	private LinkedList<HashSet<Long>> mergeConstraintCombinationsOverAllTraces(HashMap<HistoricalTrace, LinkedList<HashSet<Long>>> constraintCombinationsPerTrace,
			HashMap<HistoricalTrace, LinkedList<MinMaxValue>> timeMinMaxValuesOverAllTraces, LinkedList<Long> resultingTimings) {
		LinkedList<HashSet<Long>> constraintCombinationsOverAllTraces = null;

		// first attempt is simply combine each version from each trace with the
		// one from the other traces
		for (HistoricalTrace t : constraintCombinationsPerTrace.keySet()) {
			LinkedList<HashSet<Long>> constraintCombinationsInTrace = constraintCombinationsPerTrace.get(t);
			LinkedList<MinMaxValue> minMaxValuesForTrace = timeMinMaxValuesOverAllTraces.get(t);

			// is this the first trace we look at?
			if (constraintCombinationsOverAllTraces == null) {
				// we take the first one as given, nothing to merge here
				constraintCombinationsOverAllTraces = new LinkedList<HashSet<Long>>();
				constraintCombinationsOverAllTraces.addAll(constraintCombinationsInTrace);

				for (MinMaxValue minMaxForTraceAndCombination : minMaxValuesForTrace) {
					resultingTimings.add(minMaxForTraceAndCombination.maxVal - minMaxForTraceAndCombination.minVal);
				}

			} else {
				LinkedList<HashSet<Long>> newConstraintCombinationsOverAllTraces = new LinkedList<HashSet<Long>>();

				for (int resultingIndex = 0; resultingIndex < constraintCombinationsOverAllTraces.size(); resultingIndex++) {
					HashSet<Long> constraintCombinationOverAllTraces = constraintCombinationsOverAllTraces.get(resultingIndex);

					for (int currentIndex = 0; currentIndex < constraintCombinationsInTrace.size(); currentIndex++) {
						HashSet<Long> constraintCombinationInTrace = constraintCombinationsInTrace.get(currentIndex);

						// we work on a copy, because we need to alter this list
						// multiple times with different constraint lists
						HashSet<Long> currentCombinationOverAllTraces = new HashSet<Long>();
						currentCombinationOverAllTraces.addAll(constraintCombinationOverAllTraces);

						currentCombinationOverAllTraces.retainAll(constraintCombinationInTrace);

						// if the combination is not empty we remember this
						// merge result
						if (!currentCombinationOverAllTraces.isEmpty() && !newConstraintCombinationsOverAllTraces.contains(currentCombinationOverAllTraces)) {
							newConstraintCombinationsOverAllTraces.add(currentCombinationOverAllTraces);

							while (resultingTimings.size() <= resultingIndex) {
								resultingTimings.add(Long.MIN_VALUE);
							}

							// change time window size?
							if (minMaxValuesForTrace.get(currentIndex).maxVal - minMaxValuesForTrace.get(currentIndex).minVal > resultingTimings.get(resultingIndex)) {
								resultingTimings.set(resultingIndex, (minMaxValuesForTrace.get(currentIndex).maxVal - minMaxValuesForTrace.get(currentIndex).minVal));
							}
						}
					}
				}

				constraintCombinationsOverAllTraces = newConstraintCombinationsOverAllTraces;
			}
		}

		return constraintCombinationsOverAllTraces;
	}

	/**
	 * 
	 * @param mapOfEqualityConstraints
	 * @param mapOfInequalityConstraints
	 * @param chainConstraints
	 * @return
	 */
	private void getConstraintToIDMapping(HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfEqualityConstraints,
			HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfInequalityConstraints, LinkedList<ChainConstraint> chainConstraints,
			HashMap<AbstractConstraint, Long> constraintIDMapping, HashMap<Long, AbstractConstraint> reverseConstraintIDMapping) {
		Long lookUpIDCounter = 0L;

		if (WITH_DEBUG_OUTPUTS) {
			logger.fine("** Start finding combination of constraints for patterns. Got the following constraints: ");
		}

		for (OrderElement oe : mapOfEqualityConstraints.keySet()) {
			for (ConstantConstraint cc : mapOfEqualityConstraints.get(oe)) {
				reverseConstraintIDMapping.put(lookUpIDCounter, cc);
				constraintIDMapping.put(cc, lookUpIDCounter++);

				if (WITH_DEBUG_OUTPUTS) {
					logger.fine((lookUpIDCounter-1) + " = "+ cc.toString());
				}
			}
		}

		if (mapOfInequalityConstraints != null) {
			for (OrderElement oe : mapOfInequalityConstraints.keySet()) {
				for (ConstantConstraint cc : mapOfInequalityConstraints.get(oe)) {
					reverseConstraintIDMapping.put(lookUpIDCounter, cc);
					constraintIDMapping.put(cc, lookUpIDCounter++);

					if (WITH_DEBUG_OUTPUTS) {
						logger.fine((lookUpIDCounter-1) + " = "+ cc.toString());
					}
				}
			}
		}

		for (ChainConstraint cc : chainConstraints) {
			reverseConstraintIDMapping.put(lookUpIDCounter, cc);
			constraintIDMapping.put(cc, lookUpIDCounter++);

			if (WITH_DEBUG_OUTPUTS) {
				logger.fine((lookUpIDCounter-1) + " = "+ cc.toString());
			}

		}
	}

	/**
	 * 
	 * @param constraintIDMapping
	 * @param order
	 * @param eventMappingsForEqualityConstraints
	 * @param eventMappingsForInequalityConstraints
	 * @param eventMappingsForChainConstraints
	 * @return
	 */
	private LinkedList<HashSet<Long>> mergeConstraintsForTrace(HashMap<AbstractConstraint, Long> constraintIDMapping, LinkedList<OrderElement> order,
			LinkedList<EventMappingsForOneElementConstraint> eventMappingsForEqualityConstraints,
			LinkedList<EventMappingsForOneElementConstraint> eventMappingsForInequalityConstraints,
			LinkedList<EventMappingsForTwoElementConstraint> eventMappingsForChainConstraints, LinkedList<MinMaxValue> minMaxValues) {

		LinkedList<HashSet<Long>> constraintCombinations = new LinkedList<HashSet<Long>>();
		LinkedList<HashMap<OrderElement, LinkedList<GenericEvent>>> eventsForOrderElementsPerCombination = new LinkedList<HashMap<OrderElement, LinkedList<GenericEvent>>>();

		// we start with one constraint combination
		constraintCombinations.add(new HashSet<Long>());

		HashMap<Long, Boolean> alreadyAddedConstraintID = new HashMap<Long, Boolean>();
		eventsForOrderElementsPerCombination.add(new HashMap<OrderElement, LinkedList<GenericEvent>>());

		// for every constraint combination, try to add all constraints
		for (int i = 0; i < constraintCombinations.size(); i++) {

			HashSet<Long> currentConstraintCombination = constraintCombinations.get(i);

			// we build a hash map of the used events for this pattern in the
			// current trace
			HashMap<OrderElement, LinkedList<GenericEvent>> eventsForOrderElement = eventsForOrderElementsPerCombination.get(i);

			// *************
			// we start with the equality constraints
			for (EventMappingsForOneElementConstraint mapping : eventMappingsForEqualityConstraints) {
				// try to add it to the combination
				if (!addConstraintToConstraintCombination(currentConstraintCombination, mapping, constraintIDMapping, eventsForOrderElement)) {
					// first check if this constraint was added to a different
					// combination already
					if (alreadyAddedConstraintID.get(constraintIDMapping.get(mapping.getConstantConstraint())) != null) {
						// in this case we do nothing
						continue;
					}

					// if the constraint can not be added, we need to open up a
					// new combination with this constraint already in it to
					// guarantee that every constraint is contained in at least
					// one combination
					HashSet<Long> newConstraintCombination = new HashSet<Long>();
					HashMap<OrderElement, LinkedList<GenericEvent>> newEventsForOrderElements = new HashMap<OrderElement, LinkedList<GenericEvent>>();
					eventsForOrderElementsPerCombination.add(newEventsForOrderElements);
					addConstraintToConstraintCombination(newConstraintCombination, mapping, constraintIDMapping, newEventsForOrderElements);
					constraintCombinations.add(newConstraintCombination);
				}

				// mark this constraint as added to a combination
				alreadyAddedConstraintID.put(constraintIDMapping.get(mapping.getConstantConstraint()), true);

			}

			// *************
			// do the same for inequality constraints
			for (EventMappingsForOneElementConstraint mapping : eventMappingsForInequalityConstraints) {
				if (!addConstraintToConstraintCombination(currentConstraintCombination, mapping, constraintIDMapping, eventsForOrderElement)) {
					if (alreadyAddedConstraintID.get(constraintIDMapping.get(mapping.getConstantConstraint())) != null) {
						continue;
					}
					HashSet<Long> newConstraintCombinations = new HashSet<Long>();
					HashMap<OrderElement, LinkedList<GenericEvent>> newEventsForOrderElements = new HashMap<OrderElement, LinkedList<GenericEvent>>();
					eventsForOrderElementsPerCombination.add(newEventsForOrderElements);
					addConstraintToConstraintCombination(newConstraintCombinations, mapping, constraintIDMapping, newEventsForOrderElements);
					constraintCombinations.add(newConstraintCombinations);
				}
				alreadyAddedConstraintID.put(constraintIDMapping.get(mapping.getConstantConstraint()), true);
			}

			// *************
			// now the chain constraints
			for (EventMappingsForTwoElementConstraint mapping : eventMappingsForChainConstraints) {

				if (!addConstraintToConstraintCombination(currentConstraintCombination, mapping, constraintIDMapping, eventsForOrderElement)) {

					if (alreadyAddedConstraintID.get(constraintIDMapping.get(mapping.getChainConstraint())) != null) {
						continue;
					}
					HashSet<Long> newConstraintCombinations = new HashSet<Long>();
					HashMap<OrderElement, LinkedList<GenericEvent>> newEventsForOrderElements = new HashMap<OrderElement, LinkedList<GenericEvent>>();
					eventsForOrderElementsPerCombination.add(newEventsForOrderElements);

					addConstraintToConstraintCombination(newConstraintCombinations, mapping, constraintIDMapping, newEventsForOrderElements);

					constraintCombinations.add(newConstraintCombinations);
				}
				
				alreadyAddedConstraintID.put(constraintIDMapping.get(mapping.getChainConstraint()), true);
			}

		}

		// get the min and max value for each constraint combination
		for (int i = 0; i < constraintCombinations.size(); i++) {
			while (minMaxValues.size() <= i) {
				minMaxValues.add(new MinMaxValue());
			}

			HashMap<OrderElement, LinkedList<GenericEvent>> eventsForOrderElement = eventsForOrderElementsPerCombination.get(i);
			LinkedList<GenericEvent> eventsOfFirstOrderElement = eventsForOrderElement.get(order.getFirst());
			if (eventsOfFirstOrderElement != null) {
				for (GenericEvent e : eventsOfFirstOrderElement) {
					if (e.getTimestamp() < minMaxValues.get(i).minVal) {
						minMaxValues.get(i).minVal = e.getTimestamp();
					}
				}
			}

			LinkedList<GenericEvent> eventsOfLastOrderElement = eventsForOrderElement.get(order.getLast());
			if (eventsOfLastOrderElement != null) {
				for (GenericEvent e : eventsOfLastOrderElement) {
					if (e.getTimestamp() > minMaxValues.get(i).maxVal) {
						minMaxValues.get(i).maxVal = e.getTimestamp();
					}
				}
			}

		}

		if (WITH_DEBUG_OUTPUTS) {
			logger.fine("*** COMBINATION ");
			for (HashSet<Long> cSet : constraintCombinations) {
				logger.fine(cSet.toString());
			}
		}
		return constraintCombinations;
	}

	/**
	 * 
	 * @param currentConstraintCombination
	 * @param mapping
	 * @param constraintIDMapping
	 * @param eventsForOrderElement
	 * @return
	 */
	private boolean addConstraintToConstraintCombination(HashSet<Long> currentConstraintCombination, EventMappingsForTwoElementConstraint mapping,
			HashMap<AbstractConstraint, Long> constraintIDMapping, HashMap<OrderElement, LinkedList<GenericEvent>> eventsForOrderElement) {
		boolean ret = false;

		ChainConstraintElement fromChainConstraintElement = mapping.getChainConstraint().getElements().get(0);
		ChainConstraintElement toChainConstraintElement = mapping.getChainConstraint().getElements().get(1);

		LinkedList<GenericEvent> fromPatternBasedUponEvents = eventsForOrderElement.get(fromChainConstraintElement.getBelongsToElement());
		LinkedList<GenericEvent> toPatternBasedUponEvents = eventsForOrderElement.get(toChainConstraintElement.getBelongsToElement());

		LinkedList<GenericEvent> fromConstraintEvents = new LinkedList<GenericEvent>();
		LinkedList<GenericEvent> toConstraintEvents = new LinkedList<GenericEvent>();
		for (EventsForChainConstraint efcc : mapping.getMappings()) {
			fromConstraintEvents.add(efcc.fromEvent);
			toConstraintEvents.add(efcc.toEvent);
		}

		// check if we have events in common if we add the constraint to the
		// combination
		LinkedList<GenericEvent> fromBasedUponEventsFit = basedUponEventsFit(fromPatternBasedUponEvents, fromConstraintEvents);
		LinkedList<GenericEvent> toBasedUponEventsFit = basedUponEventsFit(toPatternBasedUponEvents, toConstraintEvents);


		if (fromBasedUponEventsFit.size() > 0 && toBasedUponEventsFit.size() > 0) {
			// both parts of the chain can be added, lets do so
			currentConstraintCombination.add(constraintIDMapping.get(mapping.getChainConstraint()));
			eventsForOrderElement.put(fromChainConstraintElement.getBelongsToElement(), fromBasedUponEventsFit);
			eventsForOrderElement.put(toChainConstraintElement.getBelongsToElement(), toBasedUponEventsFit);

			ret = true;
		}
		
		return ret;
	}

	/**
	 * 
	 * @param currentConstraintCombination
	 * @param mapping
	 * @param constraintIDMapping
	 * @param eventsForOrderElement
	 * @return
	 */
	private boolean addConstraintToConstraintCombination(HashSet<Long> currentConstraintCombination, EventMappingsForOneElementConstraint mapping,
			HashMap<AbstractConstraint, Long> constraintIDMapping, HashMap<OrderElement, LinkedList<GenericEvent>> eventsForOrderElement) {
		boolean ret = false;

		OrderElement currentOrderElement = mapping.getConstantConstraint().getBelongsToOrderElement();
		LinkedList<GenericEvent> patternBasedUponEvents = eventsForOrderElement.get(currentOrderElement);

		// check if we have events in common if we add the constraint to the
		// combination
		LinkedList<GenericEvent> basedUponEventsFit = basedUponEventsFit(patternBasedUponEvents, mapping.getMappings());

		if (basedUponEventsFit.size() > 0) {
			// at least one event is contained, we can use this
			// constraint and only remain the matching events
			currentConstraintCombination.add(constraintIDMapping.get(mapping.getConstantConstraint()));
			eventsForOrderElement.put(currentOrderElement, basedUponEventsFit);
			ret = true;
		}

		return ret;
	}

	/**
	 * return the list of matching events for the based upon events
	 * 
	 * @param patternBasedUponEvents
	 * @param constraintBasedUponEvents
	 * @return
	 */
	private LinkedList<GenericEvent> basedUponEventsFit(LinkedList<GenericEvent> patternBasedUponEvents, LinkedList<GenericEvent> constraintBasedUponEvents) {
		LinkedList<GenericEvent> ret = new LinkedList<GenericEvent>();

		// if this is the first constraint for that order element, we can
		// simply add the used events here
		if (patternBasedUponEvents == null) {
			ret.addAll(constraintBasedUponEvents);			
		} else {
			// see if we have events in common
			for (GenericEvent ev : constraintBasedUponEvents) {
				if (patternBasedUponEvents.contains(ev)) {
					// at least one event is contained, we can use this
					// constraint and only remain the matching events
					long t = System.currentTimeMillis();			
					ret.addAll(patternBasedUponEvents);					
					ret.retainAll(new HashSet<GenericEvent>(constraintBasedUponEvents));					
					break;
				}
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param mapOfEqualityConstraints
	 * @param ctx
	 * @return
	 */
	private LinkedList<EventMappingsForOneElementConstraint> getEventMappingsForConstantConstraints(MatchedOrderForOneTrace matchedOrderForOneTrace,
			HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfEqualityConstraints, EpqlContext ctx) {
		LinkedList<EventMappingsForOneElementConstraint> allEqualityMappingsForOneTrace = new LinkedList<EventMappingsForOneElementConstraint>();

		if (mapOfEqualityConstraints != null) {

			// find the order element and belonging matched events to check the
			// quality constraints
			for (OrderElement oe : mapOfEqualityConstraints.keySet()) {
				for (ConstantConstraint cc : mapOfEqualityConstraints.get(oe)) {

					EventMappingsForOneElementConstraint currentMapping = new EventMappingsForOneElementConstraint(cc);
					allEqualityMappingsForOneTrace.add(currentMapping);

					MatchedElement me = matchedOrderForOneTrace.getMatchedElements().get(cc.getBelongsToOrderElement().getOrderElementIndex().intValue());

					for (GenericEvent ev : me.getMatchedEvents()) {
						boolean constraintSucceeds = false;

						int compareValue = ev.getAttributeValue(cc.getAttributeName()).compareTo(cc.getConstantConstraintValue());

						switch (cc.getOperator()) {
						case Equal:
							if (compareValue == 0) {
								constraintSucceeds = true;
							}
							break;
						case GreaterThan:
							if (compareValue > 0) {
								constraintSucceeds = true;
							}
							break;
						case GreaterThanEqual:
							if (compareValue >= 0) {
								constraintSucceeds = true;
							}
							break;
						case LessThan:
							if (compareValue < 0) {
								constraintSucceeds = true;
							}
							break;
						case LessThanEqual:
							if (compareValue <= 0) {
								constraintSucceeds = true;
							}
							break;
						case Unequal:
							logger.warning("This constant constraint operator is not yet supported here: " + cc.getOperator());
							break;
						}

						if (constraintSucceeds) {
							currentMapping.addEventMapping(ev);
						}
					}
				}
			}
		}
		return allEqualityMappingsForOneTrace;
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param chainConstraints
	 * @param ctx
	 * @return
	 */
	private LinkedList<EventMappingsForTwoElementConstraint> getEventMappingsForChainConstraints(MatchedOrderForOneTrace matchedOrderForOneTrace,
			LinkedList<ChainConstraint> chainConstraints, EpqlContext ctx) {
		LinkedList<EventMappingsForTwoElementConstraint> allChainMappingsForOneTrace = new LinkedList<EventMappingsForTwoElementConstraint>();

		for (ChainConstraint chain : chainConstraints) {
			if (chain.getElements().size() > 2) {
				logger.warning("The current implementation of the Constraints learner does not support Chain Constraints with more than two elements!!!");
				continue;
			}

			EventMappingsForTwoElementConstraint currentChainMapping = new EventMappingsForTwoElementConstraint(chain);
			allChainMappingsForOneTrace.add(currentChainMapping);

			ChainConstraintElement fromChainElement = chain.getElements().get(0);
			ChainConstraintElement toChainElement = chain.getElements().get(1);

			MatchedElement fromMatchedElement = matchedOrderForOneTrace.getMatchedElements()
					.get(fromChainElement.getBelongsToElement().getOrderElementIndex().intValue());
			MatchedElement toMatchedElement = matchedOrderForOneTrace.getMatchedElements().get(toChainElement.getBelongsToElement().getOrderElementIndex().intValue());

			// find the combination of events that work with the
			// constraint
			LinkedList<GenericEvent> fromMatchedEvents = fromMatchedElement.getMatchedEvents();
			LinkedList<GenericEvent> toMatchedEvents = toMatchedElement.getMatchedEvents();

			for (GenericEvent fromEvent : fromMatchedEvents) {
				for (GenericEvent toEvent : toMatchedEvents) {

					// first check that the events are not the same and
					// that they are in the correct order
					if (fromEvent == toEvent || fromEvent.getTimestamp() > toEvent.getTimestamp()) {
						continue;
					}

					// check the constraint itself
					int compareValue = fromEvent.getAttributeValue(fromChainElement.getAttributeName()).compareTo(
							toEvent.getAttributeValue(toChainElement.getAttributeName()));
					boolean constraintSucceeds = false;

					switch (chain.getOperator()) {
					case Equal:
						if (compareValue == 0) {
							constraintSucceeds = true;
						}
						break;
					case GreaterThan:
						if (compareValue > 0) {
							constraintSucceeds = true;
						}
						break;
					case GreaterThanEqual:
						if (compareValue >= 0) {
							constraintSucceeds = true;
						}
						break;
					case LessThan:
						if (compareValue < 0) {
							constraintSucceeds = true;
						}
						break;
					case LessThanEqual:
						if (compareValue <= 0) {
							constraintSucceeds = true;
						}
						break;
					case Unequal:
						logger.warning("This constraint operator is not yet supported here: " + chain.getOperator());
						break;
					}

					if (constraintSucceeds) {
						currentChainMapping.addEventMapping(new EventsForChainConstraint(fromEvent, toEvent));
					}
				}
			}
		}

		return allChainMappingsForOneTrace;
	}
}
