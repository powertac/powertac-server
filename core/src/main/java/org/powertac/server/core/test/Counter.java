package org.powertac.server.core.test;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

	private final AtomicInteger count = new AtomicInteger();

	public int next() {
		int nextNumber = count.incrementAndGet();
		return (nextNumber % 5 == 0) ? -nextNumber : nextNumber;
	}

}