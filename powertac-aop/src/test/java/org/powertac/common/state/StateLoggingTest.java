package org.powertac.common.state;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class StateLoggingTest
{
  static private Logger log = LogManager.getLogger(StateLoggingTest.class.getName());

  @Test
  void testLog ()
  {
    DummyDomain dd = new DummyDomain(1, "first");
    dd = new DummyDomain(2, "second");
    dd.setNumber(42);
    //LogManager.shutdown();

    try (BufferedReader input = new BufferedReader(new FileReader("log/test.state"))) {
      //String ddClass = DummyDomain.class.getName();
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
      assertTrue(lines.size() >= 3, "at least three lines");
    }
    catch (IOException ioe) {
      fail("IOException reading log file:" + ioe.toString());
    }
  }

}
