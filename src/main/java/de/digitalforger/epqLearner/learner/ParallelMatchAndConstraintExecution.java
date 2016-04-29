package de.digitalforger.epqLearner.learner;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.ConstraintsLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.MatchOrderInstancesToTraceEventsLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.TimeWindowLearner;

/**
 * 
 * @author george
 *
 */
public class ParallelMatchAndConstraintExecution implements Callable<LinkedList<Pattern>> {

	private static Logger logger = Logger.getLogger(ParallelMatchAndConstraintExecution.class.getName());

	private EpqlContext ctx = null;
	private Pattern pattern = null;
	private LinkedList<Pattern> foundPatterns = new LinkedList<Pattern>();

	/**
	 * 
	 * @param learner
	 */
	public ParallelMatchAndConstraintExecution(EpqlContext ctx, Pattern pattern) {
		this.ctx = ctx;
		this.pattern = pattern;
	}

	@Override
	public LinkedList<Pattern> call() throws Exception {
		logger.info("Starting new parallel execution");

		ILearner learner;

		ctx.getPatternMatchingLanguage().getPatterns().clear();
		ctx.getMatchedEventOrders().clear();
		ctx.getPatternMatchingLanguage().getPatterns().add(pattern);
		logger.info("Now dealing with the following order: " + pattern.getEventOrder());
//		if(pattern.getEventOrder().toString().contains("EventInstanceOrder [0: ClusterEv{Status=\"S\", ##EventTypeName=ClusterEv, Class=\"A\"}, 1: ClusterEv{Status=\"S\", ##EventTypeName=ClusterEv}, 2: ClusterEv{Status=\"T\", ##EventTypeName=ClusterEv}")) {
//			// pattern to debug for
//			System.out.println("where at it");
//		}
		// STEP 4, match the found orders to trace events
		learner = new MatchOrderInstancesToTraceEventsLearner();
		learner.performOneStep(ctx);

		// STEP 5, find constant constraints on the matched order elements
		learner = new ConstraintsLearner();
		learner.performOneStep(ctx);

		// remember the final patterns from this learner in the list of all
		// patterns
		foundPatterns.addAll(ctx.getPatternMatchingLanguage().getPatterns());

		// STEP 6, read the maximal needed time for the pattern to match the
		// traces
		learner = new TimeWindowLearner();
		learner.performOneStep(ctx);		
		
		logger.info("One parallel execution has finished");
		
		return foundPatterns;
	}
}
