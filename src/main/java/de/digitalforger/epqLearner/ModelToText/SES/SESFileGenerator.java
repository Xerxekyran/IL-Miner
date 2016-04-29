package de.digitalforger.epqLearner.ModelToText.SES;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.digitalforger.epqLearner.ModelToText.IFileGenerator;
import de.digitalforger.epqLearner.PML.Pattern;
import de.digitalforger.epqLearner.PML.constraints.PropertyConstraint;
import de.digitalforger.epqLearner.PML.constraints.RelationConstraint;
import de.digitalforger.epqLearner.PML.constraints.order.EventInstanceOrder;
import de.digitalforger.epqLearner.PML.constraints.order.OrderElement;
import de.digitalforger.epqLearner.event.EValueType;

/**
 * class to generate SES comliant files from learned patterns
 * 
 * @author george
 * 
 */
public class SESFileGenerator implements IFileGenerator {

	private static Logger logger = Logger.getLogger(SESFileGenerator.class.getName());

	@Override
	public String getPatternAsString(Pattern pattern) {
		// FROM stream
		// MATCH ({nyse v1, nyse w1+}, {onco v2})
		// WHERE
		// {
		// v1.A = 3,
		// w1.B = 'drei',
		// v2.C = 4.6,
		// v1.X > v2.Y,
		// prev(w1.C) <= w1.C
		// }
		// WITHIN 10
		// PARTITION BY A, B

		StringBuilder str = new StringBuilder();

		str.append("FROM testFile");
		str.append(System.lineSeparator());

		str.append("MATCH");
		str.append(System.lineSeparator());
		str.append("(");
		str.append(System.lineSeparator());

		// pattern variables and order
		str.append(getOrderAsString(pattern));
		str.append(System.lineSeparator());

		str.append(")");
		str.append(System.lineSeparator());

		str.append("WHERE");
		str.append(System.lineSeparator());

		str.append("{");
		str.append(System.lineSeparator());

		// constraints
		String propertyConstraintsAsString = getPropertyConstraintsAsString(pattern);
		String relationConstraintsAsString = getRelationConstraintsAsString(pattern);

		// remove the last comma if needed
		if (propertyConstraintsAsString.length() > 1 && relationConstraintsAsString.length() < 1) {
			propertyConstraintsAsString = propertyConstraintsAsString.substring(0, propertyConstraintsAsString.length() - 2);
		}

		str.append(propertyConstraintsAsString);
		str.append(relationConstraintsAsString);
		str.append(System.lineSeparator());

		str.append("}");
		str.append(System.lineSeparator());

		str.append("WITHIN ");
		str.append("" + pattern.getTimeWindow());
		str.append(System.lineSeparator());

		return str.toString();
	}

	@Override
	public void writePatternToFile(String fileName, Pattern pattern) {
		String patternAsString = getPatternAsString(pattern);

		BufferedWriter writer = null;

		try {
			// FROM stream
			// MATCH ({nyse v1, nyse w1+}, {onco v2})
			// WHERE
			// {
			// v1.A = 3,
			// w1.B = 'drei',
			// v2.C = 4.6,
			// v1.X > v2.Y,
			// prev(w1.C) <= w1.C
			// }
			// WITHIN 10
			// PARTITION BY A, B

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"));
			writer.write(patternAsString);

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not write pattern to file: " + e.getStackTrace().toString());
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
			}
		}

	}

	/**
	 * 
	 * @param pattern
	 * @return
	 */
	private String getRelationConstraintsAsString(Pattern pattern) {
		StringBuilder str = new StringBuilder();

		for (RelationConstraint relConstr : pattern.getRelationConstraints()) {
			// v_0.A < w_1.B
			// w_1.B < w_2.B

			// from order element
			str.append("	");
			str.append(nameInPattern(relConstr.getFromOrderElement()));

			// from attribute
			str.append(".");
			str.append(relConstr.getFromOrderElementAttributeName());

			// operator
			switch (relConstr.getConstraintOperator()) {
			case Equal:
				str.append(" = ");
				break;
			case GreaterThan:
				str.append(" > ");
				break;
			case GreaterThanEqual:
				str.append(" >= ");
				break;
			case LessThan:
				str.append(" < ");
				break;
			case LessThanEqual:
				str.append(" <= ");
				break;
			case Unequal:
				str.append(" != ");
				break;
			default:
				str.append(" ??? ");
				logger.warning("This constraint operator is not yet implemented: " + relConstr.getConstraintOperator());
			}

			// from order element
			str.append(nameInPattern(relConstr.getToOrderElement()));

			// from attribute
			str.append(".");
			str.append(relConstr.getToOrderElementAttributeName());
			str.append(",\n");
		}

		// remove last comma
		if (str.length() > 1) {
			str.delete(str.length() - 2, str.length());
		}

		return str.toString();
	}

	/**
	 * 
	 * @param pattern
	 * @return
	 */
	private String getPropertyConstraintsAsString(Pattern pattern) {
		StringBuilder str = new StringBuilder();

		for (PropertyConstraint propConstr : pattern.getPropertyConstraints()) {
			// v_1.A = 3,
			// w_1.B = 'drei'

			// order element
			str.append("	");
			str.append(nameInPattern(propConstr.getBelongsToOrderElement()));

			// attribute
			str.append(".");
			str.append(propConstr.getAttributeName());

			// operator
			switch (propConstr.getConstraintOperator()) {
			case Equal:
				str.append(" = ");
				break;
			case GreaterThan:
				str.append(" > ");
				break;
			case GreaterThanEqual:
				str.append(" >= ");
				break;
			case LessThan:
				str.append(" < ");
				break;
			case LessThanEqual:
				str.append(" <= ");
				break;
			case Unequal:
				str.append(" != ");
				break;
			default:
				str.append(" ??? ");
				logger.warning("This constraint operator is not yet implemented: " + propConstr.getConstraintOperator());
			}

			// constraint value
			if (propConstr.getConstantConstraintValue().getType().equals(EValueType.STRING)) {
				str.append("'" + propConstr.getConstantConstraintValue().toString() + "'");
			} else {
				str.append(propConstr.getConstantConstraintValue().toString());
			}
			str.append(",\n");
		}

		return str.toString();
	}

	/**
	 * 
	 * @param writer
	 * @param pattern
	 */
	private String getOrderAsString(Pattern pattern) {
		// {nyse n_1}, {onco o_2}
		EventInstanceOrder eventOrder = pattern.getEventOrder();

		StringBuilder str = new StringBuilder("	");

		for (int i = 0; i < eventOrder.size(); i++) {
			str.append("{");

			OrderElement orderElement = eventOrder.get(i);
			String eventTypeName = orderElement.getEventInstance().getEventTypeName();
			str.append(eventTypeName);
			str.append(" " + nameInPattern(orderElement));

			str.append("}, ");
		}

		// truncate last comma
		if (str.length() > 0) {
			str.delete(str.length() - 2, str.length());
		}

		return str.toString();
	}

	/**
	 * 
	 * @param oe
	 * @return
	 */
	private String nameInPattern(OrderElement oe) {
		return "" + oe.getEventInstance().getEventTypeName().substring(0, 1).toLowerCase() + "_" + oe.getOrderElementIndex();
	}

	@Override
	public String getFileEnding() {
		return "ses";
	}

}
