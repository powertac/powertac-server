/*
 * Copyright (c) 2012 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.genco;

import static org.junit.Assert.*;

import java.util.Collection;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * @author jcollins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class
})
public class GencoConfigTest
{
  private Configuration config;

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
    ApplicationContext context = SpringApplicationContext.getContext();
    Resource props = context.getResource("genco-test.properties");
    // this probably won't work if tests are packaged in a jarfile
    config = new PropertiesConfiguration(props.getFile());
  }

  @Test
  public void testConfigurator ()
  {
    Configurator processor = new Configurator();
    processor.setConfiguration(config);
    Collection<?> gencos = processor.configureInstances(Genco.class);
    assertEquals("2 gencos generated", 2, gencos.size());
    Genco nsp1 = null;
    Genco nsp2 = null;
    for (Object item : gencos) {
      Genco genco = (Genco)item;
      if ("nsp1".equals(genco.getUsername()))
        nsp1 = genco;
      else if ("nsp2".equals(genco.getUsername()))
        nsp2 = genco;
    }
    assertNotNull("nsp1 created", nsp1);
    assertEquals("nsp1 capacity", 20.0, nsp1.getNominalCapacity(), 1e-6);
    assertEquals("nsp1 variability", 0.05, nsp1.getVariability(), 1e-6);
    assertEquals("nsp1 reliability", 0.98, nsp1.getReliability(), 1e-6);
    assertEquals("nsp1 cost", 20.0, nsp1.getCost(), 1e-6);
    assertEquals("nsp1 emission", 1.0, nsp1.getCarbonEmissionRate(), 1e-6);
    assertEquals("nsp1 leadtime", 8, nsp1.getCommitmentLeadtime());
    assertNotNull("nsp2 created", nsp2);
    assertEquals("nsp2 capacity", 30.0, nsp2.getNominalCapacity(), 1e-6);
    assertEquals("nsp2 variability", 0.05, nsp2.getVariability(), 1e-6);
    assertEquals("nsp2 reliability", 0.97, nsp2.getReliability(), 1e-6);
    assertEquals("nsp2 cost", 30.0, nsp2.getCost(), 1e-6);
    assertEquals("nsp2 emission", 0.95, nsp2.getCarbonEmissionRate(), 1e-6);
    assertEquals("nsp2 leadtime", 6, nsp2.getCommitmentLeadtime());
  }
}
