package org.powertac.common.state;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class StateLoggingTest
{
  static private Logger log =
          LogManager.getLogger(StateLoggingTest.class);


  private String stripMillis (String raw)
  {
    return raw.substring(raw.indexOf(':') + 1);
  }

  @Test
  void testLog ()
  {
    DummyDomain dd = new DummyDomain(1, "original");
    dd = new DummyDomain(31, 2, "second");
    dd.setNumber(42);

    File logDir = new File("log");
    if (!logDir.exists()) {
      logDir.mkdirs();
    }

    try (BufferedReader input = new BufferedReader(new FileReader("log/test.state")))
    {
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      boolean gathering = false;
      while ((line = input.readLine()) != null) {
        if (line.contains("original")) {
          gathering = true;
        }
        if (gathering) {
          lines.add(line);
        }
      }
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
      assertTrue(lines.size() >= 3, "at least three lines");
      String ln = lines.get(0);
      String body = stripMillis(ln);
      assertEquals("org.powertac.common.state.DummyDomain::0::new::1::original",
                   body);
      String[] tokens = body.split("::");
      assertEquals("org.powertac.common.state.DummyDomain",
                   StateLogging.unabbreviate(tokens[0]));
      assertEquals("org.powertac.common.state.DummyDomain::31::new::31::2::second",
                   stripMillis(lines.get(1)));
      assertEquals("org.powertac.common.state.DummyDomain::31::setNumber::42",
                   stripMillis(lines.get(2)));
    }
    catch (IOException ioe) {
      fail("IOException reading log file:" + ioe.toString());
    }
  }

  @Test
  public void testAbbreviatedLog ()
  {
    StateLogging.setClassnameAbbreviation(true);
    DummyDomain dd = new DummyDomain(1, "abbreviated");
    dd = new DummyDomain(31, 2, "second");
    dd.setNumber(42);

    File logDir = new File("log");
    if (!logDir.exists()) {
      logDir.mkdirs();
    }

    try (BufferedReader input = new BufferedReader(new FileReader("log/test.state")))
    {
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      boolean gathering = false;
      while ((line = input.readLine()) != null) {
        if (line.contains("abbreviated")) {
          gathering = true;
        }
        if (gathering) {
          lines.add(line);
        }
      }
      assertTrue(lines.size() >= 3, "at least three lines");
      String ln = lines.get(0);
      String body = stripMillis(ln);
      assertEquals("c.state.DummyDomain::0::new::1::abbreviated",
                   body);
      String[] tokens = body.split("::");
      assertEquals("org.powertac.common.state.DummyDomain",
                   StateLogging.unabbreviate(tokens[0]));
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
