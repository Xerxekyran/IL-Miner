package de.digitalforger.epqLearner.PML.constraints.order;

import java.io.Serializable;
import java.util.LinkedList;

public class EventInstanceOrder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1931376969670399623L;

	private LinkedList<OrderElement> orderElements = new LinkedList<OrderElement>();

	/**
	 * 
	 * @param e
	 */
	public void add(OrderElement e) {
		orderElements.add(e);
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public OrderElement get(int index) {
		return orderElements.get(index);
	}

	/**
	 * 
	 * @return
	 */
	public int size() {
		return orderElements.size();
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("EventInstanceOrder [");
		for (OrderElement oe : orderElements) {
			ret.append(oe.toString() + ", ");
		}
		ret.append("]");
		return ret.toString();
	}

	/**
	 * 
	 * @param eventInstanceOrder
	 * @return
	 */
	public boolean hasEqualOrder(EventInstanceOrder compareToOrder) {
		boolean ret = false;
		
		if(size() == compareToOrder.size())  {
			ret = true;
			
			for(int i = 0; i < size(); i++) {
				if(!get(i).equals(compareToOrder.get(i))) {
					ret = false;
					break;
				}
			}						
		}
		
		return ret;
	}
}
