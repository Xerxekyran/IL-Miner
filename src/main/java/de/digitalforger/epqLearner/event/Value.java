package de.digitalforger.epqLearner.event;

import java.io.Serializable;

/**
 * The value of an attribute
 * 
 * @author george
 * 
 */
public class Value implements Comparable<Value>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4863962929632314975L;

	private double doubleValue;
	private long longValue;
	private String stringValue;
	private EValueType type;

	/**
	 * 
	 * @param value
	 */
	public Value(long value) {
		setType(EValueType.LONG);
		setLongValue(value);
		setStringValue(null);
		setDoubleValue(0);
	}

	/**
	 * 
	 * @param value
	 */
	public Value(double value) {
		setType(EValueType.DOUBLE);
		setLongValue(0);
		setStringValue(null);
		setDoubleValue(value);
	}

	/**
	 * 
	 * @param value
	 */
	public Value(String value) {
		setType(EValueType.STRING);
		setLongValue(0);
		setStringValue(value);
		setDoubleValue(0);
	}

	/**
	 * 
	 * @param valFrom
	 */
	public Value(Value valFrom) {
		setType(valFrom.getType());
		setLongValue(valFrom.getLongValue());
		setStringValue(valFrom.getStringValue());
		setDoubleValue(valFrom.getDoubleValue());
	}

	/**
	 * @return the longValue
	 */
	public long getLongValue() {
		return longValue;
	}

	/**
	 * @param longValue
	 *            the longValue to set
	 */
	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}

	/**
	 * @return the stringValue
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * @param stringValue
	 *            the stringValue to set
	 */
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	/**
	 * @return the doubleValue
	 */
	public double getDoubleValue() {
		return doubleValue;
	}

	/**
	 * @param doubleValue
	 *            the doubleValue to set
	 */
	public void setDoubleValue(double doubleValue) {
		this.doubleValue = doubleValue;
	}

	/**
	 * @return the type
	 */
	public EValueType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(EValueType type) {
		this.type = type;
	}

	/**
	 * comparing two values. if they do not have the same ValueType, this
	 * methods throws an error.
	 * 
	 * @param other
	 *            the other value object to be compared to
	 * @return a value < 0 if this object has a less value, 0 if they are the
	 *         same or a value > 0 if this object has a greater value
	 * @throws Exception
	 */
	@Override
	public int compareTo(Value other) {
		if (other == null) {
			throw new NullPointerException();
		}

		if (!this.getType().equals(other.getType())) {
			throw new NumberFormatException("Trying to compare two Value objects of different types.");
		}

		int retVal = 0;

		switch (other.type) {
		case DOUBLE:
			double result = (getDoubleValue() - other.getDoubleValue());
			if (result < 0) {
				retVal = -1;
			} else if (result > 0) {
				retVal = 1;
			} else {
				retVal = 0;
			}
			break;
		case LONG:
			retVal = (int) (getLongValue() - other.getLongValue());
			break;
		case STRING:
			retVal = getStringValue().compareTo(other.getStringValue());
			break;
		}

		return retVal;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Value)) {
			return false;
		}

		Value other = (Value) obj;

		if (this.type != other.type) {
			return false;
		}

		switch (other.type) {
		case DOUBLE:
			if (this.doubleValue != other.doubleValue)
				return false;
			break;
		case LONG:
			if (this.longValue != other.longValue)
				return false;
			break;
		case STRING:
			if (this.stringValue == null) {
				if (other.stringValue != null) {
					return false;
				}
			} else if (!this.stringValue.equals(other.stringValue))
				return false;
			break;
		}

		return true;
	}

	@Override
	public String toString() {
		switch (type) {
		case LONG:
			return String.valueOf(longValue);
		case DOUBLE:
			return String.valueOf(doubleValue);
		case STRING:
			return String.valueOf(stringValue);
		default:
			assert false : type;
			return "Found not yet handled type (" + type + ")";
		}

	}

}
