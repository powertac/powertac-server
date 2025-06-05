package org.powertac.samplebroker.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.msg.BrokerAccept;

public class MessageDispatcherTest
{
  private MessageDispatcher router;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    router = new MessageDispatcher();
  }

  @Test
  public void testRegisterMessageHandler ()
  {
    assertNotNull(router, "make sure we have a router");
    Set<Object> regs = router.getRegistrations(Set.class);
    assertNull(regs, "no registrations yet");
    router.registerMessageHandler(this, Set.class);
    regs = router.getRegistrations(Set.class);
    assertEquals(1, regs.size(), "one registration");
    assertTrue(regs.contains(this), "correct registration");
  }

  @Test
  public void testRouteMessage ()
  {
    LocalHandler handler = new LocalHandler();
    router.registerMessageHandler(handler, BrokerAccept.class);
    assertNull(handler.result, "initially null");
    BrokerAccept accept = new BrokerAccept(1);
    router.routeMessage(accept);
    assertEquals(accept, handler.result, "received message");
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
