package org.powertac.visualizer.push;

import java.io.Serializable;

import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

public class GlobalCounterBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private double count;

	public double getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public synchronized void increment() {
		
		count= Math.random();
		EventBus pushContext = EventBusFactory.getDefault().eventBus();
		pushContext.publish("/counter", String.valueOf(count));
	}
}
