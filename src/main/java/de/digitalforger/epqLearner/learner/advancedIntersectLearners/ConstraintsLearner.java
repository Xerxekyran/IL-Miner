package de.digitalforger.epqLearner.learner.advancedIntersectLearners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.EConstraintOperator;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedElement;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedOrderForOneTrace;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.ILearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.ChainConstraint;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.ChainConstraintElement;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.ConstantConstraint;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.ConstantConstraintLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.ConstraintMerger;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.EventMappingsForTwoElementConstraint;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.EventsForChainConstraint;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.TrendLearner;
import de.digitalforger.epqLearner.util.ObjectCloner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * This is starting class for multiple constraints learner. The
 * MatchedEventInstanceOrder per pattern is used to find each kind of constraint
 * 
 * @author george
 * 
 */
public class ConstraintsLearner implements ILearner {
	private static Logger logger = Logger.getLogger(ConstraintsLearner.class.getName());
	private static boolean WITH_DEBUG_OUTPUT = false;

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the ConstraintsLearner");

		ConstantConstraintLearner constConstraintsLearner = new ConstantConstraintLearner();
		TrendLearner trendLearner = new TrendLearner();

		// this list saves the intermediate results of patterns for each order,
		// to save them later on back to the context
		LinkedList<Pattern> completeListOfPatterns = new LinkedList<Pattern>();

		// for each pattern (at this point we have one pattern for each existing
		// order)
		for (Pattern patternWithoutConstraints : ctx.getPatternMatchingLanguage().getPatterns()) {

			if (WITH_DEBUG_OUTPUT) {
				logger.info("------ Handling next pattern ------------");
				logger.info(patternWithoutConstraints.toString());
			}

			// starting from one pattern (for one order) this list may grow
			// depending on how the constraints can be combined in the patterns
			LinkedList<Pattern> patternsWithConstraints = new LinkedList<Pattern>();

			MatchedEventInstanceOrder matchedEventInstanceOrder = ctx.getMatchedEventOrders().get(patternWithoutConstraints);

			Pattern clonedPattern = null;
			try {
				clonedPattern = ObjectCloner.deepCopy(patternWithoutConstraints);
				// keep the object references as in the original object
				clonedPattern.setEventOrder(patternWithoutConstraints.getEventOrder());
				patternsWithConstraints.add(clonedPattern);
				ctx.getConstraintsOrderElementBasedUponEvents().put(clonedPattern, new HashMap<OrderElement, LinkedList<GenericEvent>>());
				ctx.getMatchedEventOrders().put(clonedPattern, matchedEventInstanceOrder);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not clone a pattern." + e);
				e.printStackTrace();
			}

			// This version of combining constraints has no knowledge about
			// based upon events. This approach uses less memory but needs
			// more trace parsing
			ConstraintMerger cMerger = new ConstraintMerger();

			// check for equality constraints like A.x == 10.2
			HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfEqualityConstraints = new HashMap<OrderElement, LinkedList<ConstantConstraint>>();
			// if the relevant event instance learner used all attributes to
			// get the relevant event instances, all equality constraints
			// are already handled
			if (ctx.isONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES()) {
				mapOfEqualityConstraints = constConstraintsLearner.checkForAssociatedEqualityConstraints(matchedEventInstanceOrder, ctx);
			}

			// check for equality constraints like A.x < 50
			HashMap<OrderElement, LinkedList<ConstantConstraint>> mapOfInequalityConstraints = null;
			if (ctx.isWITH_CONSTANT_INEQUALITY_CONSTRAINTS()) {
				mapOfInequalityConstraints = constConstraintsLearner.checkForInequalityConstraints(matchedEventInstanceOrder);
			}

			// check for inequality / equality chains like: A_0.x < A_1.x <
			// A_2.x
			LinkedList<ChainConstraint> chainConstraints = trendLearner.checkForChainConstraints(matchedEventInstanceOrder, ctx);

			if (WITH_DEBUG_OUTPUT) {
				logger.info("Chain Constraints for matched Order [" + matchedEventInstanceOrder.getOrderForTraces().values().iterator().next() + "]");
				logger.info(chainConstraints.toString());
			}
			// merge all found constraints into patterns
			// if constraints can not be present in the same we create
			// separate patterns here
			cMerger.mergeConstraints(mapOfEqualityConstraints, mapOfInequalityConstraints, chainConstraints, patternsWithConstraints, ctx);

			// adding all newly created patterns to the "true" list of patterns
			// (this one takes into account that not all constraints can be used
			// in the same pattern)
			completeListOfPatterns.addAll(patternsWithConstraints);
			ctx.getMatchedEventOrders().remove(patternWithoutConstraints);
		}

		// remove constraitns that express the same
		removeAmbiguishConstraints(completeListOfPatterns);

		// we have done all the constraint adding stuff
		// now refresh the patterns in the context
		LinkedList<Pattern> patternsInContext = ctx.getPatternMatchingLanguage().getPatterns();
		patternsInContext.clear();
		patternsInContext.addAll(completeListOfPatterns);
	}

	/**
	 * checks for constraints that are ambiguish removes them
	 * 
	 * @param completeListOfPatterns
	 */
	private void removeAmbiguishConstraints(LinkedList<Pattern> completeListOfPatterns) {
		for (Pattern p : completeListOfPatterns) {

			// 1:
			// check if we have this equality property constraint as part of
			// a equality relation constraint, only one is needed as they
			// express the same where there is this property constraint
			// again (Ev_0.a = '1', Ev_1.a = Ev_0.a, Ev_1.a ='1')
			for (PropertyConstraint propC : p.getPropertyConstraints()) {

				OrderElement belongsToOrderElement = propC.getBelongsToOrderElement();
				String propAttributeName = propC.getAttributeName();
				LinkedList<RelationConstraint> toDelete = new LinkedList<RelationConstraint>();

				if (propC.getConstraintOperator().equals(EConstraintOperator.Equal)) {
					for (RelationConstraint rc : p.getRelationConstraints()) {
						if (rc.getConstraintOperator().equals(EConstraintOperator.Equal)
								&& ((rc.getFromOrderElement() == belongsToOrderElement && rc.getFromOrderElementAttributeName().equals(propAttributeName)) || rc
										.getToOrderElement() == belongsToOrderElement && rc.getToOrderElementAttributeName().equals(propAttributeName))) {
							// constraints describing the same order element and
							// attribute --> we only stick to the property
							// constraints as it is an easier to read
							// description
							toDelete.add(rc);
						}
					}

					p.getRelationConstraints().removeAll(toDelete);
				}
			}

			// 2:
			// a.x < b.x < c.x
			// remove chain constraints that build larger chains and appear in
			// all chain elements
			// c.x > b.x
			// c.x > a.x <-- not needed
			// b.x > a.x

			LinkedList<LinkedList<RelationConstraint>> chains = new LinkedList<LinkedList<RelationConstraint>>();
			LinkedList<RelationConstraint> toDeleteDescription = new LinkedList<RelationConstraint>();

			for (int i = 0; i < p.getRelationConstraints().size() - 1; i++) {
				RelationConstraint relC0 = p.getRelationConstraints().get(i);

				for (int j = i + 1; j < p.getRelationConstraints().size(); j++) {

					RelationConstraint relC1 = p.getRelationConstraints().get(j);

					// they have the same operator
					if (relC0.getConstraintOperator().equals(relC1.getConstraintOperator())) {

						if (relC0.getFromOrderElement() == relC1.getToOrderElement()
								&& relC0.getFromOrderElementAttributeName().equals(relC1.getFromOrderElementAttributeName())) {
							// relC1.to == relC0.from
							LinkedList<RelationConstraint> newChain = new LinkedList<RelationConstraint>();
							newChain.add(relC1);
							newChain.add(relC0);
							chains.add(newChain);

							toDeleteDescription.add(new RelationConstraint(relC1.getFromOrderElement(), relC0.getToOrderElement(), relC1
									.getFromOrderElementAttributeName(), relC0.getToOrderElementAttributeName(), relC0.getConstraintOperator()));
						} else if (relC1.getFromOrderElement() == relC0.getToOrderElement()
								&& relC1.getFromOrderElementAttributeName().equals(relC0.getFromOrderElementAttributeName())) {
							// relC0.to == relC1.from
							LinkedList<RelationConstraint> newChain = new LinkedList<RelationConstraint>();
							newChain.add(relC0);
							newChain.add(relC1);
							chains.add(newChain);

							toDeleteDescription.add(new RelationConstraint(relC0.getFromOrderElement(), relC1.getToOrderElement(), relC0
									.getFromOrderElementAttributeName(), relC1.getToOrderElementAttributeName(), relC0.getConstraintOperator()));
						}
					}
				}
			}

			// now build even longer chains if possible
			boolean didSomething = false;
			do {
				didSomething = false;
				for (int i = 0; i < chains.size(); i++) {
					LinkedList<RelationConstraint> currentChain = chains.get(i);
					for (RelationConstraint relC : p.getRelationConstraints()) {
						RelationConstraint endOfChainConst = currentChain.getLast();

						if (relC.getConstraintOperator().equals(endOfChainConst.getConstraintOperator())) {

							if (relC.getFromOrderElement() == endOfChainConst.getToOrderElement()
									&& relC.getFromOrderElementAttributeName().equals(endOfChainConst.getFromOrderElementAttributeName())) {
								// endOfChainConst.to == relC.from
								currentChain.addLast(relC);

								toDeleteDescription.add(new RelationConstraint(currentChain.getFirst().getFromOrderElement(), relC.getToOrderElement(), currentChain
										.getFirst().getFromOrderElementAttributeName(), relC.getToOrderElementAttributeName(), relC.getConstraintOperator()));
								didSomething = true;
							}
						}
					}
				}
			} while (didSomething);

			// find the constraints that are not needed because we have longer
			// chains that describe them already
			LinkedList<RelationConstraint> toDelete = new LinkedList<RelationConstraint>();
			for (RelationConstraint rc : p.getRelationConstraints()) {
				for (RelationConstraint rcDelDesc : toDeleteDescription) {
					if (rcDelDesc.describeTheSameConstraint(rc)) {
						toDelete.add(rc);
					}
				}
			}
			logger.info("Removing " + toDelete.size() + " constraints that are included in chains (not needed).");
			p.getRelationConstraints().removeAll(toDelete);
		}
	}
}
