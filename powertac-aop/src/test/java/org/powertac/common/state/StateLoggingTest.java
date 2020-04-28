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

  private String stripMillis (String raw)
  {
    return raw.substring(raw.indexOf(':') + 1);
  }

  @Test
  void testLog ()
  {
    DummyDomain dd = new DummyDomain(1, "first");
    dd = new DummyDomain(31, 2, "second");
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
      String ln = lines.get(0);
      String body = stripMillis(ln);
      assertEquals("c.state.DummyDomain::0::new::1::first", body);
      assertEquals("c.state.DummyDomain::31::new::31::2::second",
                   stripMillis(lines.get(1)));
      assertEquals("c.state.DummyDomain::31::setNumber::42",
                   stripMillis(lines.get(2)));
    }
    catch (IOException ioe) {
      fail("IOException reading log file:" + ioe.toString());
    }
  }
}
