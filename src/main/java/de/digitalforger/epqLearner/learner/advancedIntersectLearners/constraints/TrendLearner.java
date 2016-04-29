package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedElement;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.EValueType;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * 
 * @author george
 * 
 */
public class TrendLearner {
	private static Logger logger = Logger.getLogger(TrendLearner.class.getName());

	/**
	 * 
	 * @param matchedOrder
	 * @param withChainMergin
	 * @return
	 */
	public LinkedList<ChainConstraint> checkForChainConstraints(MatchedEventInstanceOrder matchedOrder, EpqlContext ctx) {
		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = matchedOrder.getOrderForTraces();

		LinkedList<ChainConstraint> chainsOfSameEventType = null;

		boolean isFirstRound = true;

		// each trace has its own matches for the current order
		for (HistoricalTrace currentTrace : orderForTraces.keySet()) {
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(currentTrace);

			// the first time we check, remain all possible constraints
			if (isFirstRound) {
				isFirstRound = false;

				if (ctx.isONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE()) {
					chainsOfSameEventType = buildUpChainsOfSameEventTypes_IgnoreEvents(matchedOrderForOneTrace);
				} else {
					chainsOfSameEventType = buildUpChains_IgnoreEvents(matchedOrderForOneTrace, ctx.isONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS(),
							ctx.getRelationshipConstraintPartners());
				}
				logger.info("Found Constraints in first trace: "+ chainsOfSameEventType);				
			} else {

				if (ctx.isONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE()) {
					checkChainsOfSameEventTypeAgainstTrace_IgnoreEvents(matchedOrderForOneTrace, chainsOfSameEventType);
				} else {
					checkChainsAgainstTrace_IgnoreEventsImproved(matchedOrderForOneTrace, chainsOfSameEventType);
				}
			}
		}

		if (ctx.isWITH_CHAIN_MERGING()) {
			// we have chain parts of pairs each
			// now we try to put them together
			combineChainsOfSameEventType(chainsOfSameEventType);
		}

		return chainsOfSameEventType;
	}

	/**
	 * 
	 * @param chainsOfSameEventType
	 */
	private void combineChainsOfSameEventType(LinkedList<ChainConstraint> chainsOfSameEventType) {
		// ******
		// step 1
		// add chains together that are from the same to the same order element
		// with the same operator type
		for (int i = 0; i < chainsOfSameEventType.size() - 1; i++) {
			for (int k = i + 1; k < chainsOfSameEventType.size(); k++) {
				ChainConstraint ic1 = chainsOfSameEventType.get(i);
				ChainConstraint ic2 = chainsOfSameEventType.get(k);

				// if they describe the same chain
				if (ic1.getOperator().equals(ic2.getOperator()) && ic1.equalsIgnoringEvents(ic2)) {
					ic1.addAllEventsToChainElements(ic2);
					chainsOfSameEventType.remove(k);
					k--;
				}
			}
		}

		// ******
		// step 2
		// combine chains that have ending / start in common to form
		// chains of greater length
		for (int i = 0; i < chainsOfSameEventType.size() - 1; i++) {
			for (int k = i + 1; k < chainsOfSameEventType.size(); k++) {
				ChainConstraint ic1 = chainsOfSameEventType.get(i);
				ChainConstraint ic2 = chainsOfSameEventType.get(k);

				// they need to be of the same chain type (operator)
				if (ic1.getOperator().equals(ic2.getOperator())) {

					// try to combine them in any order
					if (ic1.tryToCombine(ic2)) {
						// combined ic1 to: ic1 -> ic2
						// we can remove ic2 from the list
						chainsOfSameEventType.remove(k);
						k--;
						continue;
					} else if (ic2.tryToCombine(ic1)) {
						// combined ic2 to: ic2 -> ic1
						// we can remove ic1 from the list
						chainsOfSameEventType.remove(i);
						i--;
						break;
					}
				}
			}
		}

		// ******
		// step 3
		// clean up chains that are contained in other chains
		for (int i = 0; i < chainsOfSameEventType.size() - 1; i++) {
			for (int k = i + 1; k < chainsOfSameEventType.size(); k++) {
				ChainConstraint ic1 = chainsOfSameEventType.get(i);
				ChainConstraint ic2 = chainsOfSameEventType.get(k);

				// if ic2 is contained in ic1
				if (ic1.tryToInclude(ic2)) {
					// ic2 is now part of ic1
					chainsOfSameEventType.remove(k);
					k--;
					continue;
				} else if (ic2.tryToInclude(ic1)) {
					// ic1 is now part of ic2
					chainsOfSameEventType.remove(i);
					i--;
				}
			}
		}
	}

	private void checkChainsAgainstTrace_IgnoreEventsImproved(MatchedOrderForOneTrace matchedOrderForOneTrace, LinkedList<ChainConstraint> existingChains) {

		LinkedList<ChainConstraint> chainsToRemove = new LinkedList<ChainConstraint>();

		// lets check all existing chains if they also apply to this trace
		for (ChainConstraint chainToCheck : existingChains) {
			boolean constraintExistsInTrace = false;

			ChainConstraintElement firstChainElement = chainToCheck.getElements().getFirst();
			ChainConstraintElement lastChainElement = chainToCheck.getElements().getLast();

			// what are the attributes this chain belongs to?
			String fromAttributeName = firstChainElement.getAttributeName();
			String toAttributeName = lastChainElement.getAttributeName();

			MatchedElement fromMatchedElement = matchedOrderForOneTrace.getMatchedElements().get(
					firstChainElement.getBelongsToElement().getOrderElementIndex().intValue());
			MatchedElement toMatchedElement = matchedOrderForOneTrace.getMatchedElements().get(lastChainElement.getBelongsToElement().getOrderElementIndex().intValue());

			// check all event combinations if they fulfill the current
			// chainToCheck
			for (GenericEvent fromEvent : fromMatchedElement.getMatchedEvents()) {
				if (constraintExistsInTrace) {
					break;
				}
				for (GenericEvent toEvent : toMatchedElement.getMatchedEvents()) {

					// a chain with the wrong order or of the same events?
					if (fromEvent == toEvent || fromEvent.getTimestamp() > toEvent.getTimestamp()) {
						continue;
					}

					// for every attribute of this event type
					Attribute fromAttribute = fromEvent.getAttributes().get(fromAttributeName);
					Attribute toAttribute = toEvent.getAttributes().get(toAttributeName);

					int comparesToVal = fromAttribute.getValue().compareTo(toAttribute.getValue());
					if (comparesToVal < 0) {
						if (chainToCheck.getOperator().equals(EConstraintOperator.LessThan)) {
							constraintExistsInTrace = true;
							break;
						}
					} else if (comparesToVal > 0) {
						if (chainToCheck.getOperator().equals(EConstraintOperator.GreaterThan)) {
							constraintExistsInTrace = true;
							break;
						}
					} else {
						if (chainToCheck.getOperator().equals(EConstraintOperator.Equal)) {
							constraintExistsInTrace = true;
							break;
						}
					}
				}
			}

			if (!constraintExistsInTrace) {
				chainsToRemove.add(chainToCheck);
			}
		}

		// remove all chains that were not found in the given trace
		existingChains.removeAll(chainsToRemove);
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param existingChains
	 */
	private void checkChainsAgainstTrace_IgnoreEvents(MatchedOrderForOneTrace matchedOrderForOneTrace, LinkedList<ChainConstraint> existingChains) {
		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		// build a map that holds information if the already found chain is also
		// present in the current trace to check against
		HashMap<ChainConstraint, Boolean> chainExistsInTrace = new HashMap<ChainConstraint, Boolean>();
		for (ChainConstraint chain : existingChains) {
			chainExistsInTrace.put(chain, false);
		}

		// first find a chain at all in the current trace
		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				// we only check events with the same event type here
				// if
				// (!firstElement.getOrderElement().getEventInstance().getEventTypeName().equals(secondElement.getOrderElement().getEventInstance().getEventTypeName()))
				// {
				// // continue;
				// }

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String firstAttrName : firstAttributes.keySet()) {
							for (String secondAttrName : secondAttributes.keySet()) {

								Attribute firstAttribute = firstAttributes.get(firstAttrName);
								Attribute secondAttribute = secondAttributes.get(secondAttrName);

								// String attribute are not handled here
								if (firstAttribute.getValue().getType().equals(EValueType.STRING) || secondAttribute.getValue().getType().equals(EValueType.STRING)) {
									continue;
								}

								// are the attributes inequal / equal to
								// another?
								int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
								EConstraintOperator operator = EConstraintOperator.GreaterThan;
								if (comparesToVal < 0) {
									operator = EConstraintOperator.LessThan;
								} else if (comparesToVal > 0) {
									operator = EConstraintOperator.GreaterThan;
								} else {
									operator = EConstraintOperator.Equal;
								}

								ChainConstraint currentChainConstraint = new ChainConstraint(operator);
								currentChainConstraint.addChainElement(firstElement.getOrderElement(), firstAttrName);
								currentChainConstraint.addChainElement(secondElement.getOrderElement(), secondAttrName);

								// so now we have a chain in the current
								// trace, now check if this can be found in the
								// existing once
								for (ChainConstraint activeChain : chainExistsInTrace.keySet()) {
									// check if the chain holds in the current
									// trace
									if (activeChain.canBeFoundIn(currentChainConstraint, true)) {
										chainExistsInTrace.put(activeChain, true);
									}
								}
							}
						}
					}
				}
			}
		}
		// remove all chains that were not found in the given trace
		for (ChainConstraint chain : chainExistsInTrace.keySet()) {
			if (chainExistsInTrace.get(chain) == false) {
				existingChains.remove(chain);
			}
		}

	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param chainsOfSameEventType
	 */
	private void checkChainsOfSameEventTypeAgainstTrace_IgnoreEvents(MatchedOrderForOneTrace matchedOrderForOneTrace, LinkedList<ChainConstraint> existingChains) {
		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		// build a map that holds information if the already found chain is also
		// present in the current trace to check against
		HashMap<ChainConstraint, Boolean> chainExistsInTrace = new HashMap<ChainConstraint, Boolean>();
		for (ChainConstraint chain : existingChains) {
			chainExistsInTrace.put(chain, false);
		}

		// first find an chain at all in the current trace
		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				// we only check events with the same event type here
				if (!firstElement.getOrderElement().getEventInstance().getEventTypeName().equals(secondElement.getOrderElement().getEventInstance().getEventTypeName())) {
					// continue;
				}

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String attrName : firstAttributes.keySet()) {
							Attribute firstAttribute = firstAttributes.get(attrName);
							Attribute secondAttribute = secondAttributes.get(attrName);

							// String attribute are not handled here
							if (firstAttribute.getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							// are the attributes inequal / equal to another?
							int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
							EConstraintOperator operator = EConstraintOperator.GreaterThan;
							if (comparesToVal < 0) {
								operator = EConstraintOperator.LessThan;
							} else if (comparesToVal > 0) {
								operator = EConstraintOperator.GreaterThan;
							} else {
								operator = EConstraintOperator.Equal;
							}

							ChainConstraint currentChainConstraint = new ChainConstraint(operator);
							currentChainConstraint.addChainElement(firstElement.getOrderElement(), attrName);
							currentChainConstraint.addChainElement(secondElement.getOrderElement(), attrName);

							// so now we have a chain in the current
							// trace, now check if this can be found in the
							// existing once
							for (ChainConstraint activeChain : chainExistsInTrace.keySet()) {
								// check if the chain holds in the current trace
								if (activeChain.canBeFoundIn(currentChainConstraint, true)) {
									chainExistsInTrace.put(activeChain, true);
								}
							}
						}
					}
				}
			}
		}
		// remove all chains that were not found in the given trace
		for (ChainConstraint chain : chainExistsInTrace.keySet()) {
			if (chainExistsInTrace.get(chain) == false) {
				existingChains.remove(chain);
			}
		}

	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param existingChains
	 */
	private void checkChainsOfSameEventTypeAgainstTrace(MatchedOrderForOneTrace matchedOrderForOneTrace, LinkedList<ChainConstraint> existingChains) {
		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		// build a map that holds information if the already found chain is also
		// present in the current trace to check against
		HashMap<ChainConstraint, Boolean> chainExistsInTrace = new HashMap<ChainConstraint, Boolean>();
		for (ChainConstraint chain : existingChains) {
			chainExistsInTrace.put(chain, false);
		}

		// first find an chain at all in the current trace
		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				// we only check events with the same event type here
				if (!firstElement.getOrderElement().getEventInstance().getEventTypeName().equals(secondElement.getOrderElement().getEventInstance().getEventTypeName())) {
					continue;
				}

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String attrName : firstAttributes.keySet()) {
							Attribute firstAttribute = firstAttributes.get(attrName);
							Attribute secondAttribute = secondAttributes.get(attrName);

							// String attribute are not handled here
							if (firstAttribute.getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							// are the attributes inequal / equal to another?
							int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
							EConstraintOperator operator = EConstraintOperator.GreaterThan;
							if (comparesToVal < 0) {
								operator = EConstraintOperator.LessThan;
							} else if (comparesToVal > 0) {
								operator = EConstraintOperator.GreaterThan;
							} else {
								operator = EConstraintOperator.Equal;
							}

							ChainConstraint currentChainConstraint = new ChainConstraint(operator);
							currentChainConstraint.addChainElement(firstElement.getOrderElement(), attrName, firstEvent);
							currentChainConstraint.addChainElement(secondElement.getOrderElement(), attrName, secondEvent);

							// so now we have a chain in the current
							// trace, now check if this can be found in the
							// existing once
							LinkedList<ChainConstraint> newChains = new LinkedList<ChainConstraint>();
							for (ChainConstraint activeChain : chainExistsInTrace.keySet()) {
								// check if the chain holds in the current trace
								if (activeChain.canBeFoundIn(currentChainConstraint, true)) {
									// next check is, if we already did this for
									// this trace
									if (chainExistsInTrace.get(activeChain)) {
										// in this case, we need to create a new
										// one, so we do not end up with
										// multiple events for one order element
										// in just one trace
										ChainConstraint copiedChain = new ChainConstraint(activeChain.getOperator());

										// create new chain elements with new
										// event lists and remove the event for
										// the same order element
										for (ChainConstraintElement ice : activeChain.getElements()) {
											LinkedList<GenericEvent> basedUponEvents = new LinkedList<GenericEvent>();
											basedUponEvents.addAll(ice.getBasedUponEvents());
											basedUponEvents.removeLast();

											ChainConstraintElement nearlyCopiedICE = new ChainConstraintElement(ice.getBelongsToElement(), ice.getAttributeName(),
													basedUponEvents);

											copiedChain.getElements().add(nearlyCopiedICE);
										}

										// we created a new one, so keep it in
										// the list
										newChains.add(copiedChain);

										// add the events to the chain
										copiedChain.addAllEventsToChainElements(currentChainConstraint);
									} else {
										// it seems that we can safely keep this
										// chain
										chainExistsInTrace.put(activeChain, true);

										// add the events to the chain
										activeChain.addAllEventsToChainElements(currentChainConstraint);
									}
								}
							}

							// add copied chains
							for (ChainConstraint ic : newChains) {
								existingChains.add(ic);
								chainExistsInTrace.put(ic, true);
							}
						}
					}
				}
			}
		}

		// remove all chains that were not found in the given trace
		for (ChainConstraint chain : chainExistsInTrace.keySet()) {
			if (chainExistsInTrace.get(chain) == false) {
				existingChains.remove(chain);
			}
		}
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @param onlyUseUserGivenRelationshipPartners
	 * @param allowedConstraintPartners
	 * @return
	 */
	private LinkedList<ChainConstraint> buildUpChains_IgnoreEvents(MatchedOrderForOneTrace matchedOrderForOneTrace, boolean onlyUseUserGivenRelationshipPartners,
			LinkedList<RelationshipConstraintPartner> allowedConstraintPartners) {

		LinkedList<ChainConstraint> chainConstraints = new LinkedList<ChainConstraint>();
		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String firstAttrName : firstAttributes.keySet()) {
							for (String secondAttrName : secondAttributes.keySet()) {
								Attribute firstAttribute = firstAttributes.get(firstAttrName);
								Attribute secondAttribute = secondAttributes.get(secondAttrName);

								// String attributes are not handled here
								// if
								// (firstAttribute.getValue().getType().equals(EValueType.STRING)
								// ||
								// secondAttribute.getValue().getType().equals(EValueType.STRING))
								// {
								// continue;
								// }

								// if the types of the attributes differ, we can
								// not compare them
								if (!firstAttribute.getValue().getType().equals(secondAttribute.getValue().getType())) {
									continue;
								}

								// are only user given relationships enabled?
								if (onlyUseUserGivenRelationshipPartners
										&& !relationshipAllowed(firstAttrName, secondAttrName, firstEvent.getTypeName(), secondEvent.getTypeName(),
												allowedConstraintPartners)) {
									continue;
								}

								// are the attributes inequal / equal to
								// another?
								int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
								EConstraintOperator operator = EConstraintOperator.GreaterThan;
								if (comparesToVal < 0) {
									operator = EConstraintOperator.LessThan;
								} else if (comparesToVal > 0) {
									operator = EConstraintOperator.GreaterThan;
								} else {
									operator = EConstraintOperator.Equal;
								}

								// String attributes are only checked on
								// equality for trends
								if (firstAttribute.getValue().getType().equals(EValueType.STRING) && operator != EConstraintOperator.Equal) {
									continue;
								}

								ChainConstraint chainConstraint = new ChainConstraint(operator);
								chainConstraint.addChainElement(firstElement.getOrderElement(), firstAttrName);
								chainConstraint.addChainElement(secondElement.getOrderElement(), secondAttrName);

								// only add chain constraints, that are not the
								// same
								boolean alreadyExists = false;
								for (ChainConstraint existinConstr : chainConstraints) {
									if (existinConstr.equalsIgnoringEvents(chainConstraint)) {
										alreadyExists = true;
										break;
									}
								}
								if (!alreadyExists) {
									chainConstraints.add(chainConstraint);
								}
							}
						}
					}
				}
			}
		}

		return chainConstraints;
	}

	/**
	 * 
	 * @param firstAttrName
	 * @param secondAttrName
	 * @param typeName
	 * @param typeName2
	 * @param allowedConstraintPartners
	 * @return
	 */
	private boolean relationshipAllowed(String firstAttrName, String secondAttrName, String typeName, String typeName2,
			LinkedList<RelationshipConstraintPartner> allowedConstraintPartners) {
		boolean ret = false;

		for (RelationshipConstraintPartner allowedPartner : allowedConstraintPartners) {
			if ((allowedPartner.getFromEventType().equals(typeName) && allowedPartner.getToEventType().equals(typeName2)
					&& allowedPartner.getFromAttributeName().equals(firstAttrName) && allowedPartner.getToAttributeName().equals(secondAttrName))
					|| (allowedPartner.getFromEventType().equals(typeName2) && allowedPartner.getToEventType().equals(typeName)
							&& allowedPartner.getFromAttributeName().equals(secondAttrName) && allowedPartner.getToAttributeName().equals(firstAttrName))) {
				ret = true;
				break;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @return
	 */
	private LinkedList<ChainConstraint> buildUpChainsOfSameEventTypes_IgnoreEvents(MatchedOrderForOneTrace matchedOrderForOneTrace) {
		LinkedList<ChainConstraint> chainConstraints = new LinkedList<ChainConstraint>();
		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				// we only check events with the same event type here
				if (!firstElement.getOrderElement().getEventInstance().getEventTypeName().equals(secondElement.getOrderElement().getEventInstance().getEventTypeName())) {
					continue;
				}

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String attrName : firstAttributes.keySet()) {
							Attribute firstAttribute = firstAttributes.get(attrName);
							Attribute secondAttribute = secondAttributes.get(attrName);

							// String attributes are not handled here
							// if
							// (firstAttribute.getValue().getType().equals(EValueType.STRING))
							// {
							// continue;
							// }

							// are the attributes inequal / equal to another?
							int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
							EConstraintOperator operator = EConstraintOperator.GreaterThan;
							if (comparesToVal < 0) {
								operator = EConstraintOperator.LessThan;
							} else if (comparesToVal > 0) {
								operator = EConstraintOperator.GreaterThan;
							} else {
								operator = EConstraintOperator.Equal;
							}

							// String attributes are only checked on equality
							// for trends
							if (firstAttribute.getValue().getType().equals(EValueType.STRING) && operator != EConstraintOperator.Equal) {
								continue;
							}

							ChainConstraint chainConstraint = new ChainConstraint(operator);
							chainConstraint.addChainElement(firstElement.getOrderElement(), attrName);
							chainConstraint.addChainElement(secondElement.getOrderElement(), attrName);

							// only add chain constraints, that are not the same

							boolean alreadyExists = false;
							for (ChainConstraint existinConstr : chainConstraints) {
								if (existinConstr.equalsIgnoringEvents(chainConstraint)) {
									alreadyExists = true;
									break;
								}
							}
							if (!alreadyExists) {
								chainConstraints.add(chainConstraint);
							}
						}
					}
				}
			}
		}

		return chainConstraints;
	}

	/**
	 * 
	 * @param matchedOrderForOneTrace
	 * @return
	 */
	private LinkedList<ChainConstraint> buildUpChainsOfSameEventTypes(MatchedOrderForOneTrace matchedOrderForOneTrace) {
		LinkedList<ChainConstraint> chainConstraints = new LinkedList<ChainConstraint>();

		LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

		for (int firstIndex = 0; firstIndex < matchedElements.size() - 1; firstIndex++) {
			MatchedElement firstElement = matchedElements.get(firstIndex);

			for (int secondIndex = firstIndex + 1; secondIndex < matchedElements.size(); secondIndex++) {
				MatchedElement secondElement = matchedElements.get(secondIndex);

				// we only check events with the same event type here
				if (!firstElement.getOrderElement().getEventInstance().getEventTypeName().equals(secondElement.getOrderElement().getEventInstance().getEventTypeName())) {
					continue;
				}

				LinkedList<GenericEvent> firstMatchedEvents = firstElement.getMatchedEvents();
				LinkedList<GenericEvent> secondMatchedEvents = secondElement.getMatchedEvents();

				// now check the concrete events for chains
				for (int firstEventIndex = 0; firstEventIndex < firstMatchedEvents.size(); firstEventIndex++) {
					GenericEvent firstEvent = firstMatchedEvents.get(firstEventIndex);

					for (int secondEventIndex = 0; secondEventIndex < secondMatchedEvents.size(); secondEventIndex++) {
						GenericEvent secondEvent = secondMatchedEvents.get(secondEventIndex);

						// for every attribute of this event type
						Map<String, Attribute> firstAttributes = firstEvent.getAttributes();
						Map<String, Attribute> secondAttributes = secondEvent.getAttributes();

						for (String attrName : firstAttributes.keySet()) {
							Attribute firstAttribute = firstAttributes.get(attrName);
							Attribute secondAttribute = secondAttributes.get(attrName);

							// String attributes are not handled here
							if (firstAttribute.getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							// are the attributes inequal / equal to another?
							int comparesToVal = firstAttribute.getValue().compareTo(secondAttribute.getValue());
							EConstraintOperator operator = EConstraintOperator.GreaterThan;
							if (comparesToVal < 0) {
								operator = EConstraintOperator.LessThan;
							} else if (comparesToVal > 0) {
								operator = EConstraintOperator.GreaterThan;
							} else {
								operator = EConstraintOperator.Equal;
							}

							ChainConstraint chainConstraint = new ChainConstraint(operator);
							chainConstraint.addChainElement(firstElement.getOrderElement(), attrName, firstEvent);
							chainConstraint.addChainElement(secondElement.getOrderElement(), attrName, secondEvent);

							chainConstraints.add(chainConstraint);
						}
					}
				}
			}
		}
		return chainConstraints;
	}
}
