/**
 * 
 */
package org.powertac.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.powertac.util.MessageDispatcher.dispatch;

/**
 * @author John Collins
 *
 */
class MessageDispatchTest
{
  Object result;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp () throws Exception
  {
  }

  @Test
  void test ()
  {
    Dummy obj = new Dummy();
    double dbl = 3.0;
    int num = 2;
    
    // one arg
    dispatch(obj, "testfn", Integer.valueOf(6));
    assertTrue(result instanceof Integer);
    assertEquals(7, result);

    // two args
    dispatch(obj, "testfn", Double.valueOf(dbl), Integer.valueOf(num));
    assertTrue(result instanceof String);
    assertEquals("8.0", result);

    // two args, different order
    dispatch(obj, "testfn", Integer.valueOf(num), Double.valueOf(dbl));
    assertTrue(result instanceof String);
    assertEquals("9.0", result);

    // throw an exception
    try {
      dispatch(obj, "testfn", "Dubious string");
    }
    catch (Exception ex) {
      System.out.println("exception returned by dispatch()");
    }

    // no args
    dispatch(obj, "testfn");
    assertTrue(result instanceof String);
    assertEquals("No arg", result);
  }

  public class Dummy
  {
    public void testfn (Integer num) {
      result = num + 1;
    }

    public void testfn (Double exp, Integer base) {
      result = Double.valueOf(Math.pow(base, exp)).toString();
    }

    public void testfn (Integer exp, Double base) {
      result = Double.valueOf(Math.pow(base, exp)).toString();
    }

    public void testfn () {
      result = "No arg";
    }

    public void testfn (String badarg) throws Exception {
      throw new Exception("bad argument" + badarg);
    }
  }
}
