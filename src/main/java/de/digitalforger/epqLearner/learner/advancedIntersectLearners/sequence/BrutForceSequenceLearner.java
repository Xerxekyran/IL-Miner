package de.digitalforger.epqLearner.learner.advancedIntersectLearners.sequence;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPattern;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.Sequence;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;

/**
 * A self written sequential pattern mining learner, that takes advantage of the
 * precondition, that every sequence needs to be present in all traces
 * 
 * @author george
 *
 */
public class BrutForceSequenceLearner {

	Set<Integer> sequenceIDs = null;

	/**
	 * 
	 * @param sequenceDbForBIDE
	 * @return
	 */
	public SequentialPatterns runAlgorithm(SequenceDatabase sequenceDb) {
		SequentialPatterns ret = new SequentialPatterns("Sequential Patterns with 100% support");
		LinkedList<SequentialPattern> usedPatterns = new LinkedList<SequentialPattern>();

		// find the shortest one
		Sequence shortestSequence = null;
		int shortestSequenceLength = Integer.MAX_VALUE;

		for (Sequence s : sequenceDb.getSequences()) {
			if (s.size() < shortestSequenceLength) {
				shortestSequenceLength = s.size();
				shortestSequence = s;
			}
		}

		// build all possible sequential patterns from this (only one item per
		// itemset)
		List<SequentialPattern> possibleSequentialPatterns = buildAllPossibleLongestSequences(shortestSequence.getItemsets());

		// check each sequential pattern if it occurs in all traces, if not
		// reduce it by one element (all possible combinations) and retry it by
		// adding them to the list of possible patterns
		boolean didSomething = true;

		while (didSomething) {
			didSomething = false;
			List<SequentialPattern> newPossibleSequentialPatterns = new LinkedList<SequentialPattern>();

			for (SequentialPattern sp : possibleSequentialPatterns) {
				if (!occursInAllTraces(sp, sequenceDb)) {
					// reduce the sequence by one
					List<SequentialPattern> reducedSequentialPatterns = reducePatternByOne(sp);
					if (!reducedSequentialPatterns.isEmpty()) {

						for (SequentialPattern newP : reducedSequentialPatterns) {
							if (!sequentialPatternIsInList(newPossibleSequentialPatterns, newP) && !sequentialPatternContainedInListOfPatterns(usedPatterns, newP)) {
								newPossibleSequentialPatterns.add(newP);
								didSomething = true;
							}
						}
					}
				} else {
					// its a fitting sequential pattern
					// check if its a subpattern, then we do not need it
					if (!sequentialPatternContainedInListOfPatterns(usedPatterns, sp)) {
						usedPatterns.add(sp);
						ret.addSequence(sp, sp.size());
						sp.setSequenceIDs(sequenceDb.getSequenceIDs());
					}
				}
			}

			possibleSequentialPatterns.clear();
			possibleSequentialPatterns.addAll(newPossibleSequentialPatterns);
		}

		return ret;
	}

	/**
	 * 
	 * @param lst
	 * @param p
	 * @return
	 */
	private boolean sequentialPatternIsInList(List<SequentialPattern> lst, SequentialPattern p) {
		boolean existsInList = false;

		// check if already have this sequence
		for (SequentialPattern sp : lst) {
			if (sp.size() == p.size()) {
				boolean allTheSame = true;
				for (int k = 0; k < sp.size(); k++) {
					if (!sp.get(k).containsAll(p.get(k)) || !p.get(k).containsAll(sp.get(k))) {
						allTheSame = false;
						break;
					}
				}

				if (allTheSame) {
					existsInList = true;
					break;
				}
			}
		}

		return existsInList;
	}

	/**
	 * 
	 * @param lst
	 * @param p
	 * @return
	 */
	private boolean sequentialPatternContainedInListOfPatterns(LinkedList<SequentialPattern> lst, SequentialPattern p) {
		boolean ret = false;

		for (SequentialPattern p0 : lst) {
			ret = sequentialPatternContainedInPattern(p0, p);
			if (ret) {
				break;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param lst
	 * @param p
	 * @return
	 */
	private boolean sequentialPatternContainedInPattern(SequentialPattern bigPattern, SequentialPattern smallPattern) {
		boolean ret = true;

		int indexInPattern = 0;

		for (Itemset itemset : bigPattern.getItemsets()) {
			if (itemset.containsAll(smallPattern.getItemsets().get(indexInPattern))) {
				indexInPattern++;

				if (indexInPattern >= smallPattern.size()) {
					break;
				}
			}
		}

		// if we have not found a match in the current sequence
		if (indexInPattern < smallPattern.size()) {
			ret = false;
		}

		return ret;
	}

	/**
	 * 
	 * @param sp
	 * @return
	 */
	private List<SequentialPattern> reducePatternByOne(SequentialPattern sp) {
		List<SequentialPattern> ret = new LinkedList<SequentialPattern>();

		// for(Itemset isToRemove : sp.getItemsets()) {
		for (int i = 0; i < sp.getItemsets().size(); i++) {
			SequentialPattern cloneSequence = sp.cloneSequence();
			cloneSequence.getItemsets().remove(i);

			if (!sequentialPatternIsInList(ret, sp) && cloneSequence.size() > 0) {
				ret.add(cloneSequence);
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param sp
	 * @param sequenceDb
	 * @return
	 */
	private boolean occursInAllTraces(SequentialPattern sp, SequenceDatabase sequenceDb) {
		boolean ret = true;

		for (Sequence s : sequenceDb.getSequences()) {
			if (!patternOccursInSequence(sp, s)) {
				ret = false;
				break;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param sp
	 * @param s
	 * @return
	 */
	private boolean patternOccursInSequence(SequentialPattern sp, Sequence s) {
		boolean ret = true;

		int indexInPattern = 0;

		for (List<Integer> itemset : s.getItemsets()) {
			if (itemset.contains(sp.get(indexInPattern).get(0))) {
				indexInPattern++;

				if (indexInPattern >= sp.size()) {
					break;
				}
			}
		}

		// if we have not found a match in the current sequence
		if (indexInPattern < sp.size()) {
			ret = false;
		}

		return ret;
	}

	/**
	 * 
	 * @param itemsets
	 * @return
	 */
	private List<SequentialPattern> buildAllPossibleLongestSequences(List<List<Integer>> itemsets) {
		List<SequentialPattern> ret = new LinkedList<SequentialPattern>();

		Iterator<List<Integer>> iterator = itemsets.iterator();
		Long orderCounter = 0L;
		while (iterator.hasNext()) {
			List<Integer> items = iterator.next();

			// first element?
			if (orderCounter == 0) {
				// we create a new order for each event instance
				for (Integer id : items) {
					SequentialPattern newPattern = new SequentialPattern();
					newPattern.addItemset(new Itemset(id));
					ret.add(newPattern);
				}
			} else {
				// every element after the first one adds itself to
				// all existing orders
				// with mutliple elements new orders get created

				LinkedList<SequentialPattern> retList = new LinkedList<SequentialPattern>();

				for (Integer id : items) {
					LinkedList<SequentialPattern> listOfCurrentOrders = new LinkedList<SequentialPattern>();
					for (SequentialPattern sp : ret) {
						SequentialPattern newSP = new SequentialPattern();
						for (int i = 0; i < sp.size(); i++) {
							newSP.addItemset(sp.get(i));
						}

						listOfCurrentOrders.add(newSP);
					}

					retList.addAll(listOfCurrentOrders);

					for (SequentialPattern order : listOfCurrentOrders) {
						order.addItemset(new Itemset(id));
					}

				}
				ret = retList;

			}

			orderCounter++;
		}

		return ret;
	}

}
