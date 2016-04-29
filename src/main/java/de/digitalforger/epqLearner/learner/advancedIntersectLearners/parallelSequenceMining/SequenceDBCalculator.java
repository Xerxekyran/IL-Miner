package de.digitalforger.epqLearner.learner.advancedIntersectLearners.parallelSequenceMining;

import java.util.concurrent.Callable;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;

public class SequenceDBCalculator implements Callable<SequentialPatterns> {
	private SequenceDatabase sDB = null;
	private AlgoBIDEPlus algo = new AlgoBIDEPlus();
	
	/**
	 * 
	 * @param sDB
	 */
	public SequenceDBCalculator(SequenceDatabase sDB) {
		this.sDB = sDB;		
	}

	@Override
	public SequentialPatterns call() throws Exception {
		System.out.println("Executing new parallel task for BIDE algorithm: "+ sDB.size());
		return algo.runAlgorithm(sDB, null, sDB.size());
	}
}
