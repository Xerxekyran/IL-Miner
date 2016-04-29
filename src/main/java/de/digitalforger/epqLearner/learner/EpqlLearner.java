package de.digitalforger.epqLearner.learner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.PML.PML;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.ConstraintsLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.EventInstanceOccurenceCountLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.EventInstanceOrderLearnerWithSets;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.MatchOrderInstancesToTraceEventsLearner;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.RelevantEventInstanceLearnerWithSets;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.TimeWindowLearner;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * 
 * @author george
 * 
 */
public class EpqlLearner {

	private static Logger logger = Logger.getLogger(EpqlLearner.class.getName());

	private EpqlContext ctx = null;

	// for now hold the traces in two different formats (maybe both will be
	// needed from different learners)
	private LinkedList<HistoricalTrace> positiveTraces;
	private LinkedList<HistoricalTrace> negativeTraces;

	/**
	 * 
	 * @param testInstances
	 * @param positiveTraces
	 * @param negativeTraces
	 */
	public EpqlLearner(LinkedList<HistoricalTrace> positiveTraces, LinkedList<HistoricalTrace> negativeTraces) {
		this.positiveTraces = positiveTraces;
		this.negativeTraces = negativeTraces;
	}

	/**
	 * 
	 * @param ctx
	 * @param testInstances
	 * @param positiveTraces
	 * @param negativeTraces
	 */
	public EpqlLearner(EpqlContext ctx, LinkedList<HistoricalTrace> positiveTraces, LinkedList<HistoricalTrace> negativeTraces) {
		this.ctx = ctx;

		this.positiveTraces = positiveTraces;
		this.negativeTraces = negativeTraces;
	}

	/**
	 * init context
	 */
	private void initContext() {
		if (ctx == null) {
			ctx = new EpqlContext();
		}

		ctx.setPositiveTraces(positiveTraces);
		ctx.setNegativeTraces(negativeTraces);
		ctx.getPerformanceMeasurements().numOfUsedTraces = positiveTraces.size();
	}

	/**
	 * 
	 * @param testTraces
	 * @param relevantEventTypes
	 */
	private void removeIrrelevantEventInstances(LinkedList<HistoricalTrace> testTraces, LinkedList<EventInstance> relevantEventInstances) {
		// look in each trace at each event if its really a relevant one
		// if not remove it from the trace
		for (HistoricalTrace trace : testTraces) {
			for (int i = 0; i < trace.getEvents().size(); i++) {

				boolean found = false;
				for (EventInstance evInst : relevantEventInstances) {
					if (evInst.attributesWithValuesAreContainedIn(trace.getEvents().get(i))) {
						found = true;
						break;
					}
				}
				if (!found) {
					trace.getEvents().remove(i);
					i--;
				}

			}
		}
	}

	/**
	 * executes certain parts of the learning process in parallel
	 * 
	 * @return
	 */
	private PML executeWithNewOrderInParallel() {
		// start a new context
		initContext();

		// start a new pattern language
		PML pml = new PML();
		pml.getPatterns().add(new Pattern("TestPattern"));

		// add it to the context so all algorithms can manipulate it
		ctx.setPatternMatchingLanguage(pml);

		ILearner learner;
		long startTime;

		// STEP 1, relevant event instances based on ordinal attributes (the
		// type is one ordinal attribute)
		learner = new RelevantEventInstanceLearnerWithSets();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		long timeMeasurement = (System.currentTimeMillis() - startTime);
		ctx.getPerformanceMeasurements().timeForRelevantEventInstancesLearner = timeMeasurement;
		logger.log(Level.FINE, "Time for Step 1[" + timeMeasurement + " ms]");
		// now remove irrelevant event types from the trace
		removeIrrelevantEventInstances(ctx.getPositiveTraces(), ctx.getRelevantEventInstances());

		// STEP 2, count the occurrences of each relevant event instance
		learner = new EventInstanceOccurenceCountLearner();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		timeMeasurement = (System.currentTimeMillis() - startTime);
		logger.log(Level.FINE, "Time for Step 2[" + timeMeasurement + " ms]");

		// STEP 3, find possible orders
		learner = new EventInstanceOrderLearnerWithSets();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		timeMeasurement = (System.currentTimeMillis() - startTime);
		ctx.getPerformanceMeasurements().timeForOrderLearner = timeMeasurement;
		logger.log(Level.FINE, "Time for Step 3[" + timeMeasurement + " ms]");

		// from now on execute all learner in a sequence for each learned order
		// (parallel execution possible?)

		// this list stores all patterns with orders (no constraints yet)
		LinkedList<Pattern> allFoundOrders = new LinkedList<Pattern>(ctx.getPatternMatchingLanguage().getPatterns());
		int maxParallelTasks = 16;

		ExecutorService executor = Executors.newFixedThreadPool(maxParallelTasks);
		List<Callable<LinkedList<Pattern>>> tasks = new ArrayList<Callable<LinkedList<Pattern>>>();

		LinkedList<Pattern> allFoundPatternsInParallelMode = new LinkedList<Pattern>();

		startTime = System.currentTimeMillis();
		try {

			for (Pattern pattern : allFoundOrders) {
				// STEP 4 (and 5) do the matching and constraint learner stuff in parallel
				EpqlContext newCTX = ctx.cloneButKeepTraces();
				tasks.add(new ParallelMatchAndConstraintExecution(newCTX, pattern));
			}

			List<Future<LinkedList<Pattern>>> results = executor.invokeAll(tasks);
			for (Future<LinkedList<Pattern>> fr : results) {
				LinkedList<Pattern> linkedList = fr.get();
				logger.info("Got some patterns from parallel execution: " + linkedList.size());
				if (linkedList.size() > 0) {
					synchronized (allFoundPatternsInParallelMode) {
						allFoundPatternsInParallelMode.addAll(linkedList);
					}
				}
			}

			executor.shutdown();

		} catch (Exception e) {
			logger.warning(e.toString());
			e.printStackTrace();
		}
		timeMeasurement = (System.currentTimeMillis() - startTime);
		ctx.getPerformanceMeasurements().timeForConstraintLearner = timeMeasurement;		
		logger.info("Parallel execution finished, found " + allFoundPatternsInParallelMode.size() + " patterns.");
		logger.info("Time for Parallel execution [" + timeMeasurement + " ms]");

		ctx.getPatternMatchingLanguage().getPatterns().clear();
		ctx.getPatternMatchingLanguage().getPatterns().addAll(allFoundPatternsInParallelMode);
		ctx.getMatchedEventOrders().clear();
		
		// STEP 7, evaluate found pattern		
		learner = new PatternFilterLeaner();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		timeMeasurement = (System.currentTimeMillis() - startTime);
		logger.log(Level.FINE, "Time for Step 7 - Pattern Filter[" + timeMeasurement + " ms]");
		
		ctx.getPositiveTraces().clear();
		ctx.getNegativeTraces().clear();
		ctx.getConstraintsOrderElementBasedUponEvents().clear();
		ctx.getEventInstanceOccurrenceCount().clear();
		return pml;
	}

	/**
	 * 
	 * @return
	 */
	private PML executeWithNewOrder() {
		// start a new context
		initContext();

		// start a new pattern language
		PML pml = new PML();
		pml.getPatterns().add(new Pattern("TestPattern"));

		// add it to the context so all algorithms can manipulate it
		ctx.setPatternMatchingLanguage(pml);

		ILearner learner;
		long startTime;

		// STEP 1, relevant event instances based on ordinal attributes (the
		// type is one ordinal attribute)
		learner = new RelevantEventInstanceLearnerWithSets();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		long timeMeasurement = (System.currentTimeMillis() - startTime);
		ctx.getPerformanceMeasurements().timeForRelevantEventInstancesLearner = timeMeasurement;
		logger.log(Level.FINE, "Time for Step 1[" + timeMeasurement + " ms]");
		// now remove irrelevant event types from the trace
		removeIrrelevantEventInstances(ctx.getPositiveTraces(), ctx.getRelevantEventInstances());

		// STEP 2, count the occurrences of each relevant event instance
		learner = new EventInstanceOccurenceCountLearner();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		timeMeasurement = (System.currentTimeMillis() - startTime);
		logger.log(Level.FINE, "Time for Step 2[" + timeMeasurement + " ms]");

		// STEP 3, find possible orders
		learner = new EventInstanceOrderLearnerWithSets();
		startTime = System.currentTimeMillis();
		learner.performOneStep(ctx);
		timeMeasurement = (System.currentTimeMillis() - startTime);
		ctx.getPerformanceMeasurements().timeForOrderLearner = timeMeasurement;
		logger.log(Level.FINE, "Time for Step 3[" + timeMeasurement + " ms]");

		// from now on execute all learner in a sequence for each learned order
		// (parallel execution possible?)

		// this list stores all patterns with orders (no constraints yet)
		LinkedList<Pattern> allFoundOrders = new LinkedList<Pattern>(ctx.getPatternMatchingLanguage().getPatterns());

		// this list saves all patterns with constraints (as a results of the
		// constraint learner)
		LinkedList<Pattern> allFoundPatterns = new LinkedList<Pattern>();

		for (Pattern pattern : allFoundOrders) {
			ctx.getPatternMatchingLanguage().getPatterns().clear();
			ctx.getMatchedEventOrders().clear();
			ctx.getPatternMatchingLanguage().getPatterns().add(pattern);
			logger.info("Now dealing with the following order: " + pattern.getEventOrder());

			// STEP 4, match the found orders to trace events
			learner = new MatchOrderInstancesToTraceEventsLearner();
			startTime = System.currentTimeMillis();
			learner.performOneStep(ctx);
			timeMeasurement = (System.currentTimeMillis() - startTime);
			// ctx.getPerformanceMeasurements().timeForMatchesLearner += timeMeasurement;
			logger.log(Level.FINE, "Time for Step 4[" + timeMeasurement + " ms]");

			// STEP 5, find constant constraints on the matched order elements
			learner = new ConstraintsLearner();
			startTime = System.currentTimeMillis();
			learner.performOneStep(ctx);
			timeMeasurement = (System.currentTimeMillis() - startTime);
			logger.log(Level.FINE, "Time for Step 5[" + timeMeasurement + " ms]");

			// remember the final patterns from this learner in the list of all
			// patterns
			allFoundPatterns.addAll(ctx.getPatternMatchingLanguage().getPatterns());

			// STEP 6, read the maximal needed time for the pattern to match the
			// traces
			learner = new TimeWindowLearner();
			startTime = System.currentTimeMillis();
			learner.performOneStep(ctx);
			timeMeasurement = (System.currentTimeMillis() - startTime) + timeMeasurement;
			ctx.getPerformanceMeasurements().timeForConstraintLearner += timeMeasurement;
			logger.log(Level.FINE, "Time for Step 6[" + (System.currentTimeMillis() - startTime) + " ms]");

			// STEP 7, evaluate found pattern
			learner = new PatternFilterLeaner();
			startTime = System.currentTimeMillis();
			learner.performOneStep(ctx);
			logger.log(Level.FINE, "Time for Step 7[" + (System.currentTimeMillis() - startTime) + " ms]");
		}
		ctx.getPatternMatchingLanguage().getPatterns().clear();
		ctx.getPatternMatchingLanguage().getPatterns().addAll(allFoundPatterns);
		return pml;
	}

	/**
	 * 
	 */
	public PML executeEverything() {
		return executeWithNewOrderInParallel();
//		 return executeWithNewOrder();
	}

	/**
	 * get the context object of the learner
	 * 
	 * @return
	 */
	public EpqlContext getContext() {
		return this.ctx;
	}
}
