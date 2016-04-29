package de.digitalforger.epqLearner;

import java.util.HashMap;
import java.util.LinkedList;

import de.digitalforger.epqLearner.PML.EventInstance;
import de.digitalforger.epqLearner.PML.PML;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.order.MatchedEventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.GenericEvent;
import de.digitalforger.epqLearner.learner.advancedIntersectLearners.constraints.RelationshipConstraintPartner;
import de.digitalforger.epqLearner.util.analyze.PerformanceMeasurements;
import de.digitalforger.epqLearner.util.traceSource.HistoricalTrace;

/**
 * 
 * @author george
 * 
 */
public class EpqlContext {

	private boolean useMinSupportVersion = false;
	private PML patternMatchingLanguage = null;
	private LinkedList<Pattern> filteredPatterns = new LinkedList<Pattern>();
	private int maxFilteredPatternsFromSimliarityCluster = 3;
	private LinkedList<HistoricalTrace> positiveTraces = null;
	private LinkedList<HistoricalTrace> negativeTraces = null;
	private ESequentialMiningAlgorithm sequentialMiningAlgorithm = ESequentialMiningAlgorithm.BrutForce;
	private Integer maximumPositiveTestTraces = Integer.MAX_VALUE;
	private String filePrefix = "";

	private boolean ONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES = false;

	private boolean WITH_CONSTANT_INEQUALITY_CONSTRAINTS = false;

	private boolean WITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET = false;
	private boolean WITH_SUPERSET_REDUCTION_OPTIMIZATION = true;
	private boolean WITH_KEEP_ALL_CREATED_EVENT_INSTANCES = false;
	private boolean WITH_LOW_INFORMATION_VALUE_REDUCTION = false;
	private boolean WITH_CHAIN_MERGING = false;

	private boolean ONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE = false;

	private boolean ONLY_TAKE_LONGEST_FOUND_SEQUENCES = false;
	private boolean ONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS = false;

	private boolean USE_POWERSET_OF_FOUND_ORDERS = true;
	private boolean WITH_MERGE_ITEMSETS_IN_FOUND_ORDERS = false;

	private double maxErrorRateForIntermediateCheck = 0.5;
	private double maxTooManyMatchesForIntermediateCheck = 4.0;
	private boolean doIntermediateCheckWhileAnalyzingPatterns = true;

	private PerformanceMeasurements performanceMeasurements = new PerformanceMeasurements();

	private LinkedList<RelationshipConstraintPartner> relationshipConstraintPartners = new LinkedList<RelationshipConstraintPartner>();

	/**
	 * 
	 * @return
	 */
	public EpqlContext cloneButKeepTraces() {
		EpqlContext clone = new EpqlContext();

		PML pml = new PML();
		pml.getPatterns().addAll(getPatternMatchingLanguage().getPatterns());
		clone.setPatternMatchingLanguage(pml);
		clone.getFilteredPatterns().addAll(getFilteredPatterns());
		clone.setMaxFilteredPatternsFromSimilarityCluster(getMaxFilteredPatternsFromSimilarityCluster());

		clone.setPositiveTraces(getPositiveTraces());
		clone.setNegativeTraces(getNegativeTraces());
		clone.getRelationshipConstraintPartners().addAll(getRelationshipConstraintPartners());

		clone.getPerformanceMeasurements().numOfUsedTraces = getPerformanceMeasurements().numOfUsedTraces;
		clone.getPerformanceMeasurements().timeForConstraintLearner = getPerformanceMeasurements().timeForConstraintLearner;
		clone.getPerformanceMeasurements().timeForOrderLearner = getPerformanceMeasurements().timeForOrderLearner;
		clone.getPerformanceMeasurements().timeForRelevantEventInstancesLearner = getPerformanceMeasurements().timeForRelevantEventInstancesLearner;

		clone.setFilePrefix(getFilePrefix() + "");
		clone.setONLY_TAKE_LONGEST_FOUND_SEQUENCES(isONLY_TAKE_LONGEST_FOUND_SEQUENCES());
		clone.setONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE(isONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE());
		clone.setONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES(isONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES());
		clone.setWITH_CHAIN_MERGING(isWITH_CHAIN_MERGING());
		clone.setWITH_CONSTANT_INEQUALITY_CONSTRAINTS(isWITH_CONSTANT_INEQUALITY_CONSTRAINTS());
		clone.setWITH_KEEP_ALL_CREATED_EVENT_INSTANCES(isWITH_KEEP_ALL_CREATED_EVENT_INSTANCES());
		clone.setWITH_LOW_INFORMATION_VALUE_REDUCTION(isWITH_LOW_INFORMATION_VALUE_REDUCTION());
		clone.setWITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET(isWITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET());
		clone.setWITH_SUPERSET_REDUCTION_OPTIMIZATION(isWITH_SUPERSET_REDUCTION_OPTIMIZATION());
		clone.setSequentialMiningAlgorithm(getSequentialMiningAlgorithm());
		clone.setMaximumPositiveTestTraces(getMaximumPositiveTestTraces());
		clone.setWITH_MERGE_ITEMSETS_IN_FOUND_ORDERS(isWITH_MERGE_ITEMSETS_IN_FOUND_ORDERS());
		clone.setONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS(isONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS());

		clone.setDoIntermediateCheckWhileAnalyzingPatterns(isDoIntermediateCheckWhileAnalyzingPatterns());
		clone.setMaxErrorRateForIntermediateCheck(getMaxErrorRateForIntermediateCheck());
		clone.setMaxTooManyMatchesForIntermediateCheck(getMaxTooManyMatchesForIntermediateCheck());
		return clone;
	}

	/**
	 * @return the maxFilteredPatterns
	 */
	public int getMaxFilteredPatternsFromSimilarityCluster() {
		return maxFilteredPatternsFromSimliarityCluster;
	}

	/**
	 * @param maxFilteredPatterns
	 *            the maxFilteredPatterns to set
	 */
	public void setMaxFilteredPatternsFromSimilarityCluster(int maxFilteredPatterns) {
		this.maxFilteredPatternsFromSimliarityCluster = maxFilteredPatterns;
	}

	/**
	 * @return the filteredPatterns
	 */
	public LinkedList<Pattern> getFilteredPatterns() {
		return filteredPatterns;
	}

	/**
	 * @return the maxErrorRateForIntermediateCheck
	 */
	public double getMaxErrorRateForIntermediateCheck() {
		return maxErrorRateForIntermediateCheck;
	}

	/**
	 * @param maxErrorRateForIntermediateCheck
	 *            the maxErrorRateForIntermediateCheck to set
	 */
	public void setMaxErrorRateForIntermediateCheck(double maxErrorRateForIntermediateCheck) {
		this.maxErrorRateForIntermediateCheck = maxErrorRateForIntermediateCheck;
	}

	/**
	 * @return the maxTooManyMatchesForIntermediateCheck
	 */
	public double getMaxTooManyMatchesForIntermediateCheck() {
		return maxTooManyMatchesForIntermediateCheck;
	}

	/**
	 * @param maxTooManyMatchesForIntermediateCheck
	 *            the maxTooManyMatchesForIntermediateCheck to set
	 */
	public void setMaxTooManyMatchesForIntermediateCheck(double maxTooManyMatchesForIntermediateCheck) {
		this.maxTooManyMatchesForIntermediateCheck = maxTooManyMatchesForIntermediateCheck;
	}

	/**
	 * @return the doIntermediateCheckWhileAnalyzingPatterns
	 */
	public boolean isDoIntermediateCheckWhileAnalyzingPatterns() {
		return doIntermediateCheckWhileAnalyzingPatterns;
	}

	/**
	 * @param doIntermediateCheckWhileAnalyzingPatterns
	 *            the doIntermediateCheckWhileAnalyzingPatterns to set
	 */
	public void setDoIntermediateCheckWhileAnalyzingPatterns(boolean doIntermediateCheckWhileAnalyzingPatterns) {
		this.doIntermediateCheckWhileAnalyzingPatterns = doIntermediateCheckWhileAnalyzingPatterns;
	}

	/**
	 * @return the performanceMeasurements
	 */
	public PerformanceMeasurements getPerformanceMeasurements() {
		return performanceMeasurements;
	}

	/**
	 * @param performanceMeasurements
	 *            the performanceMeasurements to set
	 */
	public void setPerformanceMeasurements(PerformanceMeasurements performanceMeasurements) {
		this.performanceMeasurements = performanceMeasurements;
	}

	/**
	 * @return the uSE_POWERSET_OF_FOUND_ORDERS
	 */
	public boolean isUSE_POWERSET_OF_FOUND_ORDERS() {
		return USE_POWERSET_OF_FOUND_ORDERS;
	}

	/**
	 * @param uSE_POWERSET_OF_FOUND_ORDERS
	 *            the uSE_POWERSET_OF_FOUND_ORDERS to set
	 */
	public void setUSE_POWERSET_OF_FOUND_ORDERS(boolean uSE_POWERSET_OF_FOUND_ORDERS) {
		USE_POWERSET_OF_FOUND_ORDERS = uSE_POWERSET_OF_FOUND_ORDERS;
	}

	/**
	 * @return the wITH_MERGE_ITEMSETS_IN_FOUND_ORDERS
	 */
	public boolean isWITH_MERGE_ITEMSETS_IN_FOUND_ORDERS() {
		return WITH_MERGE_ITEMSETS_IN_FOUND_ORDERS;
	}

	/**
	 * @param wITH_MERGE_ITEMSETS_IN_FOUND_ORDERS
	 *            the wITH_MERGE_ITEMSETS_IN_FOUND_ORDERS to set
	 */
	public void setWITH_MERGE_ITEMSETS_IN_FOUND_ORDERS(boolean wITH_MERGE_ITEMSETS_IN_FOUND_ORDERS) {
		WITH_MERGE_ITEMSETS_IN_FOUND_ORDERS = wITH_MERGE_ITEMSETS_IN_FOUND_ORDERS;
	}

	/**
	 * @return the wITH_KEEP_ALL_CREATED_EVENT_INSTANCES
	 */
	public boolean isWITH_KEEP_ALL_CREATED_EVENT_INSTANCES() {
		return WITH_KEEP_ALL_CREATED_EVENT_INSTANCES;
	}

	/**
	 * @param wITH_KEEP_ALL_CREATED_EVENT_INSTANCES
	 *            the wITH_KEEP_ALL_CREATED_EVENT_INSTANCES to set
	 */
	public void setWITH_KEEP_ALL_CREATED_EVENT_INSTANCES(boolean wITH_KEEP_ALL_CREATED_EVENT_INSTANCES) {
		WITH_KEEP_ALL_CREATED_EVENT_INSTANCES = wITH_KEEP_ALL_CREATED_EVENT_INSTANCES;
	}

	/**
	 * @return the oNLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS
	 */
	public boolean isONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS() {
		return ONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS;
	}

	/**
	 * @param oNLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS
	 *            the oNLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS to set
	 */
	public void setONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS(boolean oNLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS) {
		ONLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS = oNLY_USE_USER_GIVEN_RELATIONSHIP_PARTNERS;
	}

	/**
	 * @return the relationshipConstraintPartners
	 */
	public LinkedList<RelationshipConstraintPartner> getRelationshipConstraintPartners() {
		return relationshipConstraintPartners;
	}

	public String getFilePrefix() {
		return filePrefix;
	}

	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	public Integer getMaximumPositiveTestTraces() {
		return maximumPositiveTestTraces;
	}

	public void setMaximumPositiveTestTraces(Integer maximumPositiveTestTraces) {
		this.maximumPositiveTestTraces = maximumPositiveTestTraces;
	}

	public void setONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES(boolean oNLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES) {
		ONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES = oNLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES;
	}

	public void setWITH_CONSTANT_INEQUALITY_CONSTRAINTS(boolean wITH_CONSTANT_INEQUALITY_CONSTRAINTS) {
		WITH_CONSTANT_INEQUALITY_CONSTRAINTS = wITH_CONSTANT_INEQUALITY_CONSTRAINTS;
	}

	public void setWITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET(boolean wITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET) {
		WITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET = wITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET;
	}

	public void setWITH_SUPERSET_REDUCTION_OPTIMIZATION(boolean wITH_SUPERSET_REDUCTION_OPTIMIZATION) {
		WITH_SUPERSET_REDUCTION_OPTIMIZATION = wITH_SUPERSET_REDUCTION_OPTIMIZATION;
	}

	public void setWITH_LOW_INFORMATION_VALUE_REDUCTION(boolean wITH_LOW_INFORMATION_VALUE_REDUCTION) {
		WITH_LOW_INFORMATION_VALUE_REDUCTION = wITH_LOW_INFORMATION_VALUE_REDUCTION;
	}

	public void setONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE(boolean oNLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE) {
		ONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE = oNLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE;
	}

	public void setONLY_TAKE_LONGEST_FOUND_SEQUENCES(boolean oNLY_TAKE_LONGEST_FOUND_SEQUENCES) {
		ONLY_TAKE_LONGEST_FOUND_SEQUENCES = oNLY_TAKE_LONGEST_FOUND_SEQUENCES;
	}

	public boolean isONLY_TAKE_LONGEST_FOUND_SEQUENCES() {
		return ONLY_TAKE_LONGEST_FOUND_SEQUENCES;
	}

	public boolean isONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES() {
		return ONLY_USE_STRINGS_FOR_RELEVANT_EVENT_INSTANCES;
	}

	// which event instances are relevant for the patterns?
	private LinkedList<EventInstance> relevantEventInstances = new LinkedList<EventInstance>();

	// how often should each event instance appear in the pattern?
	private HashMap<EventInstance, Integer> eventInstanceOccurrenceCount = new HashMap<EventInstance, Integer>();

	// this is used to save the matched event instance order per pattern for the
	// next learner, it should not be part of a pattern, because it is just used
	// for one step
	private HashMap<Pattern, MatchedEventInstanceOrder> matchedEventOrders = new HashMap<Pattern, MatchedEventInstanceOrder>();

	public boolean isONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE() {
		return ONLY_LOOK_FOR_CHAINS_OF_SAME_EVENT_TYPE;
	}

	// this map holds the information, which event is used for each pattern for
	// each orderElement. this is needed, because constraints can possibly be
	// based upon different events for one order element and we should not allow
	// this in one pattern
	private HashMap<Pattern, HashMap<OrderElement, LinkedList<GenericEvent>>> constraintsOrderElementBasedUponEvents = new HashMap<Pattern, HashMap<OrderElement, LinkedList<GenericEvent>>>();

	public boolean isWITH_CONSTANT_INEQUALITY_CONSTRAINTS() {
		return WITH_CONSTANT_INEQUALITY_CONSTRAINTS;
	}

	public boolean isWITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET() {
		return WITH_REMOVE_INSTANCES_THAT_OCCUR_IN_EVERY_SET;
	}

	/**
	 * @return the relevantEventInstances
	 */
	public LinkedList<EventInstance> getRelevantEventInstances() {
		return relevantEventInstances;
	}

	/**
	 * @param relevantEventInstances
	 *            the relevantEventInstances to set
	 */
	public void setRelevantEventInstances(LinkedList<EventInstance> relevantEventInstances) {
		this.relevantEventInstances = relevantEventInstances;
	}

	/**
	 * @return the eventInstanceOccurrenceCount
	 */
	public HashMap<EventInstance, Integer> getEventInstanceOccurrenceCount() {
		return eventInstanceOccurrenceCount;
	}

	/**
	 * @param eventInstanceOccurrenceCount
	 *            the eventInstanceOccurrenceCount to set
	 */
	public void setEventInstanceOccurrenceCount(HashMap<EventInstance, Integer> eventInstanceOccurrenceCount) {
		this.eventInstanceOccurrenceCount = eventInstanceOccurrenceCount;
	}

	/**
	 * @return the equalityConstraintsOrderElementBasedUponEvent
	 */
	public HashMap<Pattern, HashMap<OrderElement, LinkedList<GenericEvent>>> getConstraintsOrderElementBasedUponEvents() {
		return constraintsOrderElementBasedUponEvents;
	}

	/**
	 * @return the matchedEventOrders
	 */
	public HashMap<Pattern, MatchedEventInstanceOrder> getMatchedEventOrders() {
		return matchedEventOrders;
	}

	/**
	 * @param matchedEventOrders
	 *            the matchedEventOrders to set
	 */
	public void setMatchedEventOrders(HashMap<Pattern, MatchedEventInstanceOrder> matchedEventOrders) {
		this.matchedEventOrders = matchedEventOrders;
	}

	/**
	 * @return the wITH_CHAIN_MERGING
	 */
	public boolean isWITH_CHAIN_MERGING() {
		return WITH_CHAIN_MERGING;
	}

	/**
	 * @param wITH_CHAIN_MERGING
	 *            the wITH_CHAIN_MERGING to set
	 */
	public void setWITH_CHAIN_MERGING(boolean wITH_CHAIN_MERGING) {
		WITH_CHAIN_MERGING = wITH_CHAIN_MERGING;
	}

	/**
	 * @return the isWITH_LOW_INFORMATION_VALUE_REDUCTION
	 */
	public boolean isWITH_LOW_INFORMATION_VALUE_REDUCTION() {
		return WITH_LOW_INFORMATION_VALUE_REDUCTION;
	}

	/**
	 * @return the wITH_SUPERSET_REDUCTION_OPTIMIZATION
	 */
	public boolean isWITH_SUPERSET_REDUCTION_OPTIMIZATION() {
		return WITH_SUPERSET_REDUCTION_OPTIMIZATION;
	}

	/**
	 * @return the sequentialMiningAlgorithm
	 */
	public ESequentialMiningAlgorithm getSequentialMiningAlgorithm() {
		return sequentialMiningAlgorithm;
	}

	/**
	 * @param sequentialMiningAlgorithm
	 *            the sequentialMiningAlgorithm to set
	 */
	public void setSequentialMiningAlgorithm(ESequentialMiningAlgorithm sequentialMiningAlgorithm) {
		this.sequentialMiningAlgorithm = sequentialMiningAlgorithm;
	}

	/**
	 * @return the negativeTraces
	 */
	public LinkedList<HistoricalTrace> getNegativeTraces() {
		return negativeTraces;
	}

	/**
	 * @param negativeTraces
	 *            the negativeTraces to set
	 */
	public void setNegativeTraces(LinkedList<HistoricalTrace> negativeTraces) {
		this.negativeTraces = negativeTraces;
	}

	/**
	 * @return the useMinSupportVersion
	 */
	public boolean isUseMinSupportVersion() {
		return useMinSupportVersion;
	}

	/**
	 * @param useMinSupportVersion
	 *            the useMinSupportVersion to set
	 */
	public void setUseMinSupportVersion(boolean useMinSupportVersion) {
		this.useMinSupportVersion = useMinSupportVersion;
	}

	/**
	 * @return the traces
	 */
	public LinkedList<HistoricalTrace> getPositiveTraces() {
		return positiveTraces;
	}

	/**
	 * @param traces
	 *            the traces to set
	 */
	public void setPositiveTraces(LinkedList<HistoricalTrace> traces) {
		this.positiveTraces = traces;
	}

	/**
	 * @return the patternMatchingLanguage
	 */
	public PML getPatternMatchingLanguage() {
		return patternMatchingLanguage;
	}

	/**
	 * @param patternMatchingLanguage
	 *            the patternMatchingLanguage to set
	 */
	public void setPatternMatchingLanguage(PML patternMatchingLanguage) {
		this.patternMatchingLanguage = patternMatchingLanguage;
	}

}
