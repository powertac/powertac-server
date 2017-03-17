package org.powertac.samplebroker.core;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.msg.BrokerAccept;
import org.powertac.samplebroker.core.MessageDispatcher;

public class MessageDispatcherTest
{
  private MessageDispatcher router;
  
  @Before
  public void setUp () throws Exception
  {
    router = new MessageDispatcher();
  }

  @Test
  public void testRegisterMessageHandler ()
  {
    assertNotNull("make sure we have a router", router);
    Set<Object> regs = router.getRegistrations(Set.class);
    assertNull("no registrations yet", regs);
    router.registerMessageHandler(this, Set.class);
    regs = router.getRegistrations(Set.class);
    assertEquals("one registration", 1, regs.size());
    assertTrue("correct registration", regs.contains(this));
  }

  @Test
  public void testRouteMessage ()
  {
    LocalHandler handler = new LocalHandler();
    router.registerMessageHandler(handler, BrokerAccept.class);
    assertNull("initially null", handler.result);
    BrokerAccept accept = new BrokerAccept(1);
    router.routeMessage(accept);
    assertEquals("received message", accept, handler.result);
  }

  public class LocalHandler
  {
    Object result = null;

    public void handleMessage (BrokerAccept msg)
    {
      result = msg;
    }
  }
}
