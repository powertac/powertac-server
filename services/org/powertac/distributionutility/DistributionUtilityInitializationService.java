package org.powertac.distributionutility;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class DistributionUtilityInitializationService
implements InitializationService
{
  Logger log = Logger.getLogger(this.getClass().getName());

  @Autowired
  private DistributionUtilityService distributionUtilityService;

  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  public void setDefaults() {
    pluginConfigRepo.makePluginConfig("DistributionUtility", "")
      .addConfiguration("distributionFeeMin", "-0.003")
      .addConfiguration("distributionFeeMax", "-0.03")
      .addConfiguration("balancingCostMin", "-0.02")
      .addConfiguration("balancingCostMax", "-0.06")
      .addConfiguration("defaultSpotPrice", "-50.0");
  }

  public String initialize(Competition competition,
                           List<String> completedInits) {
    PluginConfig duConfig = pluginConfigRepo.findByRoleName("DistributionUtility");
    if (duConfig == null) {
      log.error("PluginConfig for DistributionUtility does not exist");
      return "fail";
    } else {
      distributionUtilityService.init(duConfig);
      return "DistributionUtility";
    }
  }
}
