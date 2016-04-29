package de.digitalforger.epqLearner.PML.constraints.order;

import java.util.LinkedList;

import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.matchings.ActiveMatching;

/**
 * 
 * @author george
 * 
 */
public class MatchedOrderForOneTrace {

	private LinkedList<MatchedElement> matchedElements = new LinkedList<MatchedElement>();

	/**
	 * @return the matchedElements
	 */
	public LinkedList<MatchedElement> getMatchedElements() {
		return matchedElements;
	}

	/**
	 * 
	 * @param me
	 */
	public void addNewMatchedElement(MatchedElement me) {
		matchedElements.add(me);
	}

	/**
	 * 
	 * @return
	 */
	public MatchedOrderForOneTrace cloneWithNewMatchedElementsList() {
		MatchedOrderForOneTrace ret = new MatchedOrderForOneTrace();

		for (MatchedElement me : getMatchedElements()) {
			MatchedElement clone = me.cloneWithNewList();
			ret.getMatchedElements().add(clone);
		}

		return ret;
	}

	/**
	 * 
	 * @param am
	 */
	public void mergeWithFinishedActiveMatching(ActiveMatching am) {
		for (int i = 0; i < am.getMatchedOrder().getMatchedElements().size(); i++) {

			// if the active matching has more matched elements, we can just use
			// them
			if (getMatchedElements().size() <= i) {
				getMatchedElements().add(am.getMatchedOrder().getMatchedElements().get(i));
			} else {
				if (getMatchedElements().get(i).getOrderElement() != am.getMatchedOrder().getMatchedElements().get(i).getOrderElement()) {
					System.out.println("!!!!!!!!!!!!!!!!!!!!! WRONG COMBINATION");
				}

				addAllWithNoDuplicates(getMatchedElements().get(i).getMatchedEvents(), am.getMatchedOrder().getMatchedElements().get(i).getMatchedEvents());
				// getMatchedElements().get(i).getMatchedEvents().addAll(am.getMatchedOrder().getMatchedElements().get(i).getMatchedEvents());
			}
		}
	}

	/**
	 * 
	 * @param sourceList
	 * @param toAdd
	 */
	private void addAllWithNoDuplicates(LinkedList<GenericEvent> sourceList, LinkedList<GenericEvent> toAdd) {
		for (GenericEvent toAddEvent : toAdd) {
			if (!sourceList.contains(toAddEvent)) {
				sourceList.add(toAddEvent);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("MOFOT[");

		for (MatchedElement me : matchedElements) {
			ret.append(me + ", ");
		}

		ret.append("]");

		return ret.toString();
	}

}
