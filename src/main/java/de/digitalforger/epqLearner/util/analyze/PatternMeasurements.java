package de.digitalforger.epqLearner.util.analyze;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;

public class PatternMeasurements {
	private static Logger logger = Logger.getLogger(PatternMeasurements.class.getName());

	/**
	 * calculates the structual simplicity of the given pattern
	 * 
	 * @param p
	 *            the pattern that should be analyzed
	 * @return a value between 0 and 1 stating the structual simplicity of the
	 *         given pattern
	 */
	public static double structualSimplicity(Pattern p) {
		double ret = 0;

		if (p.getEventOrder() == null) {
			logger.warning("Got no order to work with");
			return ret;
		}

		int numConstraints = p.getPropertyConstraints().size() + p.getRelationConstraints().size();
		int numEventVariables = p.getEventOrder().size();

		ret = 1.0 / (1.0 + numConstraints + numEventVariables);

		return ret;
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static Set<RelationConstraint> symmetricalDifferenceRel(final Set<RelationConstraint> s1, final Set<RelationConstraint> s2) {
	    Set<RelationConstraint> symmetricDiff = new HashSet<RelationConstraint>(s1);
	    symmetricDiff.addAll(s2);
	    Set<RelationConstraint> tmp = new HashSet<RelationConstraint>(s1);

	    //tmp.retainAll(s2);
	    Iterator<RelationConstraint> iterator = tmp.iterator();
	    while(iterator.hasNext()) {
	    	RelationConstraint next = iterator.next();
	    	
	    	Iterator<RelationConstraint> iterator2 = s2.iterator();
	    	boolean isInOtherSet = false;
	    	while(iterator2.hasNext()) {
	    		RelationConstraint next2 = iterator2.next();
				if(next.equalsOtherPropertyConstraint(next2)) {
	    			isInOtherSet = true;
	    			break;
	    		}
	    	}
	    	if(!isInOtherSet) {
	    		iterator.remove();
	    	}
	    	
	    }
	    
	    // symmetricDiff.removeAll(tmp);
	    Iterator<RelationConstraint> it1 = symmetricDiff.iterator();
	    while(it1.hasNext()) {
	    	RelationConstraint a = it1.next();
	    	   Iterator<RelationConstraint> it2 = tmp.iterator();
	    	   while(it2.hasNext()) {
	    		   RelationConstraint b = it2.next();
	    		   if(a.equalsOtherPropertyConstraint(b)) {
	    			   it1.remove();
	    			   break;
	    		   }
	    	   }
	    }
	    
	    return symmetricDiff;
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static Set<PropertyConstraint> symmetricalDifferenceProp(final Set<PropertyConstraint> s1, final Set<PropertyConstraint> s2) {
	    Set<PropertyConstraint> symmetricDiff = new HashSet<PropertyConstraint>(s1);
	    symmetricDiff.addAll(s2);
	    Set<PropertyConstraint> tmp = new HashSet<PropertyConstraint>(s1);

	    //tmp.retainAll(s2);
	    Iterator<PropertyConstraint> iterator = tmp.iterator();
	    while(iterator.hasNext()) {
	    	PropertyConstraint next = iterator.next();
	    	
	    	Iterator<PropertyConstraint> iterator2 = s2.iterator();
	    	boolean isInOtherSet = false;
	    	while(iterator2.hasNext()) {
	    		PropertyConstraint next2 = iterator2.next();
				if(next.equalsOtherPropertyConstraint(next2)) {
	    			isInOtherSet = true;
	    			break;
	    		}
	    	}
	    	if(!isInOtherSet) {
	    		iterator.remove();
	    	}
	    	
	    }
	    
	    // symmetricDiff.removeAll(tmp);
	    Iterator<PropertyConstraint> it1 = symmetricDiff.iterator();
	    while(it1.hasNext()) {
	    	PropertyConstraint a = it1.next();
	    	   Iterator<PropertyConstraint> it2 = tmp.iterator();
	    	   while(it2.hasNext()) {
	    		   PropertyConstraint b = it2.next();
	    		   if(a.equalsOtherPropertyConstraint(b)) {
	    			   it1.remove();
	    			   break;
	    		   }
	    	   }
	    }
	    
	    return symmetricDiff;
	}
	
	/**
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static double structualSimilarity(Pattern p1, Pattern p2) {
		double ret = 0;
		
		HashSet<PropertyConstraint> propertyConstraints1 = new HashSet<PropertyConstraint>(p1.getPropertyConstraints());
		HashSet<RelationConstraint> relationConstraints1 = new HashSet<RelationConstraint>(p1.getRelationConstraints());
		HashSet<PropertyConstraint> propertyConstraints2 = new HashSet<PropertyConstraint>(p2.getPropertyConstraints());
		HashSet<RelationConstraint> relationConstraints2 = new HashSet<RelationConstraint>(p2.getRelationConstraints());
		
		Set<PropertyConstraint> differentPropertyConstraints = symmetricalDifferenceProp(propertyConstraints1, propertyConstraints2);
		Set<RelationConstraint> differentRelationConstraints = symmetricalDifferenceRel(relationConstraints1,relationConstraints2);		
		
		ret = 1.0 / (1.0 + differentPropertyConstraints.size() + differentRelationConstraints.size());
		return ret;
	}
}
