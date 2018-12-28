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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.config.Configurator;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author jcollins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class
})
public class GencoConfigTest
{
  private PropertiesConfiguration config;

  /**
   *
   */
  @BeforeEach
  public void setUp () throws Exception
  {
    // this probably won't work if tests are packaged in a jarfile
    config = Configurator.readProperties("genco-test.properties");
  }

  @Test
  public void testConfigurator ()
  {
    Configurator processor = new Configurator();
    processor.setConfiguration(config);
    Collection<?> gencos = processor.configureInstances(Genco.class);
    assertEquals(2, gencos.size(), "2 gencos generated");
    Genco nsp1 = null;
    Genco nsp2 = null;
    for (Object item : gencos) {
      Genco genco = (Genco)item;
      if ("nsp1".equals(genco.getUsername()))
        nsp1 = genco;
      else if ("nsp2".equals(genco.getUsername()))
        nsp2 = genco;
    }
    assertNotNull(nsp1, "nsp1 created");
    assertEquals(20.0, nsp1.getNominalCapacity(), 1e-6, "nsp1 capacity");
    assertEquals(0.05, nsp1.getVariability(), 1e-6, "nsp1 variability");
    assertEquals(0.98, nsp1.getReliability(), 1e-6, "nsp1 reliability");
    assertEquals(20.0, nsp1.getCost(), 1e-6, "nsp1 cost");
    assertEquals(1.0, nsp1.getCarbonEmissionRate(), 1e-6, "nsp1 emission");
    assertEquals(8, nsp1.getCommitmentLeadtime(), "nsp1 leadtime");
    assertNotNull(nsp2, "nsp2 created");
    assertEquals(30.0, nsp2.getNominalCapacity(), 1e-6, "nsp2 capacity");
    assertEquals(0.05, nsp2.getVariability(), 1e-6, "nsp2 variability");
    assertEquals(0.97, nsp2.getReliability(), 1e-6, "nsp2 reliability");
    assertEquals(30.0, nsp2.getCost(), 1e-6, "nsp2 cost");
    assertEquals(0.95, nsp2.getCarbonEmissionRate(), 1e-6, "nsp2 emission");
    assertEquals(6, nsp2.getCommitmentLeadtime(), "nsp2 leadtime");
  }
}
