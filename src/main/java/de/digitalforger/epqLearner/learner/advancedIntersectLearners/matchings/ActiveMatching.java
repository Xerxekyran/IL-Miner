package de.digitalforger.epqLearner.learner.advancedIntersectLearners.matchings;

import java.util.LinkedList;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedElement;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.event.GenericEvent;

/**
 * 
 * @author george
 *
 */
public class ActiveMatching {
	private static Logger logger = Logger.getLogger(ActiveMatching.class.getName());

	EventInstanceOrder order = null;
	int activeOrderElementIndex = -1;
	MatchedOrderForOneTrace matchedOrder = new MatchedOrderForOneTrace();
	boolean matchesWholeOrder = false;

	/**
	 * 
	 * @param order
	 */
	public ActiveMatching(EventInstanceOrder order) {
		this.order = order;
		if (this.order.size() > 0) {
			activeOrderElementIndex = 0;
			MatchedElement currentMatchingElement = new MatchedElement();
			currentMatchingElement.setOrderElement(order.get(0));
			matchedOrder.addNewMatchedElement(currentMatchingElement);
		} else {
			logger.warning("Created an ActiveMatching object with an empty order");
		}
	}

	/**
	 * 
	 * @param order
	 * @param orderElementIndex
	 * @param currentMatchingElement
	 * @param matchesWholeOrder
	 */
	private ActiveMatching(EventInstanceOrder order, int orderElementIndex, MatchedOrderForOneTrace matchedOrder, boolean matchesWholeOrder) {
		this.order = order;
		if (this.order.size() > 0) {
			this.activeOrderElementIndex = orderElementIndex;
			this.matchedOrder = matchedOrder;
			this.matchesWholeOrder = matchesWholeOrder;
		} else {
			logger.warning("Created an ActiveMatching object with an empty order");
		}
	}

	/**
	 * 
	 * @param e
	 */
	public ActiveMatching nextEvent(GenericEvent e) {
		// if we do not have any order element to use, just return
		if (activeOrderElementIndex < 0) {
			return null;
		}

		ActiveMatching ret = null;

		// is there a possible next order element?
		if (activeOrderElementIndex + 1 < order.size() && getMatchedOrder().getMatchedElements().getLast().getMatchedEvents().size() > 0) {
			// if yes check if this one would match
			if (order.get(activeOrderElementIndex + 1).getEventInstance().attributesWithValuesAreContainedIn(e)) {
				// if the next element matches we need to create a new active
				// matching. this one remains on the current order element and
				// the other active match will handle the skipping to the next
				// order element
				ret = cloneWithNextOrderElement();
				if (ret != null) {
					ret.getMatchedOrder().getMatchedElements().getLast().getMatchedEvents().add(e);
					ret.checkForCompleteness();
				}
			}
		}

		// can we add it to the current order element?
		if (order.get(activeOrderElementIndex).getEventInstance().attributesWithValuesAreContainedIn(e)) {
			// it matches the current element -> add it to the matching order
			matchedOrder.getMatchedElements().getLast().getMatchedEvents().add(e);
			checkForCompleteness();
		}

		return ret;
	}

	/**
	 * 
	 * @param am
	 * @return
	 */
	public boolean mergeWithActiveMatchingAtSameOrderPosition(ActiveMatching am) {
		int orderSize = getMatchedOrder().getMatchedElements().size();
		if (orderSize != am.getMatchedOrder().getMatchedElements().size()) {
			logger.warning("Trying to merge matchings with different order lengths!");
			return false;
		}

		if (getActiveOrderElementIndex() != am.getActiveOrderElementIndex()) {
			logger.warning("Trying to merge matchings that are not at the same position in the order, this is not allowed!");
			return false;
		}
		
		for (int i = 0; i < orderSize; i++) {
			LinkedList<GenericEvent> sourceList = getMatchedOrder().getMatchedElements().get(i).getMatchedEvents();
			LinkedList<GenericEvent> toAdd = am.getMatchedOrder().getMatchedElements().get(i).getMatchedEvents();

			for (GenericEvent toAddEvent : toAdd) {
				if (!sourceList.contains(toAddEvent)) {
					sourceList.add(toAddEvent);
				}
			}
		}
		
		return true;		
	}
	
	/**
	 * 
	 * @param am
	 */
	public boolean mergeWithFinishedActiveMatching(ActiveMatching am) {
		int orderSize = getMatchedOrder().getMatchedElements().size();

		if (orderSize != am.getMatchedOrder().getMatchedElements().size()) {
			logger.warning("Trying to merge matchings with different order lengths!");
			return false;
		}

		if (!isMatchingWholeOrder() || !am.isMatchingWholeOrder()) {
			logger.warning("Trying to merge matchings that did not already match the whole order, this is not allowed!");
			return false;
		}

		for (int i = 0; i < orderSize; i++) {
			LinkedList<GenericEvent> sourceList = getMatchedOrder().getMatchedElements().get(i).getMatchedEvents();
			LinkedList<GenericEvent> toAdd = am.getMatchedOrder().getMatchedElements().get(i).getMatchedEvents();

			for (GenericEvent toAddEvent : toAdd) {
				if (!sourceList.contains(toAddEvent)) {
					sourceList.add(toAddEvent);
				}
			}
		}

		return true;
	}

	/**
	 * 
	 */
	private void checkForCompleteness() {
		boolean allMEHaveAtLeastOneEvent = true;
		// do we have a matched element for each order element?
		if (matchedOrder.getMatchedElements().size() != order.size()) {
			allMEHaveAtLeastOneEvent = false;
		} else {
			// does every mathed element have at least one matching event?
			for (MatchedElement me : matchedOrder.getMatchedElements()) {
				if (me.getMatchedEvents().size() == 0) {
					allMEHaveAtLeastOneEvent = false;
					break;
				}
			}
		}

		this.matchesWholeOrder = allMEHaveAtLeastOneEvent;
	}

	/**
	 * 
	 */
	public ActiveMatching cloneWithNextOrderElement() {
		// if the order is at its end, we can not create a clone with the next
		// element
		if (order.size() <= activeOrderElementIndex + 1) {
			return null;
		}

		// create a new matched element
		MatchedElement nextMatchingElement = new MatchedElement();
		nextMatchingElement.setOrderElement(order.get(activeOrderElementIndex + 1));

		MatchedOrderForOneTrace clonedMO = this.matchedOrder.cloneWithNewMatchedElementsList();
		clonedMO.addNewMatchedElement(nextMatchingElement);

		ActiveMatching ret = new ActiveMatching(this.order, this.activeOrderElementIndex + 1, clonedMO, this.matchesWholeOrder);

		return ret;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isMatchingWholeOrder() {
		return matchesWholeOrder;
	}

	public int getActiveOrderElementIndex() {
		return activeOrderElementIndex;
	}

	public void setActiveOrderElementIndex(int activeOrderElementIndex) {
		this.activeOrderElementIndex = activeOrderElementIndex;
	}

	@Override
	public String toString() {
		return "ActiveMatching [" + matchedOrder + "]";
	}

	public MatchedOrderForOneTrace getMatchedOrder() {
		return matchedOrder;
	}



}
