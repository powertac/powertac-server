/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.evcustomer.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.RandomSeed;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.RandomSeedRepo;


/**
 * @author Govert Buijs, John Collins
 */
public class ActivityTest
{
  private Activity activity;
  private int id = 3;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;
  private ServerConfiguration serverConfig;
  private Configurator configurator;
  private MapConfiguration config;
  private String activityName = "TestActivity";
  private double weekdayWeight = 1.0;
  private double weekendWeight = 0.5;

  @BeforeEach
  public void setUp ()
  {
    activity = new Activity(activityName);

    // set up randomSeed mock
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(seed);

    // Set up serverProperties mock
    serverConfig = mock(ServerConfiguration.class);
    configurator = new Configurator();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        configurator.configureSingleton(args[0]);
        return null;
      }
    }).when(serverConfig).configureMe(any());
  }

  @AfterEach
  public void tearDown ()
  {
    activity = null;
  }

  // map names to instances
  private Map<String, Activity> mapNames (Collection<?> objects)
  {
    Map<String, Activity> result = new HashMap<>();
    for (Object thing : objects) {
      Activity act = (Activity)thing;
      result.put(act.getName(), act);
    }
    return result;
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(activityName, activity.getName(), activityName);
    assertEquals(0, activity.getInterval());
    assertFalse(activity.getDailyProfileOptional().isPresent());
    assertFalse(activity.getWeeklyProfileOptional().isPresent());
  }
  
  @Test
  public void basicConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("evcustomer.beans.activity.instances", "commuting,visit");
    map.put("evcustomer.beans.activity.commuting.weekdayWeight", "0.6");
    map.put("evcustomer.beans.activity.commuting.weekendWeight", "0.6");
    map.put("evcustomer.beans.activity.commuting.awayChargerProbability", "0.55");
    map.put("evcustomer.beans.activity.commuting.dailyProfile",
            "0,0,0,0,0,0,0,.4,.5,.1,0,0,0,0,0,0,0,0,0,0,0,0,0,0");
    map.put("evcustomer.beans.activity.visit.weekdayWeight", "0.2");
    map.put("evcustomer.beans.activity.visit.weekendWeight", "0.6");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(Activity.class);
    assertEquals(2, instances.size(), "two instances");
    Map<String, Activity> acts = mapNames(instances);
    Activity commuting = acts.get("commuting");
    Activity visit = acts.get("visit");
    assertNotNull(commuting);
    assertNotNull(visit);
    assertEquals(0.6, commuting.getWeekdayWeight(), 1e-6, "correct weekday weight");
    assertTrue(commuting.getDailyProfileOptional().isPresent());
    double[] daily1 = commuting.getDailyProfileOptional().get();
    assertNotNull(daily1);
    Optional<double[]> daily = commuting.getDailyProfileOptional();
    assertTrue(daily.isPresent());
    assertEquals(0.0, daily.get()[3], 1e-6, "zero at 3:00");
    assertEquals(0.4, daily.get()[7], 1e-6, ".4 at 7:00");
    assertEquals(0.2, visit.getWeekdayWeight(), 1e-6, "visit weekday weight");
  }
  
  @Test
  public void dailyProfileNormalizedGT0 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("evcustomer.beans.activity.instances", "commuting,visit");
    map.put("evcustomer.beans.activity.commuting.dailyProfile",
            "0,0,0,0,0,0,0,0.8,1.0,0.2,0,0,0,0,0,0,0,0,0,0,0,0,0,0");
    map.put("evcustomer.beans.activity.visit.weekdayWeight", "0.2");
    map.put("evcustomer.beans.activity.visit.weekendWeight", "0.6");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(Activity.class);
    Map<String, Activity> acts = mapNames(instances);
    Activity commuting = acts.get("commuting");
    assertNotNull(commuting);
    assertTrue(commuting.getDailyProfileOptional().isPresent());
    double[] daily = commuting.getDailyProfileOptional().get();
    assertNotNull(daily);
    assertEquals(0.0, daily[3], 1e-6, "zero at 3:00");
    assertEquals(0.4, daily[7], 1e-6, ".4 at 7:00");
    assertEquals(0.5, daily[8], 1e-6, ".5 at 8:00");
    assertEquals(0.1, daily[9], 1e-6, ".1 at 9:00");
  }

  @Test
  public void probabilityForTimeslot ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("evcustomer.beans.activity.instances", "min,daily");
    map.put("evcustomer.beans.activity.daily.dailyProfile",
            "0,0,0,0,0,0,0.8,1.0,0.2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0");
    config = new MapConfiguration(map);
    Configurator configurator = new Configurator();
    configurator.setConfiguration(config);
    Collection<?> instances =
        configurator.configureInstances(Activity.class);
    Map<String, Activity> acts = mapNames(instances);
    assertEquals(2, acts.size(), "two activities");

    Activity min = acts.get("min");
    assertEquals(1.0, min.getProbabilityForTimeslot(1));
    assertEquals(1.0, min.getProbabilityForTimeslot(1));
    assertEquals(1.0, min.getProbabilityForTimeslot(23));

    // check over/under
    assertEquals(1.0, min.getProbabilityForTimeslot(2));
    assertEquals(1.0, min.getProbabilityForTimeslot(2));
    assertEquals(0.0, min.getProbabilityForTimeslot(-1));
    assertEquals(0.0, min.getProbabilityForTimeslot(24));

    Activity daily = acts.get("daily");
    assertEquals(0.0, daily.getProbabilityForTimeslot(0), 1e-6);
    assertEquals(0.0, daily.getProbabilityForTimeslot(5), 1e-6);
    assertEquals(0.4, daily.getProbabilityForTimeslot(6), 1e-6);
    assertEquals(0.5, daily.getProbabilityForTimeslot(7), 1e-6);
    assertEquals(0.1, daily.getProbabilityForTimeslot(8), 1e-6);
    assertEquals(0.0, daily.getProbabilityForTimeslot(9), 1e-6);
    assertEquals(0.5, daily.getProbabilityForTimeslot(7), 1e-6);
  }
}