package org.powertac.visualizer.push;

import java.io.Serializable;

import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.stereotype.Service;

@Service
public class GlobalCounterBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private int count;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public synchronized void increment() {
		
		count++;
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();
		pushContext.push("/counter", String.valueOf(count));
	}
}
