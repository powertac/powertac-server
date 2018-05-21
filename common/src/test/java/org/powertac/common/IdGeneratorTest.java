package org.powertac.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class IdGeneratorTest {

	@Test
	public void createId() {
		IdGenerator.setPrefix(42);
		assertTrue(IdGenerator.createId()>0);
	}
}
