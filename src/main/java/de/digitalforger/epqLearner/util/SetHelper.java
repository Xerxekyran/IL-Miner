package de.digitalforger.epqLearner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class SetHelper {

	public static <T> TreeSet<TreeSet<T>> powersetNoDuplicates(Collection<T> list) {
		TreeSet<TreeSet<T>> ps = new TreeSet<TreeSet<T>>();
		  ps.add(new TreeSet<T>());   // add the empty set
		 
		  // for every item in the original list
		  for (T item : list) {
			  TreeSet<TreeSet<T>> newPs = new TreeSet<TreeSet<T>>();
		 
		    for (TreeSet<T> subset : ps) {
		      // copy all of the current powerset's subsets
		      newPs.add(subset);
		 
		      // plus the subsets appended with the current item
		      TreeSet<T> newSubset = new TreeSet<T>(subset);
		      newSubset.add(item);
		      newPs.add(newSubset);
		    }
		 
		    // powerset is now powerset of list.subList(0, list.indexOf(item)+1)
		    ps = newPs;
		  }
		  return ps;		  
		}
	
	/**
	 * 
	 * @param originalSet
	 * @return
	 */
	public static <T> List<List<T>> powerSet(List<T> originalSet) {
		// result size will be 2^n, where n=size(originalset)
		// good to initialize the array size to avoid dynamic growing
		int resultSize = (int) Math.pow(2, originalSet.size());
		// resultPowerSet is what we will return
		List<List<T>> resultPowerSet = new ArrayList<List<T>>(resultSize);

		// Initialize result with the empty set, which powersets contain by
		// definition
		resultPowerSet.add(new ArrayList<T>(0));

		// for every item in the original list
		for (T itemFromOriginalSet : originalSet) {

			// iterate through the existing powerset result
			// loop through subset and append to the resultPowerset as we go
			// must remember size at the beginning, before we append new
			// elements
			int startingResultSize = resultPowerSet.size();
			for (int i = 0; i < startingResultSize; i++) {
				// start with an existing element of the powerset
				List<T> oldSubset = resultPowerSet.get(i);

				// create a new element by adding a new item from the original
				// list
				List<T> newSubset = new ArrayList<T>(oldSubset);
				newSubset.add(itemFromOriginalSet);

				// add this element to the result powerset (past
				// startingResultSize)
				resultPowerSet.add(newSubset);
			}
		}
		return resultPowerSet;
	}
}
