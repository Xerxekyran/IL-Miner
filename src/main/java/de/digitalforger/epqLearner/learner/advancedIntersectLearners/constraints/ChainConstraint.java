package de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints;

import java.util.LinkedList;

import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;

/**
 * Represents a list of event instances that have a chain relationship on
 * certain attributes. For example A1.x < A2.y < B1.z
 * 
 * 
 * @author george
 * 
 */
public class ChainConstraint extends AbstractConstraint {

	private LinkedList<ChainConstraintElement> elements = new LinkedList<ChainConstraintElement>();

	/**
	 * 
	 * @param operator
	 * @param elements
	 */
	public ChainConstraint(EConstraintOperator operator) {
		this.operator = operator;
	}

	/**
	 * 
	 * @param operator
	 * @param elements
	 */
	public ChainConstraint(EConstraintOperator operator, ChainConstraintElement... elements) {
		this.operator = operator;

		for (ChainConstraintElement el : elements) {
			this.elements.add(el);
		}
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean canBeFoundIn(ChainConstraint other, boolean ignoreEvents) {
		boolean ret = false;

		if (!other.operator.equals(this.operator) || elements == null || elements.size() < 1) {
			return false;
		}

		// check if the elements of this chain can all be found (in the correct
		// order) in the other chain. there can also be elements in the other
		// chain that are ignored
		int thisIndex = 0;
		ChainConstraintElement currentElement = elements.get(thisIndex);
		for (ChainConstraintElement otherChainElement : other.getElements()) {

			if ((ignoreEvents && otherChainElement.equalsIgnoringEvents(currentElement))
					|| (!ignoreEvents && currentElement.equalsWithEventsCanBeFoundIn(otherChainElement))) {
				thisIndex++;

				if (thisIndex >= elements.size()) {
					break;
				}

				currentElement = elements.get(thisIndex);
			}
		}

		// did all elements resolve in the other chain?
		if (thisIndex >= this.elements.size()) {
			ret = true;
		}
		return ret;
	}

	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean equalsIgnoringEvents(ChainConstraint other) {
		boolean ret = true;

		if (other.getElements().size() == getElements().size() && other.getOperator().equals(getOperator())) {
			for (int index = 0; index < getElements().size(); index++) {
				ChainConstraintElement otherChainElement = other.getElements().get(index);
				ChainConstraintElement thisChainElement = getElements().get(index);

				// if we found one that is not equal
				if (!otherChainElement.equalsIgnoringEvents(thisChainElement)) {
					ret = false;
					break;
				}
			}
		} else {
			ret = false;
		}

		return ret;
	}

	/**
	 * looks if the other chains has the same starting element as this chain
	 * ends. Works only for chains with the same operator
	 * 
	 * @param other
	 */
	public boolean tryToCombine(ChainConstraint other) {
		boolean ret = false;
		LinkedList<ChainConstraintElement> thisElements = getElements();

		if (!getOperator().equals(other.getOperator())) {
			return false;
		}

		// do we end like the other one starts?
		if (thisElements.getLast().equalsIgnoringEvents(other.getElements().getFirst())) {

			// check if the chain element of both chains have at least one event
			// in common
			LinkedList<GenericEvent> otherBasedUponEvents = other.getElements().getFirst().getBasedUponEvents();
			boolean atLeastOneEventMatches = false;
			for (GenericEvent ev : thisElements.getLast().getBasedUponEvents()) {
				if (otherBasedUponEvents.contains(ev)) {
					atLeastOneEventMatches = true;
					break;
				}
			}

			if (atLeastOneEventMatches) {
				// only remain the events of the combined order element that
				// occur in both chainElements
				thisElements.getLast().getBasedUponEvents().retainAll(otherBasedUponEvents);

				// -> combine the other chain into this one
				ret = true;

				// add all other elements to form the longer chain
				for (int i = 1; i < other.getElements().size(); i++) {
					ChainConstraintElement otherChainElement = other.getElements().get(i);
					thisElements.add(otherChainElement);
				}
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param ic2
	 * @return
	 */
	public boolean tryToInclude(ChainConstraint other) {
		boolean ret = false;

		if (!getOperator().equals(other.getOperator())) {
			return false;
		}

		LinkedList<ChainConstraintElement> thisElements = getElements();

		// check if all elements of the other chain are also in this chain
		boolean foundAllElements = true;
		for (ChainConstraintElement otherEl : other.getElements()) {
			boolean isAlsoInThisChain = false;

			for (ChainConstraintElement thisEl : thisElements) {
				// is the chain element equal?
				if (thisEl.equalsIgnoringEvents(otherEl)) {
					// we need at least one event to match in order to allow
					// this operation
					boolean foundAtLeastOneEvent = false;
					for (GenericEvent e : thisEl.getBasedUponEvents()) {
						if (otherEl.getBasedUponEvents().contains(e)) {
							foundAtLeastOneEvent = true;
							break;
						}
					}

					if (foundAtLeastOneEvent) {
						isAlsoInThisChain = true;
						break;
					}
				}
			}

			// if we did not find a match, we can instantly end here
			if (!isAlsoInThisChain) {
				foundAllElements = false;
				break;
			}
		}

		// if the other chains is contained in this one, we can include it
		if (foundAllElements) {
			ret = true;

			// only retain the events per element, that occur in both chain
			// elements, we already checked that at least one event must match
			// to both chain elements
			for (ChainConstraintElement otherEl : other.getElements()) {
				for (ChainConstraintElement thisEl : thisElements) {
					if (thisEl.equalsIgnoringEvents(otherEl)) {
						thisEl.getBasedUponEvents().retainAll(otherEl.getBasedUponEvents());
					}
				}
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param newElement
	 */
	public void addChainElement(ChainConstraintElement newElement) {
		elements.add(newElement);
	}

	/**
	 * 
	 * @param belongsTo
	 * @param attrName
	 * @param firstBasedUponEvent
	 */
	public void addChainElement(OrderElement belongsTo, String attrName, GenericEvent firstBasedUponEvent) {
		addChainElement(new ChainConstraintElement(belongsTo, attrName, firstBasedUponEvent));
	}

	/**
	 * 
	 * @param belongsTo
	 * @param attrName
	 */
	public void addChainElement(OrderElement belongsTo, String attrName) {
		addChainElement(new ChainConstraintElement(belongsTo, attrName));
	}

	/**
	 * Adds all events of the given chain each matching ChainElement of this
	 * Chain
	 * 
	 * @param currentInequalityChain
	 */
	public void addAllEventsToChainElements(ChainConstraint other) {
		// only add stuff if its meaningfull
		if (!other.operator.equals(this.operator) || elements == null || elements.size() < 1) {
			return;
		}

		// find the matching chainelements and then add the events of the other
		// to the elements of this chain
		int thisIndex = 0;
		ChainConstraintElement currentElement = elements.get(thisIndex);
		for (ChainConstraintElement otherChainElement : other.getElements()) {

			// are we talking about the same element in the chain?
			if (otherChainElement.equalsIgnoringEvents(currentElement)) {

				// add the events
				for (GenericEvent ev : otherChainElement.getBasedUponEvents()) {
					currentElement.addBasedUponEvents(ev, true);
				}

				thisIndex++;

				// no element in this chain left?
				if (thisIndex >= elements.size()) {
					break;
				}
				currentElement = elements.get(thisIndex);
			}
		}
	}

	/**
	 * @return the operator
	 */
	public EConstraintOperator getOperator() {
		return operator;
	}

	/**
	 * @param operator
	 *            the operator to set
	 */
	public void setOperator(EConstraintOperator operator) {
		this.operator = operator;
	}

	/**
	 * @return the elements
	 */
	public LinkedList<ChainConstraintElement> getElements() {
		return elements;
	}

	/**
	 * @param elements
	 *            the elements to set
	 */
	public void setElements(LinkedList<ChainConstraintElement> elements) {
		this.elements = elements;
	}

	@Override
	public String toString() {

		return "InequalityChain (" + operator.toString() + "): " + elements;
	}

}
