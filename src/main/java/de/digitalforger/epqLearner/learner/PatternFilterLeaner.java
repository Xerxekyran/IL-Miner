package de.digitalforger.epqLearner.learner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.PDistClusteringAlgorithm;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.util.analyze.PatternMeasurements;

public class PatternFilterLeaner implements ILearner {

	private static Logger logger = Logger.getLogger(PatternFilterLeaner.class.getName());

	@Override
	public void performOneStep(EpqlContext ctx) {
		logger.log(Level.FINE, "performing one step of the PatternFilterLeaner");

		LinkedList<Pattern> allPatterns = ctx.getPatternMatchingLanguage().getPatterns();
		HashMap<Pattern, Double> structualSimplicityOfPattern = new HashMap<Pattern, Double>();
		HashMap<EventInstanceOrder, LinkedList<Pattern>> patternsByOrder = new HashMap<EventInstanceOrder, LinkedList<Pattern>>();

		LinkedList<Pattern> filteredPatterns = new LinkedList<Pattern>();

		if (allPatterns != null) {
			logger.info("Analyzing found patterns [" + allPatterns.size() + " patterns exist]");

			for (Pattern p : allPatterns) {
				// categorize patterns by the order they are based upon
				LinkedList<Pattern> listOfPatternsByOrder = patternsByOrder.get(p.getEventOrder());
				if (listOfPatternsByOrder == null) {
					listOfPatternsByOrder = new LinkedList<Pattern>();
				}
				listOfPatternsByOrder.add(p);
				patternsByOrder.put(p.getEventOrder(), listOfPatternsByOrder);

				// calculate structual simplicity for each pattern
				double structualSimplicity = PatternMeasurements.structualSimplicity(p);
				structualSimplicityOfPattern.put(p, structualSimplicity);
			}
		}

//		for (EventInstanceOrder o : patternsByOrder.keySet()) {
//			System.out.println(o);
//			System.out.println(patternsByOrder.get(o).size());
//			System.out.println("--------------");
//		}

		// select some by distance factor
		LinkedList<Pattern> selectedPatternsBySimilarityDistances = hierarchicalClusteringBySimilarity(allPatterns, ctx.getMaxFilteredPatternsFromSimilarityCluster());
		filteredPatterns.addAll(selectedPatternsBySimilarityDistances);
		
		// select some by simplicity (simple / middle / complex)
		LinkedList<Pattern> selectedPatternsBySimplicity = selectBySimplicity(structualSimplicityOfPattern);
		for(Pattern p : selectedPatternsBySimplicity) {
			if(!filteredPatterns.contains(p)) {
				filteredPatterns.add(p);
			}
		}		
		
		logger.info("PatternFilterLearner reduced set of patterns to " + filteredPatterns.size() + " (context setting) to consider further:");
		for(Pattern p: filteredPatterns) {
			logger.info(p.toString());
		}
		
		
		ctx.getFilteredPatterns().addAll(filteredPatterns);
	}

	/**
	 * 
	 * @param structualSimplicityOfPattern
	 * @return
	 */
	private LinkedList<Pattern> selectBySimplicity(HashMap<Pattern, Double> structualSimplicityOfPattern) {
		LinkedList<Pattern> ret = new LinkedList<Pattern>();
		Pattern minPattern = null;
		Pattern maxPattern = null;

		// are there even enough pattern to select some from?
		if (structualSimplicityOfPattern.keySet().size() < 3) {
			ret.addAll(structualSimplicityOfPattern.keySet());
		} else {
			for (Pattern p : structualSimplicityOfPattern.keySet()) {
				if (minPattern == null) {
					minPattern = p;
					maxPattern = p;
				} else {
					Double simplicity = structualSimplicityOfPattern.get(p);

					if (simplicity < structualSimplicityOfPattern.get(minPattern)) {
						minPattern = p;
					}

					if (simplicity > structualSimplicityOfPattern.get(maxPattern)) {
						maxPattern = p;
					}
				}
			}
		}

		ret.add(minPattern);
		ret.add(maxPattern);
		return ret;
	}

	/**
	 * 
	 * @param allPatterns
	 * @return
	 */
	private double[][] calculateDistances(LinkedList<Pattern> allPatterns) {
		LinkedList<Double> distances = new LinkedList<Double>();

		for (int i = 0; i < allPatterns.size() - 1; i++) {
			for (int y = i + 1; y < allPatterns.size(); y++) {
				distances.add(PatternMeasurements.structualSimilarity(allPatterns.get(i), allPatterns.get(y)));
			}
		}

		double[][] ret = new double[1][distances.size()];
		for (int i = 0; i < distances.size(); i++) {
			ret[0][i] = distances.get(i);
		}
		return ret;
	}

	/**
	 * 
	 * @param allPatterns
	 * @return
	 */
	private LinkedList<Pattern> hierarchicalClusteringBySimilarity(LinkedList<Pattern> allPatterns, int maxPatterns) {
		// if there are not enough patterns to select some
		if (maxPatterns > allPatterns.size()) {
			return new LinkedList<Pattern>(allPatterns);
		}

		HashMap<String, Pattern> patternNameToPattern = new HashMap<String, Pattern>();

		String[] names = new String[allPatterns.size()];
		for (int i = 0; i < allPatterns.size(); i++) {
			Pattern p = allPatterns.get(i);
			String pName = "p_" + i;
			patternNameToPattern.put(pName, p);
			names[i] = pName;
		}

		double[][] distances = calculateDistances(allPatterns);

		ClusteringAlgorithm alg = new PDistClusteringAlgorithm();
		Cluster cluster = alg.performClustering(distances, names, new AverageLinkageStrategy());

		List<Cluster> selectedClusters = new ArrayList<Cluster>();
		selectedClusters.add(cluster);

		for (int i = 0; i < selectedClusters.size() && selectedClusters.size() < maxPatterns; i++) {
			Cluster next = selectedClusters.get(i);

			// only go deeper if we are not at a leaf
			if (!next.isLeaf()) {
				selectedClusters.addAll(next.getChildren());
				selectedClusters.remove(i);
				i--;
			}
		}

		LinkedList<Pattern> ret = new LinkedList<Pattern>();

		// now select one candidate of each cluster
		for (Cluster c : selectedClusters) {
			List<Cluster> leafs = getAllLeafNodesOfCluster(c);
			// different strategies on how to pick the candidate?
			// for now pick the first one
			ret.add(patternNameToPattern.get(leafs.get(0).getName()));
		}

		// Visualisation of the Dendrogram
		// JFrame j = new JFrame();
		// DendrogramPanel dp = new DendrogramPanel();
		// dp.setModel(cluster);

		// j.add(dp);
		// j.setVisible(true);

		return ret;
	}

	/**
	 * 
	 * @param c
	 * @return
	 */
	private List<Cluster> getAllLeafNodesOfCluster(Cluster c) {
		LinkedList<Cluster> ret = new LinkedList<Cluster>();
		ret.add(c);

		boolean allLeafs = false;

		while (!allLeafs) {
			allLeafs = true;

			for (int i = 0; i < ret.size(); i++) {
				Cluster next = ret.get(i);

				if (!next.isLeaf()) {
					allLeafs = false;
					ret.addAll(next.getChildren());
					ret.remove(i);
					i--;
				}
			}
		}
		return ret;
	}
}
