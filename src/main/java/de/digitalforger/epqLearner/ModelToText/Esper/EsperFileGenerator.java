package de.digitalforger.epqLearner.ModelToText.Esper;

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
 * Generates the string version of a given pattern in the concrete language
 * Esper
 * 
 * @author george
 *
 */
public class EsperFileGenerator implements IFileGenerator {
	private static Logger logger = Logger.getLogger(EsperFileGenerator.class.getName());

	@Override
	public String getPatternAsString(Pattern pattern) {

		EventInstanceOrder eventOrder = pattern.getEventOrder();

		StringBuilder str = new StringBuilder();

		str.append("SELECT * FROM pattern [" + System.lineSeparator());

		for (int i = 0; i < eventOrder.size(); i++) {
			OrderElement orderElement = eventOrder.get(i);
			str.append(getOrderElementAsString(orderElement, pattern, (i == eventOrder.size() - 1)));
			str.append(" -> ");

			// more than one element?
			if (i == 0 && eventOrder.size() > 1) {
				str.append("(");
			}
		}

		// remove last not needed arrow
		if (eventOrder.size() > 0) {
			str.delete(str.length() - 4, str.length());
		}

		if (eventOrder.size() > 1) {
			str.append(") ");
		}

		str.append("where timer:within(");
		str.append(((pattern.getTimeWindow() / 1000.0) + 1.0 / 1000.0));
		str.append(")");
		str.append(System.lineSeparator() + "]");

		return str.toString();
	}

	/**
	 * 
	 * @param writer
	 * @param pattern
	 */
	private StringBuilder getOrderElementAsString(OrderElement orderElement, Pattern pattern, boolean isLast) {
		// every a=EventType() -> every b=EventType
		StringBuilder str = new StringBuilder("every ");
		str.append(nameInPattern(orderElement));
		str.append("=");
		str.append(orderElement.getEventInstance().getEventTypeName());
		str.append("(");

		StringBuilder propertyConstraintsForOrderElementAsString = getPropertyConstraintsForOrderElementAsString(orderElement, pattern);
		StringBuilder relationshipsConstraintForOrderElementAsString = getRelationshipsConstraintForOrderElementAsString(orderElement, pattern);

		str.append(propertyConstraintsForOrderElementAsString);

		if (propertyConstraintsForOrderElementAsString.length() > 0 && relationshipsConstraintForOrderElementAsString.length() > 0) {
			str.append(", ");
		}

		str.append(relationshipsConstraintForOrderElementAsString);

		str.append(")");

		return str;
	}

	/**
	 * 
	 * @param orderElement
	 * @param pattern
	 * @return
	 */
	private StringBuilder getRelationshipsConstraintForOrderElementAsString(OrderElement orderElement, Pattern pattern) {
		StringBuilder str = new StringBuilder();

		for (RelationConstraint relConstr : pattern.getRelationConstraints()) {
			// if this constraint does not belon here
			if (relConstr.getFromOrderElement() != orderElement && relConstr.getToOrderElement() != orderElement) {
				continue;
			}

			// so we are part of the constraint, now check if the order is
			// alright (do not use constraints in order elements that refere to
			// order elements later in the order)
			if (relConstr.getFromOrderElement() == orderElement) {
				// we are from
				// from < to
				if (relConstr.getFromOrderElement().getOrderElementIndex() < relConstr.getToOrderElement().getOrderElementIndex()) {
					// we cant do it here
					continue;
				}
			}

			if (relConstr.getToOrderElement() == orderElement) {
				// we are to
				// to < from
				if (relConstr.getFromOrderElement().getOrderElementIndex() > relConstr.getToOrderElement().getOrderElementIndex()) {
					continue;
				}
			}

			// from attribute
			if (relConstr.getFromOrderElement() == orderElement) {
				str.append(relConstr.getFromOrderElementAttributeName());
			} else {
				str.append(nameInPattern(relConstr.getFromOrderElement()));
				str.append(".");
				str.append(relConstr.getFromOrderElementAttributeName());
			}

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

			if (relConstr.getToOrderElement() == orderElement) {
				str.append(relConstr.getToOrderElementAttributeName());
			} else {
				str.append(nameInPattern(relConstr.getToOrderElement()));
				str.append(".");
				str.append(relConstr.getToOrderElementAttributeName());
			}

			str.append(", ");
		}

		// remove last comma
		if (str.length() > 1) {
			str.delete(str.length() - 2, str.length());
		}

		return str;

	}

	/**
	 * 
	 * @param orderElement
	 * @param pattern
	 * @return
	 */
	private StringBuilder getPropertyConstraintsForOrderElementAsString(OrderElement orderElement, Pattern pattern) {
		StringBuilder str = new StringBuilder();

		for (PropertyConstraint propConstr : pattern.getPropertyConstraints()) {
			// only take property constraints that belong to the current order
			// element
			if (propConstr.getBelongsToOrderElement() != orderElement) {
				continue;
			}
			// v_1.A = 3,
			// w_1.B = 'drei'

			// property name
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
			str.append(", ");
		}

		// remove last comma
		if (str.length() > 0) {
			str.delete(str.length() - 2, str.length());
		}

		return str;
	}

	@Override
	public void writePatternToFile(String fileName, Pattern pattern) {
		String patternAsString = getPatternAsString(pattern);

		BufferedWriter writer = null;

		try {
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
	 * @param oe
	 * @return
	 */
	private StringBuilder nameInPattern(OrderElement oe) {
		StringBuilder ret = new StringBuilder();
		ret.append(oe.getEventInstance().getEventTypeName().substring(0, 1).toLowerCase());
		ret.append("_");
		ret.append(oe.getOrderElementIndex());
		return ret;
	}

	@Override
	public String getFileEnding() {
		return "esper";
	}
}
