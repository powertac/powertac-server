/**
 * Copyright (c) 2022 by John Collins.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer.evcharger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandSamplerTest
{
  private static DemandSampler demandSampler;
  private static final int POP_SIZE = 1000;

  @BeforeAll
  static void beforeAll ()
  {
    demandSampler = new DemandSampler();
    demandSampler.initialize();
  }

  @Test
  public void testSampleNewPlugins ()
  {
    double newPlugins = demandSampler.sampleNewPlugins(16, POP_SIZE);
    assert newPlugins <= POP_SIZE;
    assert newPlugins >= 0;
  }

  @Test
  public void testSampleNewPluginsAreRandom ()
  {
    double newPlugins1 = demandSampler.sampleNewPlugins(16, POP_SIZE);
    double newPlugins2 = demandSampler.sampleNewPlugins(16, POP_SIZE);
    Assertions.assertNotEquals(newPlugins1, newPlugins2);
  }

  @Test
  public void testSampleNewPluginsNightIsLessThanDay ()
  {
    double newPluginsDay = demandSampler.sampleNewPlugins(16, POP_SIZE);
    double newPluginsNight = demandSampler.sampleNewPlugins(2, POP_SIZE);
    assert newPluginsNight < newPluginsDay;
  }
}
