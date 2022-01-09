/**
 * Copyright (c) 2022 by John Collins
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
package org.powertac.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.RandomSeed;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.logtool.LogtoolCore;
import org.powertac.logtool.common.DomainBuilder;
import org.powertac.logtool.common.DomainObjectReader;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 *
 */
class CompetitionSetupServiceTest
{
  CompetitionSetupService css;
  ServerPropertiesService serverProps;
  RandomSeedRepo rsRepo;

  String seedPath = "src/test/resources/artifacts/seed-data.state";

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp () throws Exception
  {
    css = new CompetitionSetupService();
    serverProps = new ServerPropertiesService();
    ReflectionTestUtils.setField(css, "serverProps", serverProps);
  }

  void setupLogtool ()
  {
    LogtoolCore core = new LogtoolCore();
    ReflectionTestUtils.setField(css, "logtoolCore", core);
    serverProps = new ServerPropertiesService();
    ReflectionTestUtils.setField(css, "serverProps", serverProps);
    rsRepo = new RandomSeedRepo();
    ReflectionTestUtils.setField(css, "randomSeedRepo", rsRepo);
    DomainObjectReader dor = new DomainObjectReader();
    ReflectionTestUtils.setField(core, "reader", dor);
    DomainBuilder db = mock(DomainBuilder.class);
    ReflectionTestUtils.setField(core, "domainBuilder", db);
  }

  /**
   * Test method for {@link org.powertac.server.CompetitionSetupService#bootSession(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
   */
  @Test
  void testSeedSource ()
  {
    String dummy = "dummy.seeds";
    css.setSeedSource(dummy);
    assertEquals(dummy, css.getSeedSource(), "seed source stored and retrieved");
  }

  @Test
  void testFileAccess ()
  {
    css.setSeedSource(seedPath);
    File source = new File(css.getSeedSource());
    assertTrue(source.canRead(), "seed source is readable");
  }

  @Test
  void testLoadSeeds ()
  {
    setupLogtool();
    css.setSeedSource(seedPath);
    css.loadSeedsMaybe();
    assertEquals("1330",
                 serverProps.getProperty("common.competition.minimumTimeslotCount"));
    assertEquals("1330",
                 serverProps.getProperty("common.competition.expectedTimeslotCount"));
    RandomSeed rs = rsRepo.getRandomSeed("CompetitionControlService", 0, "game-setup");
    assertEquals("CompetitionControlService", rs.getRequesterClass());
    assertEquals(3768546988373259332l, rs.getValue());
  }
}
