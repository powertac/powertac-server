package org.powertac.visualizer.push;

import java.io.Serializable;

import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

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
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();
		pushContext.push("/counter", String.valueOf(count));
	}
}
