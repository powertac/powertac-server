/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.genco;

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;

class GencoInitializationService 
    implements InitializationService
{  
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  @Autowired
  private SimpleGencoService simpleGencoService;

  public void setDefaults ()
  {
    build("nsp1", 100.0, 0.05, 20.0, 8, 1.0);
    build("nsp2", 60.0, 0.05, 21.8, 8, 1.0);
    build("gas1", 40.0, 0.03, 35.0, 1, 0.5);
    build("gas2", 30.0, 0.03, 38.5, 0, 0.5) ;
    build("backup", 20000.0, 0.001, 100.0, 0, 0.8); // backup source   
  }

  public String initialize (Competition competition, List<String> completedInits)
  {
    ArrayList<Genco> gencos = new ArrayList<Genco>();
    for (PluginConfig config : pluginConfigRepo.findAllByRoleName("genco")) {
      Genco genco = new Genco(config.getName());
      genco.configure(config);
    }
    simpleGencoService.init(gencos);
    return "Genco";
  }

  private void build (String name, double nominalCapacity, double variability, double cost,
                      int commitmentLeadTime, double carbonEmissionRate)
  {
    pluginConfigRepo.makePluginConfig("genco", name)
      .addConfiguration("nominalCapacity", Double.toString(nominalCapacity))
      .addConfiguration("cost", Double.toString(cost))
      .addConfiguration("commitmentLeadtime", Integer.toString(commitmentLeadTime))
      .addConfiguration("carbonEmissionRate", Double.toString(carbonEmissionRate));
  }
}
