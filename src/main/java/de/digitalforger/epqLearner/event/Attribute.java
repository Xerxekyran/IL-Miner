package de.digitalforger.epqLearner.event;

import java.io.Serializable;


/**
 * An attribute of an event
 * 
 * @author george
 * 
 */
public class Attribute implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6635984407404272012L;
	
	private String name;
	private Value value;

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public Attribute(String name, Value value) {		
		this.name = name;
		this.value = value;
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public Attribute(String name, long value) {
		this(name, new Value(value));
	}

	/**
	 * 
	 * @param name
	 * @param value
	 */
	public Attribute(String name, double value) {
		this(name, new Value(value));
	}

	/**
	 * @param name
	 * @param value
	 */
	public Attribute(String name, String value) {
		this(name, new Value(value));
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public Value getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(Value value) {
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Attribute [name=" + name + ", value=" + value + "]";
	}
}
