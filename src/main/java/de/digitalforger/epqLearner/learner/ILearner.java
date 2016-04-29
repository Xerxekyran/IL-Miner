package de.digitalforger.epqLearner.learner;

import java.util.LinkedList;

import de.digitalforger.epqLearner.EpqlContext;
import de.digitalforger.epqLearner.PML.Pattern;

public interface ILearner {
	/**
	 * 
	 * @param ctx
	 */
	public void performOneStep(final EpqlContext ctx);
}
