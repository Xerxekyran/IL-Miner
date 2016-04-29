package de.digitalforger.epqLearner.PML;

import java.io.Serializable;

public class EventType implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1334373906572048234L;

	private String name = "";

	public EventType(String name) {
		super();
		this.name = name;
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
}
