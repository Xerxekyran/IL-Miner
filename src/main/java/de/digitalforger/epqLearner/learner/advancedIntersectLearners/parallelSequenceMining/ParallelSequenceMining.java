package de.digitalforger.epqLearner.learner.advancedIntersectLearners.parallelSequenceMining;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPattern;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.Sequence;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;

/**
 * 
 * @author george
 *
 */
public class ParallelSequenceMining {
	private static Logger logger = Logger.getLogger(ParallelSequenceMining.class.getName());

	public int numParallelTasks = 10;

	/**
	 * 
	 * @param sequenceDbForBIDE
	 * @return
	 */
	public SequentialPatterns doParallelExecution(SequenceDatabase sequenceDbForBIDE) {
		SequentialPatterns fullResultingPatterns = new SequentialPatterns("Final Result");

		try {

			// first of all split the incoming sequence DB

			LinkedList<SequenceDatabase> sequenceDBs = splitSequenceDB(sequenceDbForBIDE);

			ExecutorService executor = Executors.newFixedThreadPool(sequenceDBs.size());
			List<FutureTask<SequentialPatterns>> taskList = new ArrayList<FutureTask<SequentialPatterns>>();

			// create parallel tasks			
			for (int j = 0; j < sequenceDBs.size(); j++) {
				SequenceDatabase sDB  = sequenceDBs.get(j);
			
				FutureTask<SequentialPatterns> newTask = new FutureTask<SequentialPatterns>(new SequenceDBCalculator(sDB));
				taskList.add(newTask);
				executor.execute(newTask);
			}

			// Wait until all results are available and combine them
			for (int j = 0; j < sequenceDBs.size(); j++) {
				FutureTask<SequentialPatterns> futureTask = taskList.get(j);

				SequentialPatterns sequentialPatterns = futureTask.get();

				joinSequentialPatterns(fullResultingPatterns, sequentialPatterns);
			}
			executor.shutdown();
		} catch (Exception e) {
			logger.warning(e.toString());
			e.printStackTrace();
		}
		return fullResultingPatterns;
	}

	/**
	 * 
	 * @param fullResultingPatterns
	 * @param sequentialPatterns
	 */
	private void joinSequentialPatterns(SequentialPatterns fullResultingPatterns, SequentialPatterns sequentialPatterns) {
		List<List<SequentialPattern>> levels = sequentialPatterns.getLevels();

		for (List<SequentialPattern> level : levels) {
			for (SequentialPattern p : level) {
				System.out.println("Found pattern: " + p.toString());
			}
		}
	}

	/**
	 * 
	 * @param sequenceDbForBIDE
	 * @return
	 */
	private LinkedList<SequenceDatabase> splitSequenceDB(SequenceDatabase sequenceDbForBIDE) {
		LinkedList<SequenceDatabase> ret = new LinkedList<SequenceDatabase>();
		
		// create new databases
		for (int i = 0; i < numParallelTasks; i++) {			
			ret.add(new SequenceDatabase());
		}
		
		Iterator<SequenceDatabase> dbIterator = ret.iterator();
		
		// assign the sequences among the databases
		for(int i = 0; i < sequenceDbForBIDE.getSequences().size(); i++) {
			Sequence s = sequenceDbForBIDE.getSequences().get(i);			
			
			
			if(!dbIterator.hasNext()) {
				dbIterator = ret.iterator();
			}			
			
			// we have to create new sequences, because their index needs to be set correctly again
			SequenceDatabase next = dbIterator.next();			
			Sequence newSequence = new Sequence(next.size());			
			for(List<Integer> set: s.getItemsets()) {
				newSequence.addItemset(set);	
			}						
			next.addSequence(newSequence);
		}
		
		
		return ret;
	}
}
