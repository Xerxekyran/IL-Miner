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
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.Attribute;
import de.digitalforger.epqLearner.event.EValueType;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.event.Value;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * This learner focuses on constraints that are of the form
 * "Eventattribute Operator Constant"
 * 
 * @author george
 * 
 */
public class ConstantConstraintLearner {
	private static Logger logger = Logger.getLogger(ConstantConstraintLearner.class.getName());

	/**
	 * 
	 * @param matchedEventInstanceOrder
	 * @return
	 */
	public HashMap<OrderElement, LinkedList<ConstantConstraint>> checkForInequalityConstraints(MatchedEventInstanceOrder matchedEventInstanceOrder) {

		HashMap<OrderElement, HashMap<String, Value[]>> mapOfConstraints = new HashMap<OrderElement, HashMap<String, Value[]>>();

		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = matchedEventInstanceOrder.getOrderForTraces();

		for (HistoricalTrace currentTrace : orderForTraces.keySet()) {
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(currentTrace);
			for (MatchedElement me : matchedOrderForOneTrace.getMatchedElements()) {

				OrderElement oe = me.getOrderElement();

				HashMap<String, Value[]> constraintsForOneOrderElement = mapOfConstraints.get(oe);
				if (constraintsForOneOrderElement == null) {
					constraintsForOneOrderElement = new HashMap<String, Value[]>();
					mapOfConstraints.put(oe, constraintsForOneOrderElement);
				}

				for (GenericEvent ev : me.getMatchedEvents()) {
					for (String attrName : ev.getAttributes().keySet()) {

						if (ev.getAttributes().get(attrName).getValue().getType().equals(EValueType.STRING)) {
							// String attributes are not handled by this learner
							continue;
						}

						Value currentValue = ev.getAttributes().get(attrName).getValue();

						Value[] minMaxForAttribute = constraintsForOneOrderElement.get(attrName);
						if (minMaxForAttribute == null) {
							minMaxForAttribute = new Value[2];
							minMaxForAttribute[0] = currentValue;
							minMaxForAttribute[1] = currentValue;							
						} else {
							if (currentValue.compareTo(minMaxForAttribute[0]) < 0) {
								// new lowest values
								minMaxForAttribute[0] = currentValue;
							}

							if (currentValue.compareTo(minMaxForAttribute[1]) > 0) {
								// new highest value
								minMaxForAttribute[1] = currentValue;
							}
						}
						constraintsForOneOrderElement.put(attrName, minMaxForAttribute);
					}
				}
			}
		}

		// now convert the used datastructure in the needed return type
		HashMap<OrderElement, LinkedList<ConstantConstraint>> ret = new HashMap<OrderElement, LinkedList<ConstantConstraint>>();

		for (OrderElement oe : mapOfConstraints.keySet()) {
			LinkedList<ConstantConstraint> newInequalConstraints = new LinkedList<ConstantConstraint>();
			ret.put(oe, newInequalConstraints);

			HashMap<String, Value[]> minMaxValuesForAttribute = mapOfConstraints.get(oe);
			for (String attributeName : minMaxValuesForAttribute.keySet()) {
				Value[] values = minMaxValuesForAttribute.get(attributeName);
				newInequalConstraints.add(new ConstantConstraint(oe, attributeName, values[0], new LinkedList<GenericEvent>(), EConstraintOperator.GreaterThanEqual));
				newInequalConstraints.add(new ConstantConstraint(oe, attributeName, values[1], new LinkedList<GenericEvent>(), EConstraintOperator.LessThanEqual));
			}

		}

		return ret;
	}

	/**
	 * 
	 * @param matchedEventInstanceOrder
	 * @return
	 */
	public HashMap<OrderElement, LinkedList<ConstantConstraint>> checkForAssociatedEqualityConstraints(MatchedEventInstanceOrder matchedEventInstanceOrder,
			EpqlContext ctx) {
		HashMap<OrderElement, LinkedList<ConstantConstraint>> associatedConstraints = new HashMap<OrderElement, LinkedList<ConstantConstraint>>();

		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = matchedEventInstanceOrder.getOrderForTraces();
		boolean isFirstRound = true;

		// each trace has its own matches for the current order
		for (HistoricalTrace currentTrace : orderForTraces.keySet()) {
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(currentTrace);

			LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

			// the first time we check, remain all possible constraints
			if (isFirstRound) {
				isFirstRound = false;
				for (MatchedElement me : matchedElements) {
					LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();

					// start a new list of possible constraints
					LinkedList<ConstantConstraint> currentConstraintList = new LinkedList<ConstantConstraint>();
					associatedConstraints.put(me.getOrderElement(), currentConstraintList);

					for (GenericEvent ev : matchedEvents) {
						Map<String, Attribute> attributes = ev.getAttributes();

						for (String attrName : attributes.keySet()) {

							// ignore nominal attributes
							if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							currentConstraintList.add(new ConstantConstraint(me.getOrderElement(), attrName, attributes.get(attrName).getValue(), ev,
									EConstraintOperator.Equal));
						}

					}
				}
			} else {
				// now remove all constraints that do not hold, because they do
				// not exist in all traces

				// System.out.println("next Trace ["+ traceNumberCounter + " / "
				// + orderForTraces.keySet().size() +"]");
				for (MatchedElement me : matchedElements) {

					LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();
					// System.out.println("next Me ["+ matchedElements.size()
					// +"] with events ["+ matchedEvents.size() +"]");

					// start a new list of possible constraints
					HashMap<String, LinkedList<ConstantConstraint>> currentConstraintsPerAttribute = new HashMap<String, LinkedList<ConstantConstraint>>();

					for (GenericEvent ev : matchedEvents) {
						Map<String, Attribute> attributes = ev.getAttributes();

						for (String attrName : attributes.keySet()) {
							// ignore nominal attributes
							if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							LinkedList<ConstantConstraint> currentConstraintList = currentConstraintsPerAttribute.get(attrName);
							if (currentConstraintList == null) {
								currentConstraintList = new LinkedList<ConstantConstraint>();
								currentConstraintsPerAttribute.put(attrName, currentConstraintList);
							}

							currentConstraintList.add(new ConstantConstraint(me.getOrderElement(), attrName, attributes.get(attrName).getValue(), ev,
									EConstraintOperator.Equal));
						}
					}

					// now we know all possible attribute values for the
					// current MatchedElement
					if (currentConstraintsPerAttribute != null) {
						for (String attrName : currentConstraintsPerAttribute.keySet()) {

							// the constraints that exist so far
							LinkedList<ConstantConstraint> activePossibleConstraints = associatedConstraints.get(me.getOrderElement());

							// the possible constraints from the current
							// observed trace
							LinkedList<ConstantConstraint> currentPossibleConstraintsForAttribute = currentConstraintsPerAttribute.get(attrName);

							// now we have two lists of associated constraints
							// remain the once that have the same value and
							// remember which event was responsible for that
							LinkedList<ConstantConstraint> newPossibilities = new LinkedList<ConstantConstraint>();
							for (int i = 0; i < activePossibleConstraints.size(); i++) {
								ConstantConstraint activeConstraint = activePossibleConstraints.get(i);

								// check for correct the attribute
								if (!activeConstraint.getAttributeName().equals(attrName))
									continue;

								boolean foundAnEqualityMatch = false;
								for (ConstantConstraint currentConstraint : currentPossibleConstraintsForAttribute) {
									// both constraints are for the same
									// attribute, have the same value and are
									// are meant for equality
									if (activeConstraint.getConstantConstraintValue().equals(currentConstraint.getConstantConstraintValue())
											&& activeConstraint.getOperator().equals(EConstraintOperator.Equal)
											&& currentConstraint.getOperator().equals(EConstraintOperator.Equal)) {
										// we can keep this equality constraint
										if (!foundAnEqualityMatch) {
											// if this is the first match we
											// find, simply add it
											foundAnEqualityMatch = true;
											activeConstraint.addBasedUponEvent(currentConstraint.getBasedUponEvents().getFirst());
										} else {
											// we found another match, so we
											// have to create a new
											// AssociatedConstraint for this one
											ConstantConstraint clone = activeConstraint.cloneWithNewBasedUponEventList();
											clone.getBasedUponEvents().removeLast();
											clone.addBasedUponEvent(currentConstraint.getBasedUponEvents().getFirst());

											newPossibilities.add(clone);
										}
									}
								}

								// remove equality constraints that did not find
								// a value that is actually equal
								if (!foundAnEqualityMatch && activeConstraint.getOperator().equals(EConstraintOperator.Equal)) {
									// if there was no match, we can remove it
									// from the list of observed constraints
									activePossibleConstraints.remove(i);
									i--;
								}
							}
							activePossibleConstraints.addAll(newPossibilities);
						}
					}
				}
			}
		}

		return associatedConstraints;
	}

	/**
	 * 
	 * @param matchedEventInstanceOrder
	 * @return
	 */
	public HashMap<OrderElement, LinkedList<ConstantConstraint>> checkForEqualityConstraints(MatchedEventInstanceOrder matchedEventInstanceOrder, EpqlContext ctx) {
		HashMap<OrderElement, LinkedList<ConstantConstraint>> associatedConstraints = new HashMap<OrderElement, LinkedList<ConstantConstraint>>();

		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = matchedEventInstanceOrder.getOrderForTraces();
		boolean isFirstRound = true;

		// each trace has its own matches for the current order
		for (HistoricalTrace currentTrace : orderForTraces.keySet()) {
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(currentTrace);

			LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();

			// the first time we check, remain all possible constraints
			if (isFirstRound) {
				isFirstRound = false;
				for (MatchedElement me : matchedElements) {
					LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();

					// start a new list of possible constraints
					LinkedList<ConstantConstraint> currentConstraintList = new LinkedList<ConstantConstraint>();
					associatedConstraints.put(me.getOrderElement(), currentConstraintList);

					for (GenericEvent ev : matchedEvents) {
						Map<String, Attribute> attributes = ev.getAttributes();

						for (String attrName : attributes.keySet()) {

							// ignore nominal attributes
							if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							if (!constraintIsAlreadyInList(currentConstraintList, me.getOrderElement(), attrName, attributes.get(attrName).getValue())) {
								currentConstraintList.add(new ConstantConstraint(me.getOrderElement(), attrName, attributes.get(attrName).getValue(),
										new LinkedList<GenericEvent>(), EConstraintOperator.Equal));
							}

						}

					}
				}
			} else {
				// now remove all constraints that do not hold, because they do
				// not exist in all traces

				// System.out.println("next Trace ["+ traceNumberCounter + " / "
				// + orderForTraces.keySet().size() +"]");
				for (MatchedElement me : matchedElements) {

					LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();
					// System.out.println("next Me ["+ matchedElements.size()
					// +"] with events ["+ matchedEvents.size() +"]");

					// start a new list of possible constraints
					HashMap<String, LinkedList<ConstantConstraint>> currentConstraintsPerAttribute = new HashMap<String, LinkedList<ConstantConstraint>>();

					for (GenericEvent ev : matchedEvents) {
						Map<String, Attribute> attributes = ev.getAttributes();

						for (String attrName : attributes.keySet()) {
							// ignore nominal attributes
							if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
								continue;
							}

							LinkedList<ConstantConstraint> currentConstraintList = currentConstraintsPerAttribute.get(attrName);
							if (currentConstraintList == null) {
								currentConstraintList = new LinkedList<ConstantConstraint>();
								currentConstraintsPerAttribute.put(attrName, currentConstraintList);
							}

							currentConstraintList.add(new ConstantConstraint(me.getOrderElement(), attrName, attributes.get(attrName).getValue(), ev,
									EConstraintOperator.Equal));
						}
					}

					// now we know all possible attribute values for the
					// current MatchedElement
					if (currentConstraintsPerAttribute != null) {
						for (String attrName : currentConstraintsPerAttribute.keySet()) {

							// the constraints that exist so far
							LinkedList<ConstantConstraint> activePossibleConstraints = associatedConstraints.get(me.getOrderElement());

							// the possible constraints from the current
							// observed trace
							LinkedList<ConstantConstraint> currentPossibleConstraintsForAttribute = currentConstraintsPerAttribute.get(attrName);

							// now we have two lists of associated constraints
							// remain the once that have the same value and
							// remember which event was responsible for that
							LinkedList<ConstantConstraint> newPossibilities = new LinkedList<ConstantConstraint>();
							for (int i = 0; i < activePossibleConstraints.size(); i++) {
								ConstantConstraint activeConstraint = activePossibleConstraints.get(i);

								// check for correct the attribute
								if (!activeConstraint.getAttributeName().equals(attrName))
									continue;

								boolean foundAnEqualityMatch = false;
								for (ConstantConstraint currentConstraint : currentPossibleConstraintsForAttribute) {
									// both constraints are for the same
									// attribute, have the same value and are
									// are meant for equality
									if (activeConstraint.getConstantConstraintValue().equals(currentConstraint.getConstantConstraintValue())
											&& activeConstraint.getOperator().equals(EConstraintOperator.Equal)
											&& currentConstraint.getOperator().equals(EConstraintOperator.Equal)) {
										// we can keep this equality constraint
										if (!foundAnEqualityMatch) {
											// if this is the first match we
											// find, simply add it
											foundAnEqualityMatch = true;											
										} 
									}
								}

								// remove equality constraints that did not find
								// a value that is actually equal
								if (!foundAnEqualityMatch && activeConstraint.getOperator().equals(EConstraintOperator.Equal)) {
									// if there was no match, we can remove it
									// from the list of observed constraints
									activePossibleConstraints.remove(i);
									i--;
								}
							}
							activePossibleConstraints.addAll(newPossibilities);
						}
					}
				}
			}
		}

		return associatedConstraints;
	}

	/**
	 * 
	 * @param currentConstraintList
	 * @param orderElement
	 * @param attrName
	 * @param value
	 * @return
	 */
	private boolean constraintIsAlreadyInList(LinkedList<ConstantConstraint> currentConstraintList, OrderElement orderElement, String attrName, Value value) {
		boolean ret = false;

		for (ConstantConstraint cc : currentConstraintList) {
			if (cc.getBelongsToOrderElement() == orderElement && cc.getAttributeName().equals(attrName) && cc.getConstantConstraintValue().compareTo(value) == 0) {
				ret = true;
				break;
			}
		}

		return ret;
	}

	/**
	 * This version requires all matched elements for one order element to be of
	 * the same event type (should always be?!) It does not create different
	 * constraints if they are based on different events in one set of one event
	 * instance
	 * 
	 * @param matchedEventInstanceOrder
	 *            <OrderElementIndex, Map of Attribute Name and constraint
	 *            value>
	 * @return
	 */
	public HashMap<Long, HashMap<String, Value>> checkForEqualityConstraints_OLD(MatchedEventInstanceOrder matchedEventInstanceOrder) {

		HashMap<Long, HashMap<String, Value>> equalConstraintsForAllOrderInstances = new HashMap<Long, HashMap<String, Value>>();
		HashMap<Long, HashMap<String, LinkedList<Value>>> equalConstraintsForAllOrderInstancesWithMultipleValues = new HashMap<Long, HashMap<String, LinkedList<Value>>>();

		HashMap<HistoricalTrace, MatchedOrderForOneTrace> orderForTraces = matchedEventInstanceOrder.getOrderForTraces();
		boolean isFirstRound = true;

		// each trace has its own matches for the current order
		for (HistoricalTrace currentTrace : orderForTraces.keySet()) {
			MatchedOrderForOneTrace matchedOrderForOneTrace = orderForTraces.get(currentTrace);

			LinkedList<MatchedElement> matchedElements = matchedOrderForOneTrace.getMatchedElements();
			try {
				// the first time we check, remain all possible constraints
				if (isFirstRound) {
					isFirstRound = false;
					for (MatchedElement me : matchedElements) {
						// this map holds all possible attribute values per
						// attribute of the current order element
						HashMap<String, LinkedList<Value>> equalityConstraintsPerAttribute = new HashMap<String, LinkedList<Value>>();
						equalConstraintsForAllOrderInstancesWithMultipleValues.put(me.getOrderElement().getOrderElementIndex(), equalityConstraintsPerAttribute);

						LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();
						for (GenericEvent ev : matchedEvents) {
							Map<String, Attribute> attributes = ev.getAttributes();

							for (String attrName : attributes.keySet()) {

								if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
									continue;
								}

								LinkedList<Value> possibleEqualAttributeValues = equalityConstraintsPerAttribute.get(attrName);
								// if this is the first attribute value for this
								// attribute
								if (possibleEqualAttributeValues == null) {
									possibleEqualAttributeValues = new LinkedList<Value>();
									equalityConstraintsPerAttribute.put(attrName, possibleEqualAttributeValues);
								}

								possibleEqualAttributeValues.add(attributes.get(attrName).getValue());
							}
						}
					}

				} else {
					// every other check now removes attribute values, that does
					// not already exist
					for (MatchedElement me : matchedElements) {
						HashMap<String, LinkedList<Value>> equalityConstraintsPerAttribute = equalConstraintsForAllOrderInstancesWithMultipleValues.get(me
								.getOrderElement().getOrderElementIndex());

						LinkedList<GenericEvent> matchedEvents = me.getMatchedEvents();

						HashMap<String, LinkedList<Value>> currentEqualityConstraintsPerAttribute = new HashMap<String, LinkedList<Value>>();

						for (GenericEvent ev : matchedEvents) {
							Map<String, Attribute> attributes = ev.getAttributes();
							for (String attrName : attributes.keySet()) {
								if (attributes.get(attrName).getValue().getType().equals(EValueType.STRING)) {
									continue;
								}

								LinkedList<Value> currentPossibleEqualAttributeValues = currentEqualityConstraintsPerAttribute.get(attrName);
								// if this is the first attribute value for this
								// attribute
								if (currentPossibleEqualAttributeValues == null) {
									currentPossibleEqualAttributeValues = new LinkedList<Value>();
									currentEqualityConstraintsPerAttribute.put(attrName, currentPossibleEqualAttributeValues);
								}

								currentPossibleEqualAttributeValues.add(attributes.get(attrName).getValue());
							}
						}
						// now we know all possible attribute values for the
						// current MatchedElement
						if (equalityConstraintsPerAttribute != null) {
							for (String attrName : equalityConstraintsPerAttribute.keySet()) {
								LinkedList<Value> possibleValues = equalityConstraintsPerAttribute.get(attrName);
								LinkedList<Value> currentPossibleValues = currentEqualityConstraintsPerAttribute.get(attrName);

								possibleValues.retainAll(currentPossibleValues);
							}
						}

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				logger.warning("An error analyzing for equality constraints occurred.");
			}

		}

		// now write the results in the final data structure having only one
		// equality constraint per attribute
		for (Long l : equalConstraintsForAllOrderInstancesWithMultipleValues.keySet()) {
			HashMap<String, LinkedList<Value>> constraintsPerOrderElement = equalConstraintsForAllOrderInstancesWithMultipleValues.get(l);
			for (String attributeName : constraintsPerOrderElement.keySet()) {
				LinkedList<Value> possibleValues = constraintsPerOrderElement.get(attributeName);
				if (possibleValues.size() > 1) {
					logger.warning("Found more then one equality constraint for one attribute and order element, this should not happen...");
				} else if (possibleValues.size() == 1) {
					HashMap<String, Value> hashMap = equalConstraintsForAllOrderInstances.get(l);
					if (hashMap == null) {
						hashMap = new HashMap<String, Value>();
						equalConstraintsForAllOrderInstances.put(l, hashMap);
					}
					hashMap.put(attributeName, possibleValues.get(0));
				}
			}

		}

		return equalConstraintsForAllOrderInstances;
	}
}
