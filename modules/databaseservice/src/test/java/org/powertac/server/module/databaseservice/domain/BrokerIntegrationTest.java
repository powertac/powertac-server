package org.powertac.server.module.databaseservice.domain;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.roo.addon.test.RooIntegrationTest;

@RooIntegrationTest(entity = Broker.class)
public class BrokerIntegrationTest {

  @Test
  public void testMarkerMethod() {
  }

  @Test
  public void testAddAndFetchBroker() {
    Broker b = new Broker();
    b.setAuthToken("myAuthToken");
    b.persist();

    Assert.assertNotNull(b.getId());

    Broker b2 = Broker.findBroker(b.getId());
    Assert.assertEquals("myAuthToken", b2.getAuthToken());

    Assert.assertEquals(1, Broker.countBrokers());
  }
}
