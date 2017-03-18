package org.powertac.visualizer.domain.wholesale;

import org.powertac.common.IdGenerator;
import org.powertac.common.OrderbookOrder;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This is a convenient class for wholesale visualization. It is a copy of OrderbookOrder class. Purpose of this class is to avoid unnecessary entries created by original OrderbookOrder for Power TAC server STATE log.
 * @author Jurica Babic
 *
 */
public class VisualizerOrderbookOrder implements Comparable<Object> {
	//@XStreamOmitField
	//private long id = IdGenerator.createId();

	//@XStreamAsAttribute
	private Double limitPrice;

//	@XStreamAsAttribute
	private double mWh;

	public VisualizerOrderbookOrder(double mWh, Double limitPrice) {
		super();
		this.limitPrice = limitPrice;
		this.mWh = mWh;
	}

//	public long getId() {
//		return id;
//	}

	public int compareTo(Object o) {
		if (!(o instanceof VisualizerOrderbookOrder))
			return 1;
		VisualizerOrderbookOrder other = (VisualizerOrderbookOrder) o;
		if (this.limitPrice == null)
			if (other.limitPrice == null)
				return 0;
			else
				return -1;
		else if (other.limitPrice == null)
			return 1;
		else
			return (this.limitPrice == (other.limitPrice) ? 0
					: (this.limitPrice < other.limitPrice ? -1 : 1));
	}

	/**
	 * Returns the limit price for this unsatisfied order. Normally this will
	 * have a sign opposite to the mWh energy quantity.
	 */
	public Double getLimitPrice() {
		return limitPrice;
	}

	/**
	 * Returns the quantity of energy unsatisfied for an order. This will
	 * positive for a bid, negative for an ask.
	 */
	public double getMWh() {
		return mWh;
	}

	public String toString() {
		return ("OrderbookOrder: " + mWh + "@" + limitPrice);
	}
}