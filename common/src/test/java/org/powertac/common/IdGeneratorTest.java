package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class IdGeneratorTest {

  @Test
  public void createId() {
    IdGenerator.setPrefix(42);
    assertTrue(IdGenerator.createId()>0);
  }
}
